package org.yah.sinject;

import java.util.Optional;

public interface Service<T> extends ServiceDefinition, AutoCloseable {

    /**
     * Get or create the instance of this service.
     *
     * @return the instance for this service, creating it if not yet done.
     */
    T get();

    /**
     * @return the instance of this service if created, empty otherwise.
     */
    Optional<T> peek();

    Services getSource();

    @Override
    void close();
}
