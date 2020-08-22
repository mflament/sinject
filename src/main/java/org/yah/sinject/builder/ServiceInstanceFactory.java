package org.yah.sinject.builder;


import org.yah.sinject.impl.builder.ResolvedDependencies;

@SuppressWarnings("RedundantThrows")
@FunctionalInterface
public interface ServiceInstanceFactory<T> {

    T create(ResolvedDependencies dependencies) throws Exception;

}
