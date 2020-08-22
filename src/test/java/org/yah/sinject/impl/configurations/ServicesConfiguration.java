package org.yah.sinject.impl.configurations;


import org.yah.sinject.annotations.Service;

import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("ALL")
public class ServicesConfiguration {

    @Service
    public Supplier<String> stringSupplier() {
        return () -> "I'am here !";
    }

    @Service
    public Supplier<Integer> intSupplier() {
        return () -> 42;
    }

    @Service(priority = Service.TOP_PRIORITY)
    public ServiceA serviceA() {
        return new ServiceA();
    }

    @Service
    public ServiceA secondaryServiceA() {
        return new ServiceA();
    }

    @Service
    public ServiceB serviceB(String theString) {
        return new ServiceB(theString);
    }

    @Service
    public ServiceC serviceC(ServiceB serviceB,
                             Optional<Supplier<String>> stringSupplier,
                             Optional<Supplier<Double>> doubleSupplier) {
        return new ServiceC(serviceB, stringSupplier.orElse(null), doubleSupplier.orElse(null));
    }

    @Service
    public ServiceD serviceD(ServiceC serviceC) {
        return new ServiceD(serviceC);
    }

    @Service
    public ServiceE serviceE(ServiceC serviceC, ServiceD serviceD) {
        return new ServiceE(serviceC, serviceD);
    }

    @Service
    public Class<? extends ServiceF> serviceF() {
        return DefaultServiceF.class;
    }

    @Service
    public Optional<Class<ServiceA>> emptyServiceA() {
        return Optional.empty();
    }

    @Service
    public Optional<Class<ServiceA>> presentServiceA() {
        return Optional.of(ServiceA.class);
    }

    @Service(priority = 1)
    public Optional<ServiceB> emptyInstance(String theString) {
        return Optional.empty();
    }

    @Service(priority = 1)
    public Optional<ServiceB> presentInstance(String theString) {
        return Optional.of(new ServiceB(theString));
    }

    public interface ServiceF {
        ServiceA getServiceA();

        ServiceE getServiceE();
    }

    public static class ServiceA {
    }

    public static class ServiceB {
        public final String theString;

        public ServiceB(String theString) {
            this.theString = theString;
        }
    }

    public static class ServiceC {
        public final ServiceB serviceB;
        public final Supplier<String> stringSupplier;
        public final Supplier<Double> doubleSupplier;

        public ServiceC(ServiceB serviceB, Supplier<String> stringSupplier, Supplier<Double> doubleSupplier) {
            this.serviceB = serviceB;
            this.stringSupplier = stringSupplier;
            this.doubleSupplier = doubleSupplier;
        }
    }

    public static class ServiceD {
        public final ServiceC serviceC;

        public ServiceD(ServiceC serviceC) {
            this.serviceC = serviceC;
        }
    }

    public static class ServiceE {
        public final ServiceC serviceC;
        public final ServiceD serviceD;

        public ServiceE(ServiceC serviceC, ServiceD serviceD) {
            this.serviceC = serviceC;
            this.serviceD = serviceD;
        }
    }

    public static class DefaultServiceF implements ServiceF {
        public final ServiceE serviceE;
        public final ServiceA serviceA;

        public DefaultServiceF(ServiceE serviceE, ServiceA serviceA) {
            this.serviceE = serviceE;
            this.serviceA = serviceA;
        }

        @Override
        public ServiceA getServiceA() {
            return serviceA;
        }

        @Override
        public ServiceE getServiceE() {
            return serviceE;
        }

    }

}
