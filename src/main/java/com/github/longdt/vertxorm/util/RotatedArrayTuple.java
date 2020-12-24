package com.github.longdt.vertxorm.util;

import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.impl.TupleInternal;

public class RotatedArrayTuple implements TupleInternal {
    private Object[] values;
    private int offset;
    private int size;

    public RotatedArrayTuple(Object[] values, int offset) {
        this.values = values;
        this.offset = offset;
        this.size = values.length;
    }

    @Override
    public void setValue(int pos, Object value) {
        values[resolveIndex(pos)] = value;
    }

    private int resolveIndex(int pos) {
        pos += offset;
        if (pos >= size) {
            return pos - size;
        }
        return pos;
    }

    @Override
    public Object getValue(int pos) {
        return values[resolveIndex(pos)];
    }

    @Override
    public Tuple addValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return values.length;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
