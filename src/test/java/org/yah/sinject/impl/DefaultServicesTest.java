package org.yah.sinject.impl;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.yah.sinject.Service;
import org.yah.sinject.impl.DefaultServices;
import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.exceptions.ConflictingServicesException;
import org.yah.sinject.exceptions.DuplicateServiceException;
import org.yah.sinject.exceptions.NoSuchServiceException;
import org.yah.sinject.impl.builder.declarations.DefaultServiceDeclaration;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DefaultServicesTest {

    private DefaultServices services;

    @Before
    public void setUp() {
        services = new DefaultServices();
    }

    @Test
    public void add_correct_service() {
        final Service<TestService> a = service(new TestService("A"));
        // check created service definition
        assertThat(a.match("A", TestService.class), is(true));
        assertThat(a.priority(), is(0));
        assertThat(a.getSource(), is(services));
        // check that service has not been instantiated
        assertThat(a.peek(), is(Optional.empty()));
    }

    @Test
    public void add_sorted() {
        assertThat(services, emptyIterable());

        final Service<TestService> a = service(new TestService("A"));
        // check content
        assertThat(services, contains(a));

        // tail insertion
        final Service<TestService> b = service(new TestService("B", 10));
        assertThat(services, Matchers.contains(a, b));

        // head insertion
        final Service<TestService> c = service(new TestService("C", -10));
        assertThat(services, Matchers.contains(c, a, b));

        // after existing insertion
        final Service<TestService> d = service(new TestService("D", 0));
        assertThat(services, Matchers.contains(c, a, d, b));

        // after existing at end
        final Service<TestService> e = service(new TestService("E", 10));
        assertThat(services, Matchers.contains(c, a, d, b, e));
    }

    @Test
    public void add_conflict() {
        service(new TestService("A"));
        service(new TestService("B"));
        service(new TestService("C"));
        try {
            service(new TestService("A"));
            fail("Should have failed");
        } catch (DuplicateServiceException e) {
            // expected
        }
        // test with parent
        services = new DefaultServices(services);
        try {
            service(new TestService("A"));
            fail("Should have failed");
        } catch (DuplicateServiceException e) {
            // expected
        }
    }

    @Test
    public void with_parent() {
        final DefaultServices parent = new DefaultServices();
        parent.add(service(new TestService("A", 5)));
        parent.add(service(new TestService("B", -4)));
        parent.add(service(new TestService("C", 2)));

        services = new DefaultServices(parent);
        service(new TestService("D", -10));
        service(new TestService("E", 5));
        service(new TestService("F", 10));
        service(new TestService("G", 1));

        final List<String> names = services.stream().map(Service::name).collect(Collectors.toList());
        assertThat(names, contains("D", "B", "G", "C", "A", "E", "F"));
    }

    @Test(expected = IllegalStateException.class)
    public void freeze() {
        services.freeze();
        service("A", 5, "a+5");
    }

    @Test
    public void resolution_from_type() {
        service("A", 5, "a+5");
        service("A", 2, "a+2");
        service("A", 0, 0L);
        service("A", -5, 42L, Number.class);

        // preparedService("B" , -10, "b-10");
        // preparedService("B" , 0, 10);

        Service<?> service = services.service(String.class);
        assertThat(service.get(), is("a+2"));

        service = services.service(Long.class);
        assertThat(service.get(), is(0L));

        service = services.service(Number.class);
        assertThat(service.get(), is(42L));
    }

    @Test
    public void resolution_from_name_and_type() {
        service("A", 5, "a+5");
        service("A", 2, "a+2");
        service("A", 0, 0L);
        service("A", -5, 42, Number.class);

        service("B", -10, "b-10");
        service("B", 0, 10, Number.class);
        service("B", 10, 100);

        Service<?> service = services.service("A", String.class);
        assertThat(service.get(), is("a+2"));

        service = services.service("A", Long.class);
        assertThat(service.get(), is(0L));

        service = services.service("A", Number.class);
        assertThat(service.get(), is(42));

        service = services.service("B", String.class);
        assertThat(service.get(), is("b-10"));

        service = services.service("B", Number.class);
        assertThat(service.get(), is(10));

        service = services.service("B", Integer.class);
        assertThat(service.get(), is(100));
    }

    @Test(expected = ConflictingServicesException.class)
    public void resolution_conflict() {
        service("A", 0, "a");
        service("B", 0, "b");
        services.service(String.class);
    }

    @Test
    public void resolution_not_found() {
        service("A", 0, "a");
        service("B", 0, "b");

        try {
            services.service("C", String.class);
            fail("service was found");
        } catch (NoSuchServiceException e) {
            // expected
        }

        try {
            services.service(Long.class);
            fail("service was found");
        } catch (NoSuchServiceException e) {
            // expected
        }
    }

    private <T> void service(String name, int priority, T instance) {
        //noinspection unchecked
        service(name, priority, instance, (Class<T>) instance.getClass());
    }

    private <T> void service(String name, int priority, T instance, Class<T> type) {
        final DefaultServiceDeclaration<T> declaration = DefaultServiceDeclaration.builder(type)
                .withName(name)
                .withPriority(priority)
                .withFactory(services -> instance)
                .build();
        services.add(declaration, () -> instance);
    }

    private Service<TestService> service(TestService serviceInstance) {
        final ServiceDeclaration<TestService> declaration = DefaultServiceDeclaration.builder(TestService.class)
                .withName(serviceInstance.name)
                .withPriority(serviceInstance.priority)
                .withFactory(dependencies -> serviceInstance)
                .build();
        return services.add(declaration, () -> serviceInstance);
    }

    private static class TestService {
        private final String name;
        private final int priority;

        public TestService(String name) {
            this(name, 0);
        }

        public TestService(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
    }
}
