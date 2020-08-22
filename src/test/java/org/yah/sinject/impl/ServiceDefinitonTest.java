package org.yah.sinject.impl;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.yah.sinject.ServiceDefinition;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

public class ServiceDefinitonTest {

    @Test
    public void test_isAssignableTo() {
        ServiceDefinition definiton = definition("test", 0, String.class);
        assertThat(definiton.isAssignableTo(String.class), is(true));
        assertThat(definiton.isAssignableTo(Object.class), is(true));
        assertThat(definiton.isAssignableTo(Long.class), is(false));

        definiton = definition("test", 0, Integer.class);
        assertThat(definiton.isAssignableTo(Object.class), is(true));
        assertThat(definiton.isAssignableTo(Integer.class), is(true));
        assertThat(definiton.isAssignableTo(Number.class), is(true));
        assertThat(definiton.isAssignableTo(Long.class), is(false));
        assertThat(definiton.isAssignableTo(String.class), is(false));

        ParameterizedType parameterizedType = TypeUtils.parameterize(Supplier.class, String.class);
        definiton = definition("test", 0, parameterizedType);
        assertThat(definiton.isAssignableTo(parameterizedType), is(true));
        assertThat(definiton.isAssignableTo(Supplier.class), is(true));
        parameterizedType = TypeUtils.parameterize(Supplier.class, Long.class);
        assertThat(definiton.isAssignableTo(parameterizedType), is(false));
    }

    @Test
    public void test_sortOrder() {
        List<ServiceDefinition> definitons = new ArrayList<>(List.of(
                definition("d", 5),
                definition("b", -5),
                definition("e", 100),
                definition("c", 0),
                definition("a", -10)
        ));
        final List<String> names = definitons.stream()
                .sorted()
                .map(ServiceDefinition::name)
                .collect(Collectors.toList());
        assertThat(names, contains("a", "b", "c", "d", "e"));
    }

    protected ServiceDefinition definition(String name, int priority) {
        return definition(name, priority, String.class);
    }

    protected ServiceDefinition definition(String name, int priority, Type type) {
        return new ServiceDefinition() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Type type() {
                return type;
            }

            @Override
            public int priority() {
                return priority;
            }
        };
    }
}