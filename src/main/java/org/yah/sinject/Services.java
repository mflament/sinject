package org.yah.sinject;

import org.yah.sinject.exceptions.ConflictingServicesException;
import org.yah.sinject.exceptions.NoSuchServiceException;

import java.lang.reflect.Type;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Services extends ServiceResolver, Iterable<Service<?>>, AutoCloseable {

    <V> TypedServices<V> services(Class<? super V> type);

    <V> TypedServices<V> services(Parametric<? super V> parametric);

    default Stream<Service<?>> stream() {
        return stream(false);
    }

    default Stream<Service<?>> stream(boolean parallel) {
        return StreamSupport.stream(spliterator(), parallel);
    }

    default Service<?> service(Type type) throws NoSuchServiceException, ConflictingServicesException {
        return service(null, type);
    }

    default <T> Service<T> service(String name, Class<T> type) {
        //noinspection unchecked
        return (Service<T>) service(name, (Type) type);
    }

    default <T> Service<T> service(Class<T> type) {
        //noinspection unchecked
        return (Service<T>) service(null, (Type) type);
    }

    default <T> Service<T> service(Parametric<T> parametric) {
        //noinspection unchecked
        return (Service<T>) service(parametric.getType());
    }

    default <T> Service<T> service(String name, Parametric<T> parametric) {
        //noinspection unchecked
        return (Service<T>) service(name, parametric.getType());
    }

    default <T> T get(String name, Class<T> type) {
        return service(name, type).get();
    }

    default <T> T get(Class<T> type) {
        return service(type).get();
    }

    default <T> T get(String name, Parametric<T> parametric) {
        return service(name, parametric).get();
    }

    default <T> T get(Parametric<T> parametric) {
        return service(parametric).get();
    }

    @Override
    void close();

}
