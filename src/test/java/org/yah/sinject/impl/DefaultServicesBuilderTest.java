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
import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.builder.ServiceDeclarationTransformer;
import org.yah.sinject.exceptions.CircularDependencyException;
import org.yah.sinject.exceptions.NoSuchServiceException;
import org.yah.sinject.exceptions.ServiceCreationException;
import org.yah.sinject.impl.configurations.CircularDependency;
import org.yah.sinject.impl.configurations.ServicesConfiguration;
import org.yah.sinject.impl.configurations.ServicesConfiguration.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.yah.sinject.impl.DefaultServices.builder;

@SuppressWarnings("SameParameterValue")
public class DefaultServicesBuilderTest {

    private Services parent;
    private Services services;

    @Before
    public void setUp() {
        parent = builder()
                .declare(String.class).withName("theString").withInstance("the string value").register()
                .declare(InstanceTransformer.class).withName("parentInstanceTransformer").withInstance(new InstanceTransformer()).register()
                .declare(DeclarationTransformer.class).withName("parentInvocationTransformer").withInstance(new DeclarationTransformer()).register()
                .build();
        services = builder()
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
    public void test_services_not_closable() {
        final List<Services> matched = this.services.services(Services.class).stream()
                                                    .map(Service::get)
                                                    .collect(Collectors.toList());
        matched.forEach(Services::close);
        ServiceD serviceD = this.services.service(ServiceD.class).get();
        assertThat(serviceD.closed, is(false));
    }

    @Test
    public void test_closable() {
        final ServiceD serviceD = this.services.service(ServiceD.class).get();
        assertThat(serviceD.closed, is(false));
        services.close();
        assertThat(serviceD.closed, is(true));
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

        assertThat(services
                           .service(ServicesConfiguration.ServiceB.class), match("serviceB", ServicesConfiguration.ServiceB.class));
        ServicesConfiguration.ServiceB serviceB = services.get(ServicesConfiguration.ServiceB.class);
        assertThat(serviceB.theString, sameInstance(theString));

        assertThat(services.service(ServiceC.class), match("serviceC", ServiceC.class));
        ServiceC serviceC = services.get(ServiceC.class);
        assertThat(serviceC.serviceB, sameInstance(serviceB));

        assertThat(services
                           .service(ServiceD.class), match("serviceD", ServiceD.class));
        ServiceD serviceD = services.get(ServiceD.class);
        assertThat(serviceD.serviceC, sameInstance(serviceC));

        assertThat(services
                           .service(ServicesConfiguration.ServiceE.class), match("serviceE", ServicesConfiguration.ServiceE.class));
        ServicesConfiguration.ServiceE serviceE = services.get(ServicesConfiguration.ServiceE.class);
        assertThat(serviceE.serviceC, sameInstance(serviceC));
        assertThat(serviceE.serviceD, sameInstance(serviceD));

    }

    @Test
    public void test_injected_class() {
        ServicesConfiguration.ServiceA serviceA = services.get("serviceA", ServicesConfiguration.ServiceA.class);
        ServicesConfiguration.ServiceE serviceE = services.get(ServicesConfiguration.ServiceE.class);

        assertThat(services
                           .service(ServicesConfiguration.ServiceF.class), match("serviceF", ServicesConfiguration.ServiceF.class));
        ServicesConfiguration.ServiceF serviceF = services.get(ServicesConfiguration.ServiceF.class);
        assertThat(serviceF.getServiceA(), sameInstance(serviceA));
        assertThat(serviceF.getServiceE(), sameInstance(serviceE));
    }

    @Test
    public void test_optional_classes() {
        assertThat(services
                           .service("presentServiceA", ServicesConfiguration.ServiceA.class), match("presentServiceA", ServicesConfiguration.ServiceA.class));
        try {
            services.service("emptyServiceA", ServicesConfiguration.ServiceA.class);
            fail("empty service found");
        } catch (NoSuchServiceException e) {
            // expected
        }
    }

    @Test
    public void test_optional_instances() {
        assertThat(services
                           .service("presentInstance", ServicesConfiguration.ServiceB.class), match("presentInstance", ServicesConfiguration.ServiceB.class, 1));
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
        ServicesConfiguration.ServiceA secondaryServiceA = services
                .get("secondaryServiceA", ServicesConfiguration.ServiceA.class);
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
        final TypedServices<Supplier<?>> suppliers = services.services(Supplier.class);
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
            builder()
                    .declare(CircularDependency.class).register()
                    .build();
            fail("no error ?");
        } catch (ServiceCreationException e) {
            assertThat(e, isCausedBy(CircularDependencyException.class));
            // all good
        }
    }

    @Test
    public void test_declaration_tranformers() {
        final Service<DeclarationTransformer> parentInvocationTransformer = services.service(
                "parentInvocationTransformer", DeclarationTransformer.class);
        List<ServiceDeclaration<?>> actuals = parentInvocationTransformer.get().getTransformeds();
        assertThat(actuals, containsAllExcept(s -> s == parentInvocationTransformer));

        final Service<DeclarationTransformer> declarationTransformer1 = services.service(
                "declarationTransformer1", DeclarationTransformer.class);
        actuals = declarationTransformer1.get().getTransformeds();
        assertThat(actuals, containsAllExcept(s -> s == declarationTransformer1 ||
                s.getSource().equals(parent) ||
                s.isAssignableTo(ServicesConfiguration.class)));

        final Service<DeclarationTransformer> declarationTransformer2 = services.service(
                "declarationTransformer2", DeclarationTransformer.class);
        actuals = declarationTransformer2.get().getTransformeds();
        assertThat(actuals, containsAllExcept(s -> s == declarationTransformer2 ||
                s == declarationTransformer1 ||
                s.getSource().equals(parent) ||
                s.isAssignableTo(ServicesConfiguration.class)));
    }

    @Test
    public void test_instance_tranformers() {
        Service<InstanceTransformer> parentInstanceTransformer =
                services.service("parentInstanceTransformer",
                                 InstanceTransformer.class);

        // create all service instances
        services.forEach(Service::get);

        List<ServiceDeclaration<?>> actuals = parentInstanceTransformer.get().getTransformeds();
        assertThat(actuals, containsAllExcept(s -> s == parentInstanceTransformer ||
                s.match("parentInvocationTransformer", DeclarationTransformer.class)
        ));

        Service<InstanceTransformer> instanceTransformer1 = services.service("instanceTransformer1",
                                                                             InstanceTransformer.class);
        actuals = instanceTransformer1.get().getTransformeds();
        assertThat(actuals, containsAllExcept(s -> s == instanceTransformer1 ||
                s.getSource().equals(parent) ||
                s.match("theConfiguration", ServicesConfiguration.class) ||
                s.isAssignableTo(ServiceDeclarationTransformer.class)
        ));

        Service<InstanceTransformer> instanceTransformer2 = services.service("instanceTransformer2",
                                                                             InstanceTransformer.class);
        actuals = instanceTransformer2.get().getTransformeds();
        assertThat(actuals, containsAllExcept(s -> s == instanceTransformer2 ||
                s == instanceTransformer1 ||
                s.getSource().equals(parent) ||
                s.match("theConfiguration", ServicesConfiguration.class) ||
                s.isAssignableTo(ServiceDeclarationTransformer.class)
        ));
    }

    private Matcher<Iterable<ServiceDeclaration<?>>> containsAllExcept(Predicate<Service<?>> predicate) {
        final List<Service<?>> unexpecteds = services.stream()
                                                     .filter(predicate)
                                                     .collect(Collectors.toList());
        return new TypeSafeMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("all services excepted ").appendValue(unexpecteds);
            }

            @Override
            protected boolean matchesSafely(Iterable<ServiceDeclaration<?>> actuals) {
                List<Service<?>> remainings = new ArrayList<>();
                services.stream()
                        .filter(s -> !unexpecteds.contains(s))
                        .forEach(remainings::add);
                for (ServiceDeclaration<?> actual : actuals) {
                    boolean found = remainings.removeIf(s -> s.match(actual));
                    if (!found && safeGet(actual) != null) {
                        // optional service must not be declared
                        return false;
                    }
                }
                return remainings.isEmpty();
            }

            @Override
            protected void describeMismatchSafely(Iterable<ServiceDeclaration<?>> actuals, Description mismatchDescription) {
                List<Service<?>> remainings = new ArrayList<>();
                services.stream()
                        .filter(Predicate.not(predicate))
                        .forEach(remainings::add);
                for (ServiceDeclaration<?> actual : actuals) {
                    boolean found = remainings.removeIf(s -> s.match(actual));
                    final Service<?> actualService = safeGet(actual);
                    if (!found && actualService != null) {
                        mismatchDescription.appendText("missing").appendValue(actualService);
                        return;
                    }
                }
                if (!remainings.isEmpty()) {
                    mismatchDescription.appendText("unexpecteds" + remainings);
                }
            }
        };
    }

    private <T> Service<T> safeGet(ServiceDeclaration<T> declaration) {
        try {
            //noinspection unchecked
            return (Service<T>) services.service(declaration.name(), declaration.type());
        } catch (NoSuchServiceException e) {
            return null;
        }
    }

    private static Matcher<Service<?>> match(String name, Class<?> type) {
        return match(name, type, 0);
    }

    private static Matcher<Service<?>> match(String name, Type type, int priority) {
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

    private static Matcher<ServiceCreationException> isCausedBy(Class<? extends Throwable> expectedCause) {
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

}