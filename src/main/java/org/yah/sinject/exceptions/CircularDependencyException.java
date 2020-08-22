package org.yah.sinject.exceptions;


import org.yah.sinject.ServiceDefinition;

import java.util.List;

public class CircularDependencyException extends ServiceCreationException {

    public CircularDependencyException(List<? extends ServiceDefinition> definitions) {
        super(definitions.get(definitions.size() - 1), createMessage(definitions));
    }

    private static String createMessage(List<? extends ServiceDefinition> definitions) {
        ServiceDefinition current = definitions.get(definitions.size() - 1);
        final StringBuilder sb = new StringBuilder();
        for (ServiceDefinition service : definitions) {
            if (sb.length() > 0)
                sb.append(" -> ");
            if (service == current)
                sb.append(" [ ");
            sb.append(service.name());
        }
        sb.append(" -> ").append(current.name()).append(" ]");
        return sb.toString();
    }
}
