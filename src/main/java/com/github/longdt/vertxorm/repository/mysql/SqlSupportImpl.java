package com.github.longdt.vertxorm.repository.mysql;

import com.github.longdt.vertxorm.repository.SqlSupport;
import com.github.longdt.vertxorm.repository.query.Query;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SqlSupportImpl implements SqlSupport {
    private final String tableName;
    private final List<String> columnNames;
    private final String insertSql;
    private final String autoIdInsertSql;
    private final String upsertSql;
    private final String updateSql;
    private final String querySql;
    private final String queryByIdSql;
    private final String countSql;
    private final String existSql;
    private final String existByIdSql;
    private final String deleteSql;

    public SqlSupportImpl(String tableName, List<String> columnNames) {
        this.tableName = Objects.requireNonNull(tableName);
        this.columnNames = Objects.requireNonNull(columnNames);
        insertSql = "INSERT INTO `" + tableName + "` "
                + columnNames.stream().map(c -> '`' + c + '`').collect(Collectors.joining(",", "(", ")"))
                + " VALUES "
                + IntStream.rangeClosed(1, columnNames.size()).mapToObj(idx -> "?").collect(Collectors.joining(",", "(", ")"));
        autoIdInsertSql = "INSERT INTO `" + tableName + "` "
                + columnNames.stream().skip(1).map(c -> '`' + c + '`').collect(Collectors.joining(",", "(", ")"))
                + " VALUES "
                + IntStream.rangeClosed(1, columnNames.size() - 1).mapToObj(idx -> "?").collect(Collectors.joining(",", "(", ")"));
        upsertSql = insertSql
                + " ON DUPLICATE KEY UPDATE "
                + columnNames.stream().skip(1).map(c -> '`' + c + "` = ?").collect(Collectors.joining(", "));
        updateSql = "UPDATE `" + tableName + "` SET "
                + columnNames.stream().skip(1).map(c -> '`' + c + "` = ?").collect(Collectors.joining(","))
                + " WHERE `" + getIdName() + "` = ?";
        querySql = "SELECT " + columnNames.stream().map(c -> '`' + c + '`').collect(Collectors.joining(","))
                + " FROM `" + tableName + '`';
        queryByIdSql = querySql + " WHERE `" + getIdName() + "` = ?";
        countSql = "SELECT count(*) FROM `" + tableName + "`";
        existSql = "SELECT 1 FROM `" + tableName + "`";
        existByIdSql = existSql + " WHERE `" + getIdName() + "` = ? LIMIT 1";
        deleteSql = "DELETE FROM `" + tableName + "` WHERE `" + getIdName() + "` = ?";
    }

    public String getTableName() {
        return tableName;
    }

    private String getIdName() {
        return columnNames.get(0);
    }

    @Override
    public String getInsertSql() {
        return insertSql;
    }

    @Override
    public String getAutoIdInsertSql() {
        return autoIdInsertSql;
    }

    @Override
    public String getUpsertSql() {
        return upsertSql;
    }

    @Override
    public String getUpdateSql() {
        return updateSql;
    }

    @Override
    public String getQuerySql() {
        return querySql;
    }

    @Override
    public String getQueryByIdSql() {
        return queryByIdSql;
    }

    @Override
    public <E> String getSql(String sql, Query<E> query) {
        StringBuilder sqlBuilder = new StringBuilder(sql);
        if (query.isConditional()) {
            sqlBuilder.append(" WHERE ");
            query.appendQuerySql(sqlBuilder, 0);
        }
        if (query.orderBy() != null && !query.orderBy().isEmpty()) {
            sqlBuilder.append(" ORDER BY ");
            query.orderBy().forEach(o -> sqlBuilder.append("`").append(o.getFieldName()).append("` ")
                    .append(o.isDescending() ? "DESC," : "ASC,"));
            sqlBuilder.deleteCharAt(sqlBuilder.length() - 1);
        }

        if (query.limit() >= 0) {
            sqlBuilder.append(" LIMIT ?");
        }
        if (query.offset() >= 0) {
            sqlBuilder.append(" OFFSET ?");
        }
        return sqlBuilder.toString();
    }

    @Override
    public String getCountSql() {
        return countSql;
    }

    @Override
    public String getExistSql() {
        return existSql;
    }

    @Override
    public String getExistByIdSql() {
        return existByIdSql;
    }

    @Override
    public String getDeleteSql() {
        return deleteSql;
    }
}
