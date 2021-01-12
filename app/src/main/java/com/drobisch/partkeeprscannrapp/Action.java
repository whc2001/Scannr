package com.drobisch.partkeeprscannrapp;

public interface Action<T> {
    public void run(T obj);
}
