package org.yah.sinject.exceptions;

import java.lang.reflect.Type;

public class NoSuchServiceException extends ServiceResolutionException {

    public NoSuchServiceException(String name, Type type) {
        super(name, type, " no matching service found");
    }

    public NoSuchServiceException(Type type) {
        this(null, type);
    }

}
