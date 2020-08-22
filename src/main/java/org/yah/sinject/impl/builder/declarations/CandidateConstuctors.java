package org.yah.sinject.impl.builder.declarations;

import org.apache.commons.lang3.StringUtils;
import org.yah.sinject.annotations.Service;
import org.yah.sinject.ServiceResolver;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CandidateConstuctors<T> {

    public static <T> CandidateConstuctors<T> create(Class<T> type) {
        final Constructor<T> annotated = constructors(type)
                .filter(constructor -> constructor.getAnnotation(Service.class) != null)
                .findFirst().orElse(null);
        List<CandidateConstuctor<T>> candidates;
        String name = null;
        Integer priority = null;
        if (annotated != null) {
            Service annotation = annotated.getAnnotation(Service.class);
            name = StringUtils.trimToNull(annotation.name());
            if (name == null)
                name = StringUtils.trimToNull(annotation.value());
            priority = annotation.priority();
            candidates = Collections.singletonList(CandidateConstuctor.create(annotated));
        } else {
            candidates = constructors(type)
                    .map(CandidateConstuctor::create)
                    .sorted()
                    .collect(Collectors.toList());
        }
        return new CandidateConstuctors<>(name, priority, candidates);
    }

    private final String name;
    private final Integer priority;
    private final List<CandidateConstuctor<T>> candidates;

    private CandidateConstuctors(String name, Integer priority, List<CandidateConstuctor<T>> candidates) {
        this.name = name;
        this.priority = priority;
        this.candidates = candidates;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public OptionalInt getPriority() {
        return priority != null ? OptionalInt.of(priority) : OptionalInt.empty();
    }

    public Optional<ResolvedConstructor<T>> resolve(ServiceResolver resolver) {
        return candidates.stream()
                .map(candidate -> candidate.resolve(resolver))
                .sorted()
                .filter(ResolvedConstructor::isResolved)
                .findFirst();
    }

    private static <T> Stream<Constructor<T>> constructors(Class<T> type) {
        //noinspection unchecked
        return Arrays.stream(type.getDeclaredConstructors()).map(c -> (Constructor<T>) c);
    }

}
