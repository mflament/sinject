package org.yah.sinject.builder;

import org.yah.sinject.ServiceDefinition;
import org.yah.sinject.ServiceResolver;

import java.util.Optional;

public interface ServiceDeclaration<T> extends ServiceDefinition {

    Optional<InstanceSupplier<? extends T>> createInstanceSupplier(ServiceResolver resolver);

}
