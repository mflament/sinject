package org.yah.sinject.impl.builder.declarations;

import org.yah.sinject.builder.ServiceDeclaration;

import java.lang.reflect.Type;
import java.util.Objects;

public abstract class AbstractServiceDeclaration<T> implements ServiceDeclaration<T> {

    private final Type type;
    private final String name;
    private final int priority;

    protected AbstractServiceDeclaration(AbstractBuilder<T, ?> builder) {
        this.type = builder.type;
        this.name = builder.name != null ? builder.name : type().getTypeName() + "@" + hashCode();
        this.priority = builder.priority;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public String toString() {
        return "ServiceDeclaration{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                '}';
    }

    public static abstract class AbstractBuilder<T, SELF extends AbstractBuilder<T, SELF>> {
        protected final Type type;
        protected String name;
        protected int priority;

        protected AbstractBuilder(Type type) {
            this.type = Objects.requireNonNull(type, "type is null");
        }

        public SELF withName(String name) {
            this.name = name;
            return getThis();
        }

        public SELF withPriority(int priority) {
            this.priority = priority;
            return getThis();
        }

        protected abstract SELF getThis();

    }
}
