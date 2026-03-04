package com.indianmusictiles.game.engine;

import java.util.ArrayList;
import java.util.List;

public class ObjectPool<T> {
    public interface Factory<T> {
        T create();
    }

    public interface Resetter<T> {
        void reset(T obj);
    }

    private final List<T> pool;
    private final Factory<T> factory;
    private final Resetter<T> resetter;
    private final int maxSize;

    public ObjectPool(Factory<T> factory, Resetter<T> resetter, int initialSize, int maxSize) {
        this.factory = factory;
        this.resetter = resetter;
        this.maxSize = maxSize;
        this.pool = new ArrayList<>(initialSize);

        for (int i = 0; i < initialSize; i++) {
            pool.add(factory.create());
        }
    }

    public T obtain() {
        if (pool.isEmpty()) {
            return factory.create();
        }
        return pool.remove(pool.size() - 1);
    }

    public void recycle(T obj) {
        if (pool.size() < maxSize) {
            resetter.reset(obj);
            pool.add(obj);
        }
    }

    public int getPoolSize() {
        return pool.size();
    }
}
