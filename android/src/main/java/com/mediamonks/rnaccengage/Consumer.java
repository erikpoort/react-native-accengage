package com.mediamonks.rnaccengage;

/**
 * Functional interface I was missing because of not supporting Java 8
 */
public interface Consumer<T> {
    void accept(T t);
}
