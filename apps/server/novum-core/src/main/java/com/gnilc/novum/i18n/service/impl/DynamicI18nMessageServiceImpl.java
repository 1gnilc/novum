package com.gnilc.novum.i18n.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.common.base.Preconditions;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.common.i18n.SupportedLocale;
import com.gnilc.common.utils.PageResult;
import com.gnilc.novum.i18n.I18nMessageConstants;
import com.gnilc.novum.i18n.dao.I18nMessageDao;
import com.gnilc.novum.i18n.entity.bo.I18nMessageBo;
import com.gnilc.novum.i18n.entity.dto.I18nMessageValueDto;
import com.gnilc.novum.i18n.entity.dto.I18nMessagePageDto;
import com.gnilc.novum.i18n.entity.dto.I18nMessageDto;
import com.gnilc.novum.i18n.entity.vo.I18nMessageValueVo;
import com.gnilc.novum.i18n.entity.vo.I18nMessageVo;
import com.gnilc.novum.i18n.entity.vo.I18nMessageItemVo;
import com.gnilc.novum.i18n.service.DynamicI18nMessageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DynamicI18nMessageServiceImpl extends ServiceImpl<I18nMessageDao, I18nMessageBo> implements DynamicI18nMessageService {

    private static final int MAX_KEY_LENGTH = 191;
    private static final int MAX_VALUE_LENGTH = 4000;
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*$");
    private static final Set<String> FORBIDDEN_SEGMENTS = Set.of(
            "__proto__", "prototype", "constructor");

    private final I18nMessageService messages;

    public DynamicI18nMessageServiceImpl(I18nMessageService messages) {
        this.messages = messages;
    }

    @Override
    public Map<String, Object> getMessageBundle(String category) {
        String targetCategory = requireCategory(category);
        List<I18nMessageBo> rows = lambdaQuery()
                .eq(I18nMessageBo::getCategory, targetCategory)
                .orderByAsc(I18nMessageBo::getMessageKey)
                .list();
        Map<String, Object> bundle = new LinkedHashMap<>();
        for (String locale : SupportedLocale.codes()) {
            Map<String, Object> localeMessages = new LinkedHashMap<>();
            rows.stream()
                    .filter(row -> locale.equals(row.getLocale()))
                    .forEach(row -> putPath(localeMessages, row.getMessageKey(), row.getI18nValue()));
            bundle.put(locale, localeMessages);
        }
        return bundle;
    }

    @Override
    public List<String> getSupportedCategories() {
        return I18nMessageConstants.SUPPORTED_CATEGORIES;
    }

    @Override
    public PageResult<I18nMessageItemVo> getMessagePage(I18nMessagePageDto dto) {
        I18nMessagePageDto query = dto == null ? new I18nMessagePageDto() : dto;
        String targetCategory = StringUtils.isBlank(query.getCategory())
                ? null
                : requireCategory(query.getCategory());
        if (StringUtils.isNotBlank(query.getLocale())) {
            requireLocale(query.getLocale());
        }

        IPage<I18nMessageBo> keyPage = lambdaQuery()
                .select(I18nMessageBo::getCategory, I18nMessageBo::getMessageKey)
                .eq(targetCategory != null, I18nMessageBo::getCategory, targetCategory)
                .in(targetCategory == null,
                        I18nMessageBo::getCategory,
                        I18nMessageConstants.SUPPORTED_CATEGORIES)
                .like(StringUtils.isNotBlank(query.getKey()), I18nMessageBo::getMessageKey, query.getKey())
                .like(StringUtils.isNotBlank(query.getValue()), I18nMessageBo::getI18nValue, query.getValue())
                .eq(StringUtils.isNotBlank(query.getLocale()), I18nMessageBo::getLocale, query.getLocale())
                .groupBy(I18nMessageBo::getCategory, I18nMessageBo::getMessageKey)
                .orderByAsc(I18nMessageBo::getCategory, I18nMessageBo::getMessageKey)
                .page(query.getPage());
        List<I18nMessageBo> identities = keyPage.getRecords();
        if (identities.isEmpty()) {
            return PageResult.of(keyPage, List.of());
        }

        List<String> keys = identities.stream().map(I18nMessageBo::getMessageKey).distinct().toList();
        Map<String, List<I18nMessageBo>> rowsByKey = lambdaQuery()
                .in(I18nMessageBo::getMessageKey, keys)
                .list()
                .stream()
                .collect(Collectors.groupingBy(I18nMessageBo::getMessageKey));
        List<I18nMessageItemVo> items = identities.stream()
                .map(row -> new I18nMessageItemVo(
                        row.getCategory(),
                        row.getMessageKey(),
                        toValues(rowsByKey.getOrDefault(row.getMessageKey(), List.of()))))
                .toList();
        return PageResult.of(keyPage, items);
    }

    @Override
    public I18nMessageVo getMessageValues(String messageKey) {
        String targetKey = requireKey(messageKey);
        List<I18nMessageBo> rows = findRows(targetKey);
        return rows.isEmpty()
                ? null
                : new I18nMessageVo(rows.get(0).getCategory(), targetKey, toValues(rows));
    }

    @Transactional
    @Override
    public I18nMessageVo createMessage(I18nMessageDto dto) {
        ValidatedMessage target = validateMessage(dto);
        Preconditions.checkArgument(findRows(target.messageKey()).isEmpty(),
                messages.get("system.i18n.targetKey.exists", target.messageKey()));
        Map<String, String> values = new LinkedHashMap<>();
        applyValues(values, target.values());

        try {
            persistRows(target.category(), target.messageKey(), List.of(), values);
        } catch (DuplicateKeyException exception) {
            throw new InvalidArgumentException(
                    messages.get("system.i18n.targetKey.exists"), exception);
        }
        return new I18nMessageVo(target.category(), target.messageKey(), toValues(values));
    }

    @Transactional
    @Override
    public I18nMessageVo saveMessage(I18nMessageDto dto) {
        ValidatedMessage target = validateMessage(dto);
        List<I18nMessageBo> sourceRows = findRows(target.messageKey());
        Map<String, String> mergedValues = sourceRows.stream().collect(Collectors.toMap(
                I18nMessageBo::getLocale,
                I18nMessageBo::getI18nValue,
                (left, right) -> right,
                LinkedHashMap::new));
        applyValues(mergedValues, target.values());

        Preconditions.checkArgument(!sourceRows.isEmpty() || !mergedValues.isEmpty(),
                messages.get("system.i18n.save.empty"));

        persistRows(target.category(), target.messageKey(), sourceRows, mergedValues);
        return new I18nMessageVo(target.category(), target.messageKey(), toValues(mergedValues));
    }

    private ValidatedMessage validateMessage(I18nMessageDto dto) {
        Preconditions.checkArgument(dto != null, messages.get("system.i18n.message.required"));
        String targetCategory = requireCategory(dto.getCategory());
        String targetKey = requireKey(dto.getMessageKey());
        List<I18nMessageValueDto> submittedValues = validateValues(dto.getValues());
        validatePathConflict(targetKey);
        return new ValidatedMessage(targetCategory, targetKey, submittedValues);
    }

    @Transactional
    @Override
    public void removeMessage(String messageKey) {
        String targetKey = requireKey(messageKey);
        lambdaUpdate()
                .eq(I18nMessageBo::getMessageKey, targetKey)
                .remove();
    }

    private String requireCategory(String category) {
        String targetCategory = StringUtils.trimToNull(category);
        Preconditions.checkArgument(targetCategory != null, messages.get("system.i18n.category.required"));
        Preconditions.checkArgument(I18nMessageConstants.SUPPORTED_CATEGORIES.contains(targetCategory),
                messages.get("system.i18n.category.unsupported", targetCategory));
        return targetCategory;
    }

    private String requireKey(String messageKey) {
        String targetKey = StringUtils.trimToNull(messageKey);
        Preconditions.checkArgument(targetKey != null, messages.get("system.i18n.key.required"));
        Preconditions.checkArgument(targetKey.length() <= MAX_KEY_LENGTH,
                messages.get("system.i18n.key.tooLong", MAX_KEY_LENGTH));
        Preconditions.checkArgument(KEY_PATTERN.matcher(targetKey).matches(),
                messages.get("system.i18n.key.invalid"));
        Preconditions.checkArgument(Stream.of(targetKey.split("\\."))
                        .noneMatch(FORBIDDEN_SEGMENTS::contains),
                messages.get("system.i18n.key.invalid"));
        return targetKey;
    }

    private String requireLocale(String locale) {
        Preconditions.checkArgument(SupportedLocale.supports(locale),
                messages.get("system.i18n.locale.unsupported", locale));
        return locale;
    }

    private List<I18nMessageValueDto> validateValues(List<I18nMessageValueDto> values) {
        Preconditions.checkArgument(values != null, messages.get("system.i18n.value.required"));
        Set<String> locales = new HashSet<>();
        for (I18nMessageValueDto value : values) {
            Preconditions.checkArgument(value != null, messages.get("system.i18n.value.required"));
            String locale = requireLocale(value.getLocale());
            value.setValue(StringUtils.trimToNull(value.getValue()));
            Preconditions.checkArgument(locales.add(locale),
                    messages.get("system.i18n.locale.duplicate", locale));
            Preconditions.checkArgument(value.getValue() == null
                            || value.getValue().codePointCount(0, value.getValue().length()) <= MAX_VALUE_LENGTH,
                    messages.get("system.i18n.value.tooLong", MAX_VALUE_LENGTH));
        }
        boolean hasFallback = values.stream().anyMatch(value ->
                "en-US".equals(value.getLocale()) && StringUtils.isNotBlank(value.getValue()));
        Preconditions.checkArgument(hasFallback, messages.get("system.i18n.fallback.required"));
        return values;
    }

    private void validatePathConflict(String targetKey) {
        List<String> existingKeys = lambdaQuery()
                .select(I18nMessageBo::getMessageKey)
                .list()
                .stream()
                .map(I18nMessageBo::getMessageKey)
                .distinct()
                .toList();
        String conflict = existingKeys.stream()
                .filter(key -> !key.equals(targetKey))
                .filter(key -> key.startsWith(targetKey + ".") || targetKey.startsWith(key + "."))
                .findFirst()
                .orElse(null);
        Preconditions.checkCondition(conflict == null,
                messages.get("system.i18n.key.pathConflict", targetKey, conflict));
    }

    private List<I18nMessageBo> findRows(String messageKey) {
        return lambdaQuery()
                .eq(I18nMessageBo::getMessageKey, messageKey)
                .list();
    }

    private void applyValues(Map<String, String> values, List<I18nMessageValueDto> submittedValues) {
        for (I18nMessageValueDto submitted : submittedValues) {
            if (StringUtils.isBlank(submitted.getValue())) {
                values.remove(submitted.getLocale());
            } else {
                values.put(submitted.getLocale(), submitted.getValue());
            }
        }
    }

    private void persistRows(
            String category,
            String messageKey,
            List<I18nMessageBo> existingRows,
            Map<String, String> values) {
        Map<String, I18nMessageBo> existingByLocale = existingRows.stream().collect(Collectors.toMap(
                I18nMessageBo::getLocale,
                Function.identity()));
        for (I18nMessageBo existing : existingRows) {
            String value = values.get(existing.getLocale());
            if (value == null) {
                removeById(existing.getId());
            } else if (!Objects.equals(category, existing.getCategory())
                    || !Objects.equals(value, existing.getI18nValue())) {
                existing.setCategory(category);
                existing.setI18nValue(value);
                updateById(existing);
            }
        }
        values.entrySet().stream()
                .filter(entry -> !existingByLocale.containsKey(entry.getKey()))
                .map(entry -> newRow(category, messageKey, entry.getKey(), entry.getValue()))
                .forEach(this::save);
    }

    private I18nMessageBo newRow(String category, String messageKey, String locale, String value) {
        I18nMessageBo row = new I18nMessageBo();
        row.setCategory(category);
        row.setMessageKey(messageKey);
        row.setLocale(locale);
        row.setI18nValue(value);
        return row;
    }

    private record ValidatedMessage(
            String category,
            String messageKey,
            List<I18nMessageValueDto> values) {
    }

    private List<I18nMessageValueVo> toValues(Collection<I18nMessageBo> rows) {
        Map<String, String> values = rows.stream().collect(Collectors.toMap(
                I18nMessageBo::getLocale,
                I18nMessageBo::getI18nValue,
                (left, right) -> right));
        return toValues(values);
    }

    private List<I18nMessageValueVo> toValues(Map<String, String> values) {
        return SupportedLocale.codes().stream()
                .filter(values::containsKey)
                .map(locale -> new I18nMessageValueVo(locale, values.get(locale)))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private void putPath(Map<String, Object> root, String messageKey, String value) {
        String[] segments = messageKey.split("\\.");
        Map<String, Object> cursor = root;
        for (int index = 0; index < segments.length - 1; index++) {
            cursor = (Map<String, Object>) cursor.computeIfAbsent(
                    segments[index], ignored -> new LinkedHashMap<>());
        }
        cursor.put(segments[segments.length - 1], value);
    }
}
