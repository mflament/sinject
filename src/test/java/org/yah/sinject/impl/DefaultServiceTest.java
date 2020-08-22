package org.yah.sinject.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.yah.sinject.impl.DefaultService;
import org.yah.sinject.impl.DefaultServices;
import org.yah.sinject.builder.InstanceSupplier;
import org.yah.sinject.builder.ServiceDeclaration;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultServiceTest {

    private TestService serviceInstance;

    private DefaultServices source;

    @Mock
    private InstanceSupplier<TestService> instanceSupplier;

    private ServiceDeclaration<TestService> declaration;

    private DefaultService<TestService> service;

    @Before
    public void setUp() throws Exception {
        serviceInstance = new TestService();
        source = new DefaultServices();

        //noinspection unchecked
        declaration = mock(ServiceDeclaration.class);
        when(declaration.name()).thenReturn("theService");
        when(declaration.type()).thenReturn(TestService.class);
        when(declaration.priority()).thenReturn(10);

        when(instanceSupplier.get()).thenReturn(serviceInstance);

        service = new DefaultService<>(source, declaration, instanceSupplier);
    }

    @Test
    public void constructor() {
        assertThat(service.getSource(), sameInstance(source));
        assertThat(service.type(), is(declaration.type()));
        assertThat(service.name(), is(declaration.name()));
        assertThat(service.priority(), is(declaration.priority()));
    }

    @Test
    public void get() {
        assertThat(service.get(), sameInstance(serviceInstance));
    }

    @Test
    public void peek() {
        assertThat(service.peek(), is(Optional.empty()));
        service.get();
        assertThat(service.peek(), is(Optional.of(serviceInstance)));
    }

    private static class TestService {
    }
}