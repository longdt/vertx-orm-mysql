package net.vinid.vertx.orm.repository.impl;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import net.vinid.vertx.orm.repository.CrudRepository;
import net.vinid.vertx.orm.repository.EntityNotFoundException;
import net.vinid.vertx.orm.repository.Page;
import net.vinid.vertx.orm.repository.PageRequest;
import net.vinid.vertx.orm.repository.query.Query;
import net.vinid.vertx.orm.util.Futures;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractCrudRepository<ID, E> implements CrudRepository<ID, E> {
    protected SQLClient sqlClient;
    protected RowMapperImpl<ID, E> rowMapper;
    private String deleteSql;
    private String upsertSql;
    private String insertSql;
    private String insertPkSql;
    private String updateSql;
    private String querySql;
    private String countSql;

    public void init(SQLClient sqlClient, RowMapperImpl<ID, E> rowMapper) {
        this.sqlClient = sqlClient;
        this.rowMapper = rowMapper;
        deleteSql = "DELETE FROM `" + rowMapper.tableName() + "` WHERE `" + rowMapper.pkName() + "` = ?";
        upsertSql = "INSERT INTO `" + rowMapper.tableName() + "` "
                + rowMapper.getColumnNames().stream().map(c -> "`" + c + "`").collect(Collectors.joining(",", "(", ")"))
                + " VALUES "
                + rowMapper.getColumnNames().stream().map(c -> "?").collect(Collectors.joining(",", "(", ")"))
                + " ON CONFLICT (`" + rowMapper.pkName() + "`) DO UPDATE SET "
                + rowMapper.getColumnNames(false).stream().map(c -> "`" + c + "` = EXCLUDED.`" + c + "`").collect(Collectors.joining(", "));
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
    public void save(SQLConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        if (rowMapper.getId(entity) != null) {
            upsert(conn, entity, resultHandler);
        } else {
            insert(conn, entity, resultHandler);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insert(SQLConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        boolean genPk = rowMapper.isPkAutoGen() && rowMapper.getId(entity) == null;
        JsonArray params = rowMapper.toJsonArray(entity, !genPk);
        String sql = genPk ? insertSql : insertPkSql;
        if (genPk) {
            Futures.toFuture(conn::updateWithParams, insertSql, params)
                    .map(res -> {
                        rowMapper.setId(entity, res.getKeys().getValue(0));
                        return entity;
                    })
                    .setHandler(resultHandler);
            return;
        }

        conn.querySingleWithParams(sql, params, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(entity));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    @Override
    public void update(SQLConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        JsonArray params = rowMapper.toJsonArray(entity);
        conn.querySingleWithParams(updateSql, params, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(entity));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    private void upsert(SQLConnection conn, E entity, Handler<AsyncResult<E>> resultHandler) {
        JsonArray params = rowMapper.toJsonArray(entity);
        conn.updateWithParams(upsertSql, params, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(entity));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void delete(SQLConnection conn, ID id, Handler<AsyncResult<Void>> resultHandler) {
        conn.updateWithParams(deleteSql, new JsonArray().add(rowMapper.id2DbValue(id)), res -> {
            if (res.succeeded()) {
                if(res.result().getUpdated() != 1) {
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
    public void find(SQLConnection conn, ID id, Handler<AsyncResult<Optional<E>>> resultHandler) {
        String query = querySql + " WHERE `" + rowMapper.pkName() + "`=?";
        conn.querySingleWithParams(query, new JsonArray().add(rowMapper.id2DbValue(id)), toEntity(resultHandler));
    }

    protected Handler<AsyncResult<ResultSet>> toList(Handler<AsyncResult<List<E>>> resultHandler) {
        return res -> {
            if (res.succeeded()) {
                try {
                    List<E> entities = res.result().getResults().stream()
                            .map(rowMapper::map).collect(Collectors.toList());
                    resultHandler.handle(Future.succeededFuture(entities));
                } catch (Exception e) {
                    resultHandler.handle(Future.failedFuture(e));
                }
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        };
    }

    protected Handler<AsyncResult<JsonArray>> toEntity(Handler<AsyncResult<Optional<E>>> resultHandler) {
        return res -> {
            if (res.succeeded()) {
                try {
                    E entity = res.result() != null ? rowMapper.map(res.result()) : null;
                    resultHandler.handle(Future.succeededFuture(Optional.ofNullable(entity)));
                } catch (Exception e) {
                    resultHandler.handle(Future.failedFuture(e));
                }
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        };
    }

    @Override
    public void findAll(SQLConnection conn, Handler<AsyncResult<List<E>>> resultHandler) {
        conn.query(querySql, toList(resultHandler));
    }

    @Override
    public void findAll(SQLConnection conn, Query<E> query, Handler<AsyncResult<List<E>>> resultHandler) {
        String queryStr = toSQL(querySql, query);
        JsonArray params = getSqlParams(query);
        conn.queryWithParams(queryStr, params, toList(resultHandler));
    }

    @Override
    public void find(SQLConnection conn, Query<E> query, Handler<AsyncResult<Optional<E>>> resultHandler) {
        String queryStr = toSQL(querySql, query);
        JsonArray params = getSqlParams(query);
        conn.querySingleWithParams(queryStr, params, toEntity(resultHandler));
    }


    @Override
    public void findAll(SQLConnection conn, Query<E> query, PageRequest pageRequest, Handler<AsyncResult<Page<E>>> resultHandler) {
        count(conn, query).compose(cnt -> {
            Promise<ResultSet> pageResult = Promise.promise();
            query.limit(pageRequest.getSize()).offset(pageRequest.getOffset());
            String queryStr = toSQL(querySql, query);
            JsonArray params = getSqlParams(query);
            conn.queryWithParams(queryStr, params, pageResult);
            Future<List<E>> entities = pageResult.future().map(rs -> rs.getResults().stream()
                    .map(rowMapper::map).collect(Collectors.toList()));
            return entities.map(es -> new Page<E>(pageRequest, cnt, es));
        })
        .setHandler(resultHandler);
    }

    @Override
    public void count(SQLConnection conn, Query<E> query, Handler<AsyncResult<Long>> resultHandler) {
        conn.querySingleWithParams(where(countSql, query), query.getConditionParams(), res -> {
            if (res.failed()) {
                resultHandler.handle(Future.failedFuture(res.cause()));
            } else {
                resultHandler.handle(Future.succeededFuture(res.result().getLong(0)));
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


    protected JsonArray getSqlParams(Query<E> query) {
        if (query.limit() < 0 && query.offset() < 0) {
            return query.getConditionParams();
        }
        JsonArray params = new JsonArray().addAll(query.getConditionParams());
        if (query.limit() >= 0) {
            params.add(query.limit());
        }
        if (query.offset() >= 0) {
            params.add(query.offset());
        }
        return params;
    }


    public RowMapper<ID, E> getRowMapper() {
        return rowMapper;
    }

    @Override
    public SQLClient getSQLClient() {
        return sqlClient;
    }
}
