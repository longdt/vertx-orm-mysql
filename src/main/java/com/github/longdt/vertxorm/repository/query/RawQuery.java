package com.github.longdt.vertxorm.repository.query;

import io.vertx.sqlclient.Tuple;

import java.util.regex.Pattern;

public class RawQuery<E> extends AbstractQuery<E> {
    protected String querySql;

    public RawQuery(String querySql) {
        this(querySql, QueryFactory.EMPTY_PARAMS);
    }

    public RawQuery(String querySql, Object... params) {
        this(querySql, Tuple.wrap(params));
    }

    public RawQuery(String querySql, Tuple params) {
        super(params);
        this.querySql = querySql;
    }

    @Override
    public int appendQuerySql(StringBuilder sqlBuilder, int index) {
	sqlBuilder.append(querySql);
        return index + params.size();
    }

    @Override
    public boolean isConditional() {
        return querySql != null;
    }
}
