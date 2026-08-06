package com.gnilc.novum.image.support;

import com.gnilc.novum.image.annotation.ObjectKeyToUrl;
import com.gnilc.novum.image.service.ImageService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * 在 Spring MVC 序列化响应体之前解析标记了 {@link ObjectKeyToUrl} 的字段。
 *
 * <p>该注解标记 URL 字段，并指定同一对象中 ObjectKey 字段的名称。本处理器会遍历响应对象图，
 * 包括常见的响应包装对象和容器，再通过 {@link ImageService} 填充 URL 字段。
 */
@RestControllerAdvice
public class ObjectKeyToUrlProcessor implements ResponseBodyAdvice<Object> {
    /** 将稳定的 ObjectKey 转换为 API 对外提供的公共 URL。 */
    private final ImageService imageService;

    /** 同一类型的反射元数据固定不变，因此只构建一次并重复使用。 */
    private final ConcurrentMap<Class<?>, TypeMetadata> metadataCache = new ConcurrentHashMap<>();

    public ObjectKeyToUrlProcessor(ImageService imageService) {
        this.imageService = imageService;
    }

    @Override
    public boolean supports(
            @NonNull MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        // 注解可能出现在任意响应结构的嵌套对象中，也可能由任意转换器处理。
        return true;
    }

    @Override
    @Nullable
    public Object beforeBodyWrite(
            @Nullable Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response) {
        // 每个响应使用独立的对象标识集合，避免循环引用和共享引用导致重复处理。
        return processValue(body, newVisitedSet());
    }

    /** 直接处理指定值，同时作为单元测试的调用入口。 */
    public void process(@Nullable Object value) {
        processValue(value, newVisitedSet());
    }

    @Nullable
    private Object processValue(@Nullable Object value, Set<Object> visited) {
        if (value == null || !visited.add(value)) {
            return value;
        }

        // 在反射处理 POJO 前先处理包装类型。不可变包装对象需要替换，可变包装对象可以原地更新。
        if (value instanceof Optional<?> optional) {
            return optional.map(item -> processValue(item, visited));
        }
        if (value instanceof AtomicReference<?> reference) {
            return processReference(reference, visited);
        }
        if (value instanceof Map<?, ?> map) {
            return processMap(map, visited);
        }
        if (value instanceof Stream<?> stream) {
            // 检查响应时不能提前消费 Stream，因此在元素实际被读取时再处理。
            return stream.map(item -> processValue(item, newVisitedSet()));
        }
        if (value instanceof Iterator<?> iterator) {
            // Iterator 同样具有惰性，因此这里只包装它，而不提前读取元素。
            return new ProcessingIterator(iterator);
        }
        if (value instanceof Collection<?> collection) {
            return processCollection(collection, visited);
        }
        if (value instanceof Iterable<?> iterable) {
            return new ProcessingIterable(iterable);
        }

        Class<?> type = value.getClass();
        if (type.isArray()) {
            if (type.getComponentType().isPrimitive()) {
                return value;
            }
            // 对象数组中的元素处理完成后可以直接写回原数组。
            for (int index = 0; index < Array.getLength(value); index++) {
                Object item = Array.get(value, index);
                Object processed = processValue(item, visited);
                if (processed != item) {
                    Array.set(value, index, processed);
                }
            }
            return value;
        }
        if (isLeafType(value, type)) {
            return value;
        }

        // 先转换当前对象中带注解的字段，再递归处理它的所有实例字段。
        TypeMetadata metadata = metadataCache.computeIfAbsent(type, this::inspect);
        metadata.getMappings().forEach(mapping -> apply(value, mapping));
        metadata.getFields().forEach(field -> processField(value, field, visited));
        return value;
    }

    /**
     * 检查指定类型，并记录注解字段映射以及后续需要递归处理的字段。
     */
    private TypeMetadata inspect(Class<?> type) {
        List<FieldMapping> mappings = new ArrayList<>();
        ReflectionUtils.doWithFields(type, field -> {
            ObjectKeyToUrl annotation = field.getAnnotation(ObjectKeyToUrl.class);
            if (annotation == null) {
                return;
            }
            ReflectionUtils.makeAccessible(field);
            Field source = findSourceField(type, field, annotation.value());
            mappings.add(new FieldMapping(source, field));
        });

        List<Field> fields = new ArrayList<>();
        ReflectionUtils.doWithFields(type, field -> {
            // 静态字段和编译器生成的字段不属于响应对象图。
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                ReflectionUtils.makeAccessible(field);
                fields.add(field);
            }
        });
        return new TypeMetadata(List.copyOf(mappings), List.copyOf(fields));
    }

    /** 校验注解指定的源字段，并将其设置为可通过反射读取。 */
    private Field findSourceField(Class<?> type, Field target, String sourceName) {
        if (sourceName.isBlank()) {
            throw invalidMapping(type, target, "source field name must not be blank");
        }
        Field source = ReflectionUtils.findField(type, sourceName);
        if (source == null) {
            throw invalidMapping(type, target, "source field '" + sourceName + "' does not exist");
        }
        if (source.getType() != String.class || target.getType() != String.class) {
            throw invalidMapping(type, target, "source and URL fields must both be String");
        }
        if (Modifier.isFinal(target.getModifiers())) {
            throw invalidMapping(type, target, "URL field must be writable");
        }
        ReflectionUtils.makeAccessible(source);
        return source;
    }

    /** 读取 ObjectKey，生成对应 URL，并写入带注解的目标字段。 */
    private void apply(Object owner, FieldMapping mapping) {
        String objectKey = (String) read(mapping.getSource(), owner);
        String url = objectKey == null ? null : imageService.getUrl(objectKey);
        try {
            mapping.getTarget().set(owner, url);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(
                    "Cannot write @ObjectKeyToUrl field " + fieldName(mapping.getTarget()), exception);
        }
    }

    /** 递归处理嵌套字段，并在容器对象发生替换时将新对象写回字段。 */
    private void processField(Object owner, Field field, Set<Object> visited) {
        Object nested = read(field, owner);
        Object processed = processValue(nested, visited);
        if (processed == nested) {
            return;
        }
        try {
            field.set(owner, processed);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot write field " + fieldName(field), exception);
        }
    }

    /** 只处理 Map 的值而不改变键；若值均未被替换，则保留原 Map。 */
    private Object processMap(Map<?, ?> map, Set<Object> visited) {
        Map<Object, Object> processed = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object original = entry.getValue();
            Object value = processValue(original, visited);
            changed |= value != original;
            processed.put(entry.getKey(), value);
        }
        return changed ? processed : map;
    }

    /** 处理集合元素；存在元素被替换时，以新的 List 返回处理结果。 */
    private Object processCollection(Collection<?> collection, Set<Object> visited) {
        List<Object> processed = new ArrayList<>(collection.size());
        boolean changed = false;
        for (Object original : collection) {
            Object value = processValue(original, visited);
            changed |= value != original;
            processed.add(value);
        }
        return changed ? processed : collection;
    }

    /** AtomicReference 中的值被替换时，直接更新原引用。 */
    @SuppressWarnings("unchecked")
    private Object processReference(AtomicReference<?> reference, Set<Object> visited) {
        Object original = reference.get();
        Object processed = processValue(original, visited);
        if (processed != original) {
            ((AtomicReference<Object>) reference).set(processed);
        }
        return reference;
    }

    /**
     * 判断无需继续反射检查的响应标量值。枚举内部字段属于 JDK 模块，不能作为普通 POJO 反射处理。
     */
    private boolean isLeafType(Object value, Class<?> type) {
        return value instanceof Enum<?>
                || type.getPackageName().startsWith("java.")
                || value instanceof Resource
                || value instanceof InputStream
                || value instanceof Reader;
    }

    /** 使用对象标识而不是 equals 语义，确保内容相等但实例不同的对象都会被处理。 */
    private Set<Object> newVisitedSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /** 不调用业务 getter，直接读取字段值。 */
    private Object read(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read field " + fieldName(field), exception);
        }
    }

    private IllegalStateException invalidMapping(Class<?> type, Field target, String reason) {
        return new IllegalStateException(
                "Invalid @ObjectKeyToUrl mapping on " + type.getName() + "." + target.getName()
                        + ": " + reason + ".");
    }

    private String fieldName(Field field) {
        return field.getDeclaringClass().getName() + "." + field.getName();
    }

    @Data
    @AllArgsConstructor
    /** 单个响应对象类型对应的反射缓存数据。 */
    private static class TypeMetadata {
        private final List<FieldMapping> mappings;
        private final List<Field> fields;
    }

    @Data
    @AllArgsConstructor
    /** 一个注解声明的源 ObjectKey 字段和目标 URL 字段。 */
    private static class FieldMapping {
        private final Field source;
        private final Field target;
    }

    /** Iterator 惰性适配器，仅在元素被读取时进行处理。 */
    private final class ProcessingIterator implements Iterator<Object> {
        private final Iterator<?> delegate;

        private ProcessingIterator(Iterator<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public Object next() {
            // 每个元素使用独立集合，避免不同元素之间相互抑制处理。
            return processValue(delegate.next(), newVisitedSet());
        }

        @Override
        public void remove() {
            delegate.remove();
        }
    }

    /** 基于原 Iterable 的惰性适配器。 */
    private final class ProcessingIterable implements Iterable<Object> {
        private final Iterable<?> delegate;

        private ProcessingIterable(Iterable<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Iterator<Object> iterator() {
            return new ProcessingIterator(delegate.iterator());
        }
    }
}
