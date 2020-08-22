package org.yah.sinject.exceptions;


import org.yah.sinject.ServiceDefinition;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class ConflictingServicesException extends ServiceResolutionException {
    private final List<ServiceDefinition> candidates;

    public ConflictingServicesException(String name, Type type, Collection<? extends ServiceDefinition> candidates) {
        super(name, type, "conflicting definitions " + candidates);
        this.candidates = List.copyOf(candidates);
    }

    public List<ServiceDefinition> getCandidates() {
        return candidates;
    }
}
