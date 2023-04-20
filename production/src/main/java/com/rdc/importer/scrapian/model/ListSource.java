package com.rdc.importer.scrapian.model;

import java.util.List;

import groovy.lang.Closure;

public class ListSource<E extends ScrapianSource> implements ScrapianSource {

    private List<E> value;

    public ListSource(List<E> value) {
        this.value = value;
    }

    public List<E> getValue() {
        return value;
    }

    public String serialize() {
        return value.toString();
    }

    public String toString() {
        try {
            return serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void each(Closure closure) {
        if (value != null) {
            for (E object : value) {
                closure.call(object);
            }
        }
    }

    public E getAt(int i) {
        if (value != null && i < value.size() && i >= 0) {
            return value.get(i);
        }
        return null;
    }

    public int hashCode() {
        String value = toString();
        if (value != null) {
            return value.hashCode();
        }
        return super.hashCode();
    }

    public boolean equals(Object that) {
        if (that instanceof ListSource) {
            String thisToString = this.toString();
            String thatToString = that.toString();
            return thisToString != null && thatToString != null && thisToString.equals(thatToString);
        }
        return false;
    }
}
