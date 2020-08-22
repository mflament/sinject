package org.yah.sinject.builder;


import org.yah.sinject.impl.builder.ResolvedDependencies;

@FunctionalInterface
public interface ServiceInstanceFactory<T> {

    T create(ResolvedDependencies dependencies) throws Exception;

}
