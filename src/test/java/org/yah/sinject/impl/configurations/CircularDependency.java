package org.yah.sinject.impl.configurations;

import org.yah.sinject.annotations.Service;

public class CircularDependency {

    @Service
    public MockService serviceA(MockService serviceC) {
        return new MockService();
    }

    @Service
    public MockService serviceB(MockService serviceA) {
        return new MockService();
    }

    @Service
    public MockService serviceC(MockService serviceB) {
        return new MockService();
    }

    @Service(priority = -1)
    public MockService serviceD(MockService serviceC) {
        return new MockService();
    }

    public static class MockService {
    }
}
