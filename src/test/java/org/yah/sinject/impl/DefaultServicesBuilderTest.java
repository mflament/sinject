package org.yah.sinject.impl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.yah.sinject.Parametric;
import org.yah.sinject.Service;
import org.yah.sinject.Services;
import org.yah.sinject.TypedServices;
import org.yah.sinject.impl.configurations.ServicesConfiguration;
import org.yah.sinject.impl.configurations.CircularDependency;
import org.yah.sinject.exceptions.CircularDependencyException;
import org.yah.sinject.exceptions.NoSuchServiceException;
import org.yah.sinject.exceptions.ServiceCreationException;
import org.yah.sinject.impl.configurations.ServicesConfiguration.NestedConfiguration;
import org.yah.sinject.impl.configurations.ServicesConfiguration.ServiceC;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SuppressWarnings("SameParameterValue")
public class DefaultServicesBuilderTest {

    private Services parent;
    private Services services;

    @Before
    public void setUp() {
        parent = DefaultServices.builder()
                .declare(String.class).withName("theString").withInstance("the string value")
                .register()
                .build();
        services = DefaultServices.builder()
                .withParent(parent)
                .withName("theServices")
                .declare(ServicesConfiguration.class).withName("theConfiguration").register()
                .build();
    }

    @After
    public void tearDown() {
        if (services != null)
            services.close();
    }

    @Test
    public void test_services_presence() {
        final List<Services> matched = this.services.services(Services.class).stream()
                .map(Service::get)
                .collect(Collectors.toList());
        assertThat(matched, Matchers.containsInAnyOrder(parent, services));
    }

    @Test
    public void test_injection() {
        services.get("theConfiguration", ServicesConfiguration.class);

        final Service<String> theStringService = services.service("theString", String.class);
        assertThat(theStringService, match("theString", String.class));
        String theString = theStringService.get();
        assertThat(theString, is("the string value"));

        assertThat(services.service("serviceA", ServicesConfiguration.ServiceA.class),
                match("serviceA", ServicesConfiguration.ServiceA.class, Integer.MIN_VALUE));
        services.get("serviceA", ServicesConfiguration.ServiceA.class);

        assertThat(services.service(ServicesConfiguration.ServiceB.class), match("serviceB", ServicesConfiguration.ServiceB.class));
        ServicesConfiguration.ServiceB serviceB = services.get(ServicesConfiguration.ServiceB.class);
        assertThat(serviceB.theString, sameInstance(theString));

        assertThat(services.service(ServiceC.class), match("serviceC", ServiceC.class));
        ServiceC serviceC = services.get(ServiceC.class);
        assertThat(serviceC.serviceB, sameInstance(serviceB));

        assertThat(services.service(ServicesConfiguration.ServiceD.class), match("serviceD", ServicesConfiguration.ServiceD.class));
        ServicesConfiguration.ServiceD serviceD = services.get(ServicesConfiguration.ServiceD.class);
        assertThat(serviceD.serviceC, sameInstance(serviceC));

        assertThat(services.service(ServicesConfiguration.ServiceE.class), match("serviceE", ServicesConfiguration.ServiceE.class));
        ServicesConfiguration.ServiceE serviceE = services.get(ServicesConfiguration.ServiceE.class);
        assertThat(serviceE.serviceC, sameInstance(serviceC));
        assertThat(serviceE.serviceD, sameInstance(serviceD));

    }

    @Test
    public void test_injected_class() {
        ServicesConfiguration.ServiceA serviceA = services.get("serviceA", ServicesConfiguration.ServiceA.class);
        ServicesConfiguration.ServiceE serviceE = services.get(ServicesConfiguration.ServiceE.class);

        assertThat(services.service(ServicesConfiguration.ServiceF.class), match("serviceF", ServicesConfiguration.ServiceF.class));
        ServicesConfiguration.ServiceF serviceF = services.get(ServicesConfiguration.ServiceF.class);
        assertThat(serviceF.getServiceA(), sameInstance(serviceA));
        assertThat(serviceF.getServiceE(), sameInstance(serviceE));
    }

    @Test
    public void test_optional_classes() {
        assertThat(services.service("presentServiceA", ServicesConfiguration.ServiceA.class), match("presentServiceA", ServicesConfiguration.ServiceA.class));
        try {
            services.service("emptyServiceA", ServicesConfiguration.ServiceA.class);
            fail("empty service found");
        } catch (NoSuchServiceException e) {
            // expected
        }
    }

    @Test
    public void test_optional_instances() {
        assertThat(services.service("presentInstance", ServicesConfiguration.ServiceB.class), match("presentInstance", ServicesConfiguration.ServiceB.class,1));
        try {
            services.service("emptyInstance", ServicesConfiguration.ServiceB.class);
            fail("empty service found");
        } catch (NoSuchServiceException e) {
            // expected
        }
    }

    @Test
    public void test_nested_configuration() {
        services.get(NestedConfiguration.class);
        services.get("nestedServiceC", ServiceC.class);
    }

    @Test
    public void test_static_string() {
        final String staticString = services.get("staticString", String.class);
        assertThat(staticString, is("static"));
    }

    @Test
    public void test_ambiguity() {
        assertThat(services.service("serviceA", ServicesConfiguration.ServiceA.class),
                match("serviceA", ServicesConfiguration.ServiceA.class, Integer.MIN_VALUE));
        ServicesConfiguration.ServiceA primaryServiceA = services.get("serviceA", ServicesConfiguration.ServiceA.class);

        assertThat(services.service("secondaryServiceA", ServicesConfiguration.ServiceA.class),
                match("secondaryServiceA", ServicesConfiguration.ServiceA.class));
        ServicesConfiguration.ServiceA secondaryServiceA = services.get("secondaryServiceA", ServicesConfiguration.ServiceA.class);
        assertThat(secondaryServiceA, not(primaryServiceA));

        assertThat(services.service(ServicesConfiguration.ServiceA.class).get(), sameInstance(primaryServiceA));
    }

    @Test
    public void test_optional_dependency() {
        final ServiceC serviceC = services.service(ServiceC.class).get();
        assertThat(serviceC.stringSupplier, is(services.get("stringSupplier", Supplier.class)));
        assertThat(serviceC.doubleSupplier, nullValue());
    }

    @Test
    public void test_generic() {
        final TypedServices<Supplier<?>> suppliers = this.services.services(Supplier.class);
        assertThat(suppliers, Matchers.containsInAnyOrder(List.of(
                match("stringSupplier", Supplier.class),
                match("intSupplier", Supplier.class)
        )));

        final TypedServices<Supplier<String>> stringSuppliers = this.services.services(new Parametric<>() {
        });
        assertThat(stringSuppliers, Matchers.contains(match("stringSupplier", Supplier.class)));
    }

    @Test
    public void test_circular_dependency() {
        try {
            DefaultServices.builder()
                    .declare(CircularDependency.class).register()
                    .build();
            fail("no error ?");
        } catch (ServiceCreationException e) {
            assertThat(e, isCausedBy(CircularDependencyException.class));
            // all good
        }
    }

    private Matcher<ServiceCreationException> isCausedBy(Class<? extends Throwable> expectedCause) {
        return new TypeSafeMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendValue(expectedCause);
            }

            @Override
            protected boolean matchesSafely(ServiceCreationException item) {
                final Throwable rootCause = ExceptionUtils.getRootCause(item);
                System.out.println(rootCause.getMessage());
                return expectedCause.isAssignableFrom(rootCause.getClass());
            }
        };
    }

    private static <T> Matcher<Service<?>> match(String name, Class<T> type) {
        return match(name, type, 0);
    }

    private static <T> Matcher<Service<?>> match(String name, Class<T> type, int priority) {
        return new TypeSafeMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(new DefaultServiceDefinition(type, name, priority).toString());
            }

            @Override
            protected boolean matchesSafely(Service<?> item) {
                return item.match(name, type) && item.priority() == priority;
            }
        };
    }

}