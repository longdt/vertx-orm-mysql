package net.vinid.vertx.orm.repository.query;

import io.vertx.core.json.JsonArray;

public class RawQuery<E> extends AbstractQuery<E> {
    protected String querySql;
    protected JsonArray params;

    public RawQuery(String querySql) {
        this(querySql, QueryFactory.EMPTY_PARAMS);
    }

    public RawQuery(String querySql, JsonArray params) {
        this.querySql = querySql;
        this.params = params;
    }

    @Override
    public String getConditionSql() {
        return querySql;
    }

    @Override
    public JsonArray getConditionParams() {
        return params;
    }
}
