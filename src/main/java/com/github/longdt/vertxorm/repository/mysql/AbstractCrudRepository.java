package com.github.longdt.vertxorm.repository.mysql;

import com.github.longdt.vertxorm.repository.*;
import com.github.longdt.vertxorm.repository.base.RowMapperImpl;
import com.github.longdt.vertxorm.repository.query.Query;
import com.github.longdt.vertxorm.util.Tuples;
import io.vertx.core.Future;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.impl.ArrayTuple;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractCrudRepository<ID, E> implements CrudRepository<ID, E> {
    private final Class<ID> idClass;
    protected Pool pool;
    protected RowMapperImpl<ID, E> rowMapper;
    private String deleteSql;
    private String upsertSql;
    private String insertSql;
    private String insertPkSql;
    private String updateSql;
    private String querySql;
    private String countSql;

    @SuppressWarnings("unchecked")
    public AbstractCrudRepository() {
        idClass = (Class<ID>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public void init(Pool pool, RowMapperImpl<ID, E> rowMapper) {
        this.pool = pool;
        this.rowMapper = rowMapper;
        deleteSql = "DELETE FROM `" + rowMapper.tableName() + "` WHERE `" + rowMapper.pkName() + "` = ?";
        upsertSql = "INSERT INTO `" + rowMapper.tableName() + "` "
                + rowMapper.getColumnNames().stream().map(c -> "`" + c + "`").collect(Collectors.joining(",", "(", ")"))
                + " VALUES "
                + rowMapper.getColumnNames().stream().map(c -> "?").collect(Collectors.joining(",", "(", ")"))
                + " ON DUPLICATE KEY UPDATE "
                + rowMapper.getColumnNames(false).stream().map(c -> "`" + c + "` = ?").collect(Collectors.joining(", "));

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
    public Future<E> save(SqlConnection conn, E entity) {
        if (rowMapper.getId(entity) != null) {
            return upsert(conn, entity);
        }
        return insert(conn, entity);
    }

    @Override
    public Future<E> insert(SqlConnection conn, E entity) {
        boolean genPk = rowMapper.isPkAutoGen() && rowMapper.getId(entity) == null;
        var params = rowMapper.toTuple(entity, !genPk);
        String sql = genPk ? insertSql : insertPkSql;
        return conn.preparedQuery(sql)
                .execute(params)
                .map(res -> {
                    try {
                        if (genPk) {
                            var id = cast(res.property(MySQLClient.LAST_INSERTED_ID));
                            rowMapper.setId(entity, id);
                        }
                        return entity;
                    } catch (Exception e) {
                        throw new RuntimeException("Can't set id value of entity: " + entity.getClass().getName(), e);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private ID cast(Long id) {
        Object value;
        if (Long.class.equals(idClass)) {
            value = id;
        } else if (Integer.class.equals(idClass)) {
            value = id.intValue();
        } else if (Short.class.equals(idClass)) {
            value = id.shortValue();
        } else if (Byte.class.equals(idClass)) {
            value = id.byteValue();
        } else {
            value = id;
        }
        return (ID) value;
    }

    @Override
    public Future<E> update(SqlConnection conn, E entity) {
        var params = rowMapper.toTuple(entity);
        return conn.preparedQuery(updateSql)
                .execute(params)
                .map(entity);
    }

    private Future<E> upsert(SqlConnection conn, E entity) {
        var params = rowMapper.toTuple(entity);
        Tuples.addAll(params, rowMapper.toTuple(entity, false));
        return conn.preparedQuery(upsertSql)
                .execute(params)
                .map(entity);
    }

    public Future<Void> delete(SqlConnection conn, ID id) {
        return conn.preparedQuery(deleteSql)
                .execute(Tuple.of(rowMapper.id2DbValue(id)))
                .map(res -> {
                    if (res.rowCount() != 1) {
                        throw new EntityNotFoundException("Entity " + id + " is not found");
                    }
                    return null;
                });
    }

    @Override
    public Future<Optional<E>> find(SqlConnection conn, ID id) {
        String query = querySql + " WHERE `" + rowMapper.pkName() + "`=?";
        return conn.preparedQuery(query)
                .mapping(rowMapper::map)
                .execute(Tuple.of(rowMapper.id2DbValue(id)))
                .map(this::toEntity);
    }

    protected List<E> toList(SqlResult<List<E>> sqlResult) {
        return sqlResult.value();
    }

    protected Optional<E> toEntity(RowSet<E> rowSet) {
        var rowIter = rowSet.iterator();
        E entity = rowIter.hasNext() ? rowIter.next() : null;
        return Optional.ofNullable(entity);
    }

    @Override
    public Future<List<E>> findAll(SqlConnection conn) {
        return conn.query(querySql)
                .collecting(Collectors.mapping(rowMapper::map, Collectors.toList()))
                .execute()
                .map(this::toList);
    }

    @Override
    public Future<List<E>> findAll(SqlConnection conn, Query<E> query) {
        String queryStr = toSQL(querySql, query);
        var params = getSqlParams(query);
        return conn.preparedQuery(queryStr)
                .collecting(Collectors.mapping(rowMapper::map, Collectors.toList()))
                .execute(params)
                .map(this::toList);
    }

    @Override
    public Future<Optional<E>> find(SqlConnection conn, Query<E> query) {
        String queryStr = toSQL(querySql, query);
        var params = getSqlParams(query);
        return conn.preparedQuery(queryStr)
                .mapping(rowMapper::map)
                .execute(params)
                .map(this::toEntity);
    }

    @Override
    public Future<Page<E>> findAll(SqlConnection conn, Query<E> query, PageRequest pageRequest) {
        query.limit(pageRequest.getSize()).offset(pageRequest.getOffset());
        var queryStr = toSQL(querySql, query);
        var params = getSqlParams(query);
        return conn.preparedQuery(queryStr)
                .collecting(Collectors.mapping(rowMapper::map, Collectors.toList()))
                .execute(params)
                .compose(sqlResult -> {
                    var content = sqlResult.value();
                    if (content.size() < pageRequest.getSize()) {
                        return Future.succeededFuture(new Page<>(pageRequest, pageRequest.getOffset() + content.size(), content));
                    }
                    return count(conn, query).map(cnt -> new Page<>(pageRequest, cnt, content));
                });
    }

    @Override
    public Future<Long> count(SqlConnection conn, Query<E> query) {
        return conn.preparedQuery(where(countSql, query))
                .execute(query.getConditionParams())
                .map(res -> res.iterator().next().getLong(0));
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
