package org.yah.sinject.impl;

import org.yah.sinject.Parametric;
import org.yah.sinject.Service;
import org.yah.sinject.Services;
import org.yah.sinject.TypedServices;
import org.yah.sinject.builder.InstanceSupplier;
import org.yah.sinject.builder.ServiceDeclaration;
import org.yah.sinject.exceptions.ConflictingServicesException;
import org.yah.sinject.exceptions.DuplicateServiceException;
import org.yah.sinject.exceptions.NoSuchServiceException;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultServices implements Services {

    public static DefaultServicesBuilder builder() {
        return new DefaultServicesBuilder();
    }

    /**
     * sorted by priority
     * Use {@link java.util.RandomAccess} collection to ensure fast insert position lookup
     * However this will make the insertion ugly
     */
    protected final List<Service<?>> services = new ArrayList<>();

    private boolean freezed;

    public DefaultServices() {
        this(null);
    }

    public DefaultServices(Services parent) {
        if (parent != null) {
            parent.forEach(this::add);
        }
    }

    @Override
    @Nonnull
    public Iterator<Service<?>> iterator() {
        return services.iterator();
    }

    @Override
    public Service<?> service(String name, Type type) throws NoSuchServiceException, ConflictingServicesException {
        final Iterator<Service<?>> iterator = services.stream()
                .filter(s -> s.match(name, type))
                .collect(Collectors.toList())
                .iterator();
        if (iterator.hasNext()) {
            Service<?> first = iterator.next();
            if (iterator.hasNext()) {
                Service<?> next = iterator.next();
                if (next.priority() == first.priority()) {
                    List<Service<?>> conflictingCandidates = new ArrayList<>();
                    conflictingCandidates.add(first);
                    conflictingCandidates.add(next);
                    while (iterator.hasNext()) {
                        next = iterator.next();
                        if (next.priority() == first.priority())
                            conflictingCandidates.add(next);
                    }
                    throw new ConflictingServicesException(name, type, conflictingCandidates);
                }
            }
            return first;
        }
        throw new NoSuchServiceException(name, type);
    }

    @Override
    public <V> TypedServices<V> services(Class<? super V> type) {
        return FilteredServices.create(services, type);
    }

    @Override
    public <V> TypedServices<V> services(Parametric<? super V> parametric) {
        return FilteredServices.create(services, parametric);
    }

    @Override
    public void close() {
        services.forEach(Service::close);
    }

    public <T> Service<T> add(ServiceDeclaration<T> declaration, InstanceSupplier<? extends T> supplier) {
        final DefaultService<T> service = new DefaultService<>(this, declaration, supplier);
        add(service);
        return service;
    }

    /**
     * Add a new service, checking for type & name & priority conflict
     * Using priority here is to make explicit the override of one service by another by changing priority
     *
     * @param service the service to add
     * @throws DuplicateServiceException if a service assignable to the new service type and with the same name
     *                                   and priority already exists.
     */
    public void add(Service<?> service) throws DuplicateServiceException {
        if (freezed)
            throw new IllegalStateException("freezed services can not be edited");

        final Service<?> other = services.stream()
                .filter(s -> isConflicting(s, service))
                .findFirst()
                .orElse(null);
        if (other != null)
            throw new DuplicateServiceException(other, service);

        int index = Collections.binarySearch(services, service);
        if (index < 0)
            index = -index - 1;
        else {
            // insert after other equivalent priority
            while (index < services.size() && services.get(index).priority() == service.priority())
                index++;
        }
        services.add(index, service);
    }

    public void freeze() {
        freezed = true;
    }

    private static boolean isConflicting(Service<?> current, Service<?> newService) {
        return current.match(newService) && current.priority() == newService.priority();
    }

}
