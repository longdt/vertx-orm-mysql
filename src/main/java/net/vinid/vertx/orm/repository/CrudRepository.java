package net.vinid.vertx.orm.repository;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import net.vinid.vertx.orm.repository.query.Query;
import net.vinid.vertx.orm.repository.query.RawQuery;
import net.vinid.vertx.orm.util.Futures;
import net.vinid.vertx.orm.util.SQLHelper;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public interface CrudRepository<ID, E> {
    default void save(E entity, Handler<AsyncResult<E>> resultHandler) {
        SQLHelper.executeInConnection(getSQLClient(), this::save, entity, resultHandler);
    }

    default Future<E> save(E entity) {
        return Futures.toFuture(this::save, entity);
    }

    void save(SQLConnection conn, E entity, Handler<AsyncResult<E>> resultHandler);

    default Future<E> save(SQLConnection conn, E entity) {
        return Futures.toFuture(this::save, conn, entity);
    }

    default void insert(E entity, Handler<AsyncResult<E>> resultHandler) {
        SQLHelper.executeInConnection(getSQLClient(), this::insert, entity, resultHandler);
    }

    default Future<E> insert(E entity) {
        return Futures.toFuture(this::insert, entity);
    }

    void insert(SQLConnection conn, E entity, Handler<AsyncResult<E>> resultHandler);

    default Future<E> insert(SQLConnection conn, E entity) {
        return Futures.toFuture(this::insert, conn, entity);
    }

    default void update(E entity, Handler<AsyncResult<E>> resultHandler) {
        SQLHelper.executeInConnection(getSQLClient(), this::update, entity, resultHandler);
    }

    default Future<E> update(E entity) {
        return Futures.toFuture(this::update, entity);
    }

    void update(SQLConnection conn, E entity, Handler<AsyncResult<E>> resultHandler);

    default Future<E> update(SQLConnection conn, E entity) {
        return Futures.toFuture(this::update, conn, entity);
    }

    default void delete(ID id, Handler<AsyncResult<Void>> resultHandler) {
        SQLHelper.executeInConnection(getSQLClient(), this::delete, id, resultHandler);
    }

    default Future<Void> delete(ID id) {
        return Futures.toFuture(this::delete, id);
    }

    void delete(SQLConnection conn, ID id, Handler<AsyncResult<Void>> resultHandler);

    default Future<Void> delete(SQLConnection conn, ID id) {
        return Futures.toFuture(this::delete, conn, id);
    }

    default void find(ID id, Handler<AsyncResult<Optional<E>>> resultHandler) {
        SQLHelper.executeInConnection(getSQLClient(), this::find, id, resultHandler);
    }

    default Future<Optional<E>> find(ID id) {
        return Futures.toFuture(this::find, id);
    }

    void find(SQLConnection conn, ID id, Handler<AsyncResult<Optional<E>>> resultHandler);

    default Future<Optional<E>> find(SQLConnection conn, ID id) {
        return Futures.toFuture(this::find, conn, id);
    }

    default void findAll(Handler<AsyncResult<List<E>>> resultHandler) {
        SQLHelper.executeInConnection(getSQLClient(), (BiConsumer<SQLConnection, Handler<AsyncResult<List<E>>>>) this::findAll, resultHandler);
    }

    default Future<List<E>> findAll() {
        return Futures.toFuture(this::findAll);
    }

    void findAll(SQLConnection conn, Handler<AsyncResult<List<E>>> resultHandler);

    default Future<List<E>> findAll(SQLConnection conn) {
        return Futures.toFuture(this::findAll, conn);
    }

    default void findAll(Query<E> query, Handler<AsyncResult<List<E>>> resultHandler) {
        SQLHelper.executeInConnection(getSQLClient(), this::findAll, query, resultHandler);
    }

    default Future<List<E>> findAll(Query<E> query) {
        return Futures.toFuture(this::findAll, query);
    }

    void findAll(SQLConnection conn, Query<E> query, Handler<AsyncResult<List<E>>> resultHandler);

    default Future<List<E>> findAll(SQLConnection conn, Query<E> query) {
        return Futures.toFuture(this::findAll, conn, query);
    }

    default void find(Query<E> query, Handler<AsyncResult<Optional<E>>> resultHandler) {
        SQLHelper.executeInConnection(getSQLClient(), this::find, query, resultHandler);
    }

    default Future<Optional<E>> find(Query<E> query) {
        return Futures.toFuture(this::find, query);
    }

    void find(SQLConnection conn, Query<E> query, Handler<AsyncResult<Optional<E>>> resultHandler);

    default Future<Optional<E>> find(SQLConnection conn, Query<E> query) {
        return Futures.toFuture(this::find, conn, query);
    }

    default void findAll(PageRequest pageRequest, Handler<AsyncResult<Page<E>>> resultHandler) {
        findAll(new RawQuery<>(null), pageRequest, resultHandler);
    }

    default Future<Page<E>> findAll(PageRequest pageRequest) {
        return Futures.toFuture(this::findAll, pageRequest);
    }

    default void findAll(Query<E> query, PageRequest pageRequest, Handler<AsyncResult<Page<E>>> resultHandler) {
        SQLHelper.inTransactionSingle(getSQLClient(), conn -> findAll(conn, query, pageRequest), resultHandler);
    }

    default Future<Page<E>> findAll(Query<E> query, PageRequest pageRequest) {
        return Futures.toFuture(this::findAll, query, pageRequest);
    }

    default void findAll(SQLConnection conn, PageRequest pageRequest, Handler<AsyncResult<Page<E>>> resultHandler) {
        findAll(conn, new RawQuery<>(null), pageRequest, resultHandler);
    }

    default Future<Page<E>> findAll(SQLConnection conn, PageRequest pageRequest) {
        return Futures.toFuture(this::findAll, conn, pageRequest);
    }

    void findAll(SQLConnection conn, Query<E> query, PageRequest pageRequest, Handler<AsyncResult<Page<E>>> resultHandler);

    default Future<Page<E>> findAll(SQLConnection conn, Query<E> query, PageRequest pageRequest) {
        return Futures.toFuture(this::findAll, conn, query, pageRequest);
    }

    default void count(Query<E> query, Handler<AsyncResult<Long>> resultHandler) {
        SQLHelper.executeInConnection(getSQLClient(), this::count, query, resultHandler);
    }

    default Future<Long> count(Query<E> query) {
        return Futures.toFuture(this::count, query);
    }

    void count(SQLConnection conn, Query<E> query, Handler<AsyncResult<Long>> resultHandler);

    default Future<Long> count(SQLConnection conn, Query<E> query) {
        return Futures.toFuture(this::count, conn, query);
    }

    SQLClient getSQLClient();
}
