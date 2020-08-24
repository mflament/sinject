package org.yah.sinject.builder;

public interface ServiceDeclarationTransformer {
    ServiceDeclaration<?> transform(ServiceDeclaration<?> declaration);
}
