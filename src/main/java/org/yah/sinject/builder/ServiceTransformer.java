package org.yah.sinject.builder;

public interface ServiceTransformer {
    <T> ServiceDeclaration<T> transform(ServiceDeclaration<T> declaration);
}
