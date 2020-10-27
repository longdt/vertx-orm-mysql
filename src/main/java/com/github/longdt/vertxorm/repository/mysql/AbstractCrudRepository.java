package com.github.longdt.vertxorm.repository.mysql;

import com.github.longdt.vertxorm.repository.*;
import com.github.longdt.vertxorm.repository.base.RowMapperImpl;
import com.github.longdt.vertxorm.repository.query.Query;
import com.github.longdt.vertxorm.util.Tuples;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.impl.ArrayTuple;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractCrudRepository<ID, E> implements CrudRepository<ID, E> {
    protected Pool pool;
    protected RowMapperImpl<ID, E> rowMapper;
    private String deleteSql;
    private String upsertSql;
    private String insertSql;
    private String insertPkSql;
    private String updateSql;
    private String querySql;
    private String countSql;

    public void init(Pool pool, RowMapperImpl<ID, E> rowMapper) {
        this.pool = pool;
        this.rowMapper = rowMapper;
        deleteSql = "DELETE FROM `" + rowMapper.tableName() + "` WHERE `" + rowMapper.pkName() + "` = ?";
        upsertSql = "INSERT INTO `" + rowMapper.tableName() + "` "
                + rowMapper.getColumnNames().stream().map(c -> "`" + c + "`").collect(Collectors.joining(",", "(", ")"))
                + " VALUES "
                + rowMapper.getColumnNames().stream().map(c -> "?").collect(Collectors.joining(",", "(", ")"))
                + " ON DUPLICATE KEY UPDATE "
                + rowMapper.getColumnNames(false).stream().map(c -> "`" + c + "` = `" + c + "`").collect(Collectors.joining(", "));

        insertSql = "INSERT INTO `" + rowMapper.tableName() + "` "
                + rowMapper.getColumnNames(false).stream().map(c -> "`" + c + "`").collect(Collectors.joining(",", "(", ")"))
                + " VALUES "
                + rowMapper.getColumnNames(false).stream().map(c -> "?").collect(Collectors.joining(",", "(", ")"));

        insertPkSql = "INSERT INTO `" + rowMapper.tableName() + "` "
                + rowMapper.getColumnNames().stream().map(c -> "`" + c + "`").collect(Collectors.joining(",", "(", ")"))
                + " VALUES "
                + rowMapper.getColumnNames().stream().map(c -> "?").collect(Collectors.joining(",", "(", ")"));

        updateSql = "UPDATE `" + rowMapper.tableName() + "`"
                + " SET " + rowMapper.getColumnNames(false).stream().map(c -> "`" + c + "` = ?").collect(Collectors.joining(","))
                + " WHERE `" + rowMapper.pkName() + "` = ?";
        querySql = "SELECT " + rowMapper.getColumnNames().stream().map(c -> "`" + c + "`").collect(Collectors.joining(","))
                + " FROM `" + rowMapper.tableName() + "`";
        countSql = "SELECT count(*) FROM `" + rowMapper.tableName() + "`";
    }

    @Override
    public void save(SqlConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        if (rowMapper.getId(entity) != null) {
            upsert(conn, entity, resultHandler);
        } else {
            insert(conn, entity, resultHandler);
        }
    }

    @Override
    public void insert(SqlConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        boolean genPk = rowMapper.isPkAutoGen() && rowMapper.getId(entity) == null;
        var params = rowMapper.toTuple(entity, !genPk);
        String sql = genPk ? insertSql : insertPkSql;
        conn.preparedQuery(sql)
                .execute(params, res -> {
                    if (res.succeeded()) {
                        try {
                            if (genPk) {
                                rowMapper.setId(entity, res.result().property(MySQLClient.LAST_INSERTED_ID));
                            }
                            resultHandler.handle(Future.succeededFuture(entity));
                        } catch (Exception e) {
                            resultHandler.handle(Future.failedFuture(new RuntimeException("Can't set id value of entity: " + entity.getClass().getName(), e)));
                        }
                    } else {
                        resultHandler.handle(Future.failedFuture(res.cause()));
                    }
                });
    }

    @Override
    public void update(SqlConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        var params = rowMapper.toTuple(entity);
        conn.preparedQuery(updateSql)
                .execute(params, res -> {
                    if (res.succeeded()) {
                        resultHandler.handle(Future.succeededFuture(entity));
                    } else {
                        resultHandler.handle(Future.failedFuture(res.cause()));
                    }
                });
    }

    private void upsert(SqlConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        var params = rowMapper.toTuple(entity);
        conn.preparedQuery(upsertSql)
                .execute(params, res -> {
                    if (res.succeeded()) {
                        resultHandler.handle(Future.succeededFuture(entity));
                    } else {
                        resultHandler.handle(Future.failedFuture(res.cause()));
                    }
                });
    }

    public void delete(SqlConnection conn, ID id, Handler<AsyncResult<Void>> resultHandler) {
        conn.preparedQuery(deleteSql)
                .execute(Tuple.of(rowMapper.id2DbValue(id)), res -> {
                    if (res.succeeded()) {
                        if (res.result().rowCount() != 1) {
                            resultHandler.handle(Future.failedFuture(new EntityNotFoundException("Entity " + id + " is not found")));
                            return;
                        }
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(res.cause()));
                    }
                });
    }

    @Override
    public void find(SqlConnection conn, ID id, Handler<AsyncResult<Optional<E>>> resultHandler) {
        String query = querySql + " WHERE `" + rowMapper.pkName() + "`=?";
        conn.preparedQuery(query)
                .mapping(rowMapper::map)
                .execute(Tuple.of(rowMapper.id2DbValue(id)), toEntity(resultHandler));
    }

    protected Handler<AsyncResult<SqlResult<List<E>>>> toList(Handler<AsyncResult<List<E>>> resultHandler) {
        return res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result().value()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        };
    }

    protected Handler<AsyncResult<RowSet<E>>> toEntity(Handler<AsyncResult<Optional<E>>> resultHandler) {
        return res -> {
            if (res.succeeded()) {
                var rowIter = res.result().iterator();
                E entity = rowIter.hasNext() ? rowIter.next() : null;
                resultHandler.handle(Future.succeededFuture(Optional.ofNullable(entity)));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        };
    }

    @Override
    public void findAll(SqlConnection conn, Handler<AsyncResult<List<E>>> resultHandler) {
        conn.query(querySql)
                .collecting(Collectors.mapping(rowMapper::map, Collectors.toList()))
                .execute(toList(resultHandler));
    }

    @Override
    public void findAll(SqlConnection conn, Query<E> query, Handler<AsyncResult<List<E>>> resultHandler) {
        String queryStr = toSQL(querySql, query);
        var params = getSqlParams(query);
        conn.preparedQuery(queryStr)
                .collecting(Collectors.mapping(rowMapper::map, Collectors.toList()))
                .execute(params, toList(resultHandler));
    }

    @Override
    public void find(SqlConnection conn, Query<E> query, Handler<AsyncResult<Optional<E>>> resultHandler) {
        String queryStr = toSQL(querySql, query);
        var params = getSqlParams(query);
        conn.preparedQuery(queryStr)
                .mapping(rowMapper::map)
                .execute(params, toEntity(resultHandler));
    }

    @Override
    public void findAll(SqlConnection conn, Query<E> query, PageRequest pageRequest, Handler<AsyncResult<Page<E>>> resultHandler) {
        count(conn, query)
                .compose(cnt -> {
                    if (cnt == 0) {
                        return Future.succeededFuture(new Page<>(pageRequest, cnt, Collections.<E>emptyList()));
                    }
                    Promise<SqlResult<List<E>>> pageResult = Promise.promise();
                    query.limit(pageRequest.getSize()).offset(pageRequest.getOffset());
                    var queryStr = toSQL(querySql, query);
                    var params = getSqlParams(query);
                    conn.preparedQuery(queryStr)
                            .collecting(Collectors.mapping(rowMapper::map, Collectors.toList()))
                            .execute(params, pageResult);
                    return pageResult.future().map(sqlResult -> new Page<>(pageRequest, cnt, sqlResult.value()));
                })
                .onComplete(resultHandler);
    }

    @Override
    public void count(SqlConnection conn, Query<E> query, Handler<AsyncResult<Long>> resultHandler) {
        conn.preparedQuery(where(countSql, query))
                .execute(query.getConditionParams(), res -> {
                    if (res.failed()) {
                        resultHandler.handle(Future.failedFuture(res.cause()));
                    } else {
                        resultHandler.handle(Future.succeededFuture(res.result().iterator().next().getLong(0)));
                    }
                });
    }

    protected String where(String sql, Query<E> query) {
        String condition = query.getConditionSql();
        if (condition != null) {
            sql = sql + " WHERE " + query.getConditionSql();
        }
        return sql;
    }

    protected String toSQL(String sql, Query<E> query) {
        StringBuilder queryStr = new StringBuilder(sql);
        String condition = query.getConditionSql();
        if (condition != null) {
            queryStr.append(" WHERE ").append(condition);
        }
        if (query.orderBy() != null && !query.orderBy().isEmpty()) {
            queryStr.append(" ORDER BY ");
            query.orderBy().forEach(o -> queryStr.append("`").append(o.getFieldName()).append("` ")
                    .append(o.isDescending() ? "DESC," : "ASC,"));
            queryStr.deleteCharAt(queryStr.length() - 1);
        }

        if (query.limit() >= 0) {
            queryStr.append(" LIMIT ?");
        }
        if (query.offset() >= 0) {
            queryStr.append(" OFFSET ?");
        }
        return queryStr.toString();
    }


    protected Tuple getSqlParams(Query<E> query) {
        if (query.limit() < 0 && query.offset() < 0) {
            return query.getConditionParams();
        }

        var params = query.getConditionParams();
        params = Tuples.addAll(new ArrayTuple(params.size() + 2), params);
        if (query.limit() >= 0) {
            params.addInteger(query.limit());
        }
        if (query.offset() >= 0) {
            params.addLong(query.offset());
        }
        return params;
    }


    public RowMapper<ID, E> getRowMapper() {
        return rowMapper;
    }

    @Override
    public Pool getPool() {
        return pool;
    }
}
