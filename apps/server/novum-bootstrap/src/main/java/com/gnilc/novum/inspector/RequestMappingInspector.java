package com.gnilc.novum.inspector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Logs the request mappings registered by Spring MVC after application startup.
 */
@Component
public class RequestMappingInspector {
    private static final Logger log = LoggerFactory.getLogger(RequestMappingInspector.class);
    private static final String ANY_METHOD = "*";
    private static final Comparator<MappingLogEntry> LOG_ORDER = Comparator
            .comparing(MappingLogEntry::method)
            .thenComparing(MappingLogEntry::path)
            .thenComparing(MappingLogEntry::handler);

    private final RequestMappingHandlerMapping handlerMapping;

    public RequestMappingInspector(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    /**
     * Logs every path and HTTP method combination once the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logMappings() {
        List<MappingLogEntry> mappings = inspectMappings();
        mappings.forEach(mapping -> log.info(
                "Request mapping: {} {} -> {}", mapping.method(), mapping.path(), mapping.handler()));
        log.info("Discovered {} request mappings", mappings.size());
    }

    List<MappingLogEntry> inspectMappings() {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .flatMap(entry -> expand(entry.getKey(), entry.getValue()))
                .sorted(LOG_ORDER)
                .toList();
    }

    private Stream<MappingLogEntry> expand(RequestMappingInfo mapping, HandlerMethod handlerMethod) {
        Set<String> paths = mapping.getPatternValues();
        Set<RequestMethod> requestMethods = mapping.getMethodsCondition().getMethods();
        Stream<String> methods = requestMethods.isEmpty()
                ? Stream.of(ANY_METHOD)
                : requestMethods.stream().map(RequestMethod::name);
        String handler = handlerMethod.getBeanType().getName() + "#" + handlerMethod.getMethod().getName();

        return methods.flatMap(method -> paths.stream()
                .map(path -> new MappingLogEntry(method, path, handler)));
    }

    record MappingLogEntry(String method, String path, String handler) {
    }
}
