package org.yah.sinject.builder;

public interface ServiceInstanceTransformer {
    <T> T transform(ServiceDeclaration<? super T> declaration, T instance);
}
