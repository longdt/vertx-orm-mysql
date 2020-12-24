package com.github.longdt.vertxorm.util;

import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.impl.ArrayTuple;

public class Tuples {
    /**
     * add all element from t2 to t1
     * @param t1
     * @param t2
     * @return t1
     */
    public static Tuple addAll(Tuple t1, Tuple t2) {
        for (int i = 0; i < t2.size(); ++i) {
            t1.addValue(t2.getValue(i));
        }
        return t1;
    }

    public static Tuple addAll(Tuple tuple, Object[] values, int offset) {
        for (int i = offset; i < values.length; ++i) {
            tuple.addValue(values[i]);
        }
        return tuple;
    }

    public static Tuple tuple(Tuple src) {
        return new ArrayTuple(src);
    }

    public static Tuple tuple(int capacity) {
        return new ArrayTuple(capacity);
    }

    public static Tuple shift(Object[] data, int offset) {
        return new ShiftedArrayTuple(data, offset);
    }

    public static Tuple rotate(Object[] data, int offset) {
        return new RotatedArrayTuple(data, offset);
    }
}
