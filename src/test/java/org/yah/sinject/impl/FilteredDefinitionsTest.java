package org.yah.sinject.impl;

import org.junit.Test;
import org.yah.sinject.Service;
import org.yah.sinject.ServiceDefinition;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FilteredDefinitionsTest {

    @Test
    public void test_filter() {
        List<Service<?>> services = new ArrayList<>();
        services.add(service("A", String.class));
        services.add(service("B", String.class));
        services.add(service("C", Collection.class));
        services.add(service("D", List.class));
        services.add(service("E", Set.class));
        services.add(service("F", Long.class));
        services.add(service("G", String.class));

        assertThat(FilteredServices.create(services, Object.class),
                contains(services.toArray(Service[]::new)));

        assertThat(filter(services, String.class), is(List.of("A", "B", "G")));
        assertThat(filter(services, Collection.class), is(List.of("C", "D", "E")));
        assertThat(filter(services, List.class), is(List.of("D")));
        assertThat(filter(services, Set.class), is(List.of("E")));
        assertThat(filter(services, Long.class), is(List.of("F")));
    }

    private List<String> filter(List<Service<?>> services, Class<?> type) {
        return FilteredServices.create(services, type).stream()
                .map(ServiceDefinition::name)
                .collect(Collectors.toList());
    }

    private Service<?> service(String name, Type type) {
        final Service<?> service = mock(Service.class);
        when(service.name()).thenReturn(name);
        when(service.type()).thenReturn(type);
        when(service.toString()).thenReturn(name + " (" + type + ")");
        return service;
    }
}