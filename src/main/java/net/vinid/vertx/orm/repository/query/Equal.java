package net.vinid.vertx.orm.repository.query;

public class Equal<E> extends SingleQuery<E> {

    public Equal(String fieldName, Object value) {
        super(fieldName, "`" + fieldName + "`=?", value);
    }
}
