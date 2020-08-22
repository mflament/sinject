package org.yah.sinject;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface TypedServices<T> extends Iterable<Service<T>> {

    <V> TypedServices<V> services(Class<? super V> type);

    <V> TypedServices<V> services(Parametric<? super V> parametric);

    default Stream<Service<T>> stream() {
        return stream(false);
    }

    default Stream<Service<T>> stream(boolean parallel) {
        return StreamSupport.stream(spliterator(), parallel);
    }

}
