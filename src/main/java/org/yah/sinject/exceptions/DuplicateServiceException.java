package org.yah.sinject.exceptions;


import org.yah.sinject.Service;

import java.util.Objects;

public class DuplicateServiceException extends ServiceCreationException {
    private final Service<?> currentService;
    private final Service<?> newService;

    public DuplicateServiceException(Service<?> currentService, Service<?> newService) {
        super(newService, "conflicting with " + currentService);
        this.currentService = Objects.requireNonNull(currentService, "currentService is null");
        this.newService = Objects.requireNonNull(newService, "newService is null");
    }

    public Service<?> getCurrentService() {
        return currentService;
    }

    public Service<?> getNewService() {
        return newService;
    }
}
