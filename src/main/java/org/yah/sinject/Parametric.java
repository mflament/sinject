package org.yah.sinject;

import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;

public abstract class Parametric<T> {
    private final Type type;

    public Parametric() {
        final Iterator<Type> iterator = TypeUtils.getTypeArguments(getClass(), Parametric.class)
                .values()
                .iterator();
        type = iterator.hasNext() ? iterator.next() : Object.class;
    }

    public final Type getType() {
        return type;
    }

    public Class<T> asClass() {
        Type t = type;
        if (t instanceof ParameterizedType)
            t = ((ParameterizedType) type).getRawType();
        //noinspection unchecked
        return (Class<T>) t;
    }
}
