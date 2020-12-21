package com.github.longdt.vertxorm.repository;

import com.github.longdt.vertxorm.repository.query.Query;
import com.github.longdt.vertxorm.repository.query.RawQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<ID, E> {
    default void save(E entity, Handler<AsyncResult<E>> resultHandler) {
        save(entity).onComplete(resultHandler);
    }

    default Future<E> save(E entity) {
        return getPool().withConnection(conn -> save(conn, entity));
    }

    default void save(SqlConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        save(conn, entity).onComplete(resultHandler);
    }

    Future<E> save(SqlConnection conn, E entity);

    default void insert(E entity, Handler<AsyncResult<E>> resultHandler) {
        insert(entity).onComplete(resultHandler);
    }

    default Future<E> insert(E entity) {
        return getPool().withConnection(conn -> insert(conn, entity));
    }

    default void insert(SqlConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        insert(conn, entity).onComplete(resultHandler);
    }

    Future<E> insert(SqlConnection conn, E entity);

    default void update(E entity, Handler<AsyncResult<E>> resultHandler) {
        update(entity).onComplete(resultHandler);
    }

    default Future<E> update(E entity) {
        return getPool().withConnection(conn -> update(conn, entity));
    }

    default void update(SqlConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        update(conn, entity).onComplete(resultHandler);
    }

    Future<E> update(SqlConnection conn, E entity);

    default void delete(ID id, Handler<AsyncResult<Void>> resultHandler) {
        delete(id).onComplete(resultHandler);
    }

    default Future<Void> delete(ID id) {
        return getPool().withConnection(conn -> delete(conn, id));
    }

    default void delete(SqlConnection conn, ID id, Handler<AsyncResult<Void>> resultHandler) {
        delete(conn, id).onComplete(resultHandler);
    }

    Future<Void> delete(SqlConnection conn, ID id);

    default void find(ID id, Handler<AsyncResult<Optional<E>>> resultHandler) {
        find(id).onComplete(resultHandler);
    }

    default Future<Optional<E>> find(ID id) {
        return getPool().withConnection(conn -> find(conn, id));
    }

    default void find(SqlConnection conn, ID id, Handler<AsyncResult<Optional<E>>> resultHandler) {
        find(conn, id).onComplete(resultHandler);
    }

    Future<Optional<E>> find(SqlConnection conn, ID id);

    default void findAll(Handler<AsyncResult<List<E>>> resultHandler) {
        findAll().onComplete(resultHandler);
    }

    default Future<List<E>> findAll() {
        return getPool().withConnection(this::findAll);
    }

    default void findAll(SqlConnection conn, Handler<AsyncResult<List<E>>> resultHandler) {
        findAll(conn).onComplete(resultHandler);
    }

    Future<List<E>> findAll(SqlConnection conn);

    default void findAll(Query<E> query, Handler<AsyncResult<List<E>>> resultHandler) {
        findAll(query).onComplete(resultHandler);
    }

    default Future<List<E>> findAll(Query<E> query) {
        return getPool().withConnection(conn -> findAll(conn, query));
    }

    default void findAll(SqlConnection conn, Query<E> query, Handler<AsyncResult<List<E>>> resultHandler) {
        findAll(conn, query).onComplete(resultHandler);
    }

    Future<List<E>> findAll(SqlConnection conn, Query<E> query);

    default void find(Query<E> query, Handler<AsyncResult<Optional<E>>> resultHandler) {
        find(query).onComplete(resultHandler);
    }

    default Future<Optional<E>> find(Query<E> query) {
        return getPool().withConnection(conn -> find(conn, query));
    }

    default void find(SqlConnection conn, Query<E> query, Handler<AsyncResult<Optional<E>>> resultHandler) {
        find(conn, query).onComplete(resultHandler);
    }

    Future<Optional<E>> find(SqlConnection conn, Query<E> query);

    default void findAll(PageRequest pageRequest, Handler<AsyncResult<Page<E>>> resultHandler) {
        findAll(new RawQuery<>(null), pageRequest).onComplete(resultHandler);
    }

    default Future<Page<E>> findAll(PageRequest pageRequest) {
        return findAll(new RawQuery<>(null), pageRequest);
    }

    default void findAll(Query<E> query, PageRequest pageRequest, Handler<AsyncResult<Page<E>>> resultHandler) {
        findAll(query, pageRequest).onComplete(resultHandler);
    }

    default Future<Page<E>> findAll(Query<E> query, PageRequest pageRequest) {
        return getPool().withTransaction(conn -> findAll(conn, query, pageRequest));
    }

    default void findAll(SqlConnection conn, PageRequest pageRequest, Handler<AsyncResult<Page<E>>> resultHandler) {
        findAll(conn, new RawQuery<>(null), pageRequest).onComplete(resultHandler);
    }

    default Future<Page<E>> findAll(SqlConnection conn, PageRequest pageRequest) {
        return findAll(conn, new RawQuery<>(null), pageRequest);
    }

    default void findAll(SqlConnection conn, Query<E> query, PageRequest pageRequest, Handler<AsyncResult<Page<E>>> resultHandler) {
        findAll(conn, query, pageRequest).onComplete(resultHandler);
    }

    Future<Page<E>> findAll(SqlConnection conn, Query<E> query, PageRequest pageRequest);

    default void count(Query<E> query, Handler<AsyncResult<Long>> resultHandler) {
        count(query).onComplete(resultHandler);
    }

    default Future<Long> count(Query<E> query) {
        return getPool().withConnection(conn -> count(conn, query));
    }

    default void count(SqlConnection conn, Query<E> query, Handler<AsyncResult<Long>> resultHandler) {
        count(conn, query).onComplete(resultHandler);
    }

    Future<Long> count(SqlConnection conn, Query<E> query);

    Pool getPool();
}
