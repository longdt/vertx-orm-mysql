package net.vinid.vertx.orm.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQuery;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BuilderImpl<ID, E> implements RowMapper.Builder<ID, E> {
    private String tableName;
    private Supplier<E> supplier;
    private String pkName;
    private boolean pkAutoGen;
    private Function<E, ID> pkGetter;
    private BiConsumer<E, ID> pkSetter;
    private FieldMapping<E, ?> pkConverter;
    private Function<ID, ?> pkGetConverter;
    private Map<String, FieldMapping<E, ?>> mappings;

    public BuilderImpl(String tableName, Supplier<E> supplier) {
        this.tableName = tableName;
        this.supplier = supplier;
        mappings = new LinkedHashMap<>();
    }

    @Override
    public RowMapper.Builder<ID, E> pk(String pkName, Function<E, ID> pkGetter, BiConsumer<E, ID> pkSetter) {
        return pk(pkName, pkGetter, pkSetter, false);
    }

    @Override
    public RowMapper.Builder<ID, E> pk(String pkName, Function<E, ID> pkGetter, BiConsumer<E, ID> pkSetter, boolean autogen) {
        this.pkName = pkName;
        this.pkGetter = pkGetter;
        this.pkSetter = pkSetter;
        this.pkAutoGen = autogen;
        pkConverter = new FieldMapping<>(pkName, pkGetter, pkSetter);
        pkGetConverter = Function.identity();
        mappings.put(pkName, pkConverter);
        return this;
    }

    @Override
    public <T> RowMapper.Builder<ID, E> pkConverter(Function<ID, T> pkGetConverter, Function<T, ID> pkSetConverter) {
        Objects.requireNonNull(pkName);
        this.pkGetConverter = pkGetConverter;
        pkConverter =
                new FieldMapping<>(pkName,
                        entity -> pkGetConverter.apply(pkGetter.apply(entity)),
                        (entity, value) -> pkSetter.accept(entity, pkSetConverter.apply(value)));
        mappings.put(pkName, pkConverter);
        return this;
    }

    @Override
    public <T> RowMapper.Builder<ID, E> addField(String fieldName, Function<? super E, T> getter, BiConsumer<? super E, T> setter) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, getter, setter));
        return this;
    }

    @Override
    public <T, D> RowMapper.Builder<ID, E> addField(String fieldName, Function<? super E, T> getter, BiConsumer<? super E, T> setter, Function<T, D> getConverter, Function<D, T> setConverter) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? getConverter.apply(v) : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, setConverter.apply(value));
                }));
        return this;
    }

    @Override
    public RowMapper.Builder<ID, E> addUuidField(String fieldName, Function<E, UUID> getter, BiConsumer<E, UUID> setter) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? v.toString() : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, UUID.fromString(value));
                }));
        return this;
    }

    @Override
    public RowMapper.Builder<ID, E> addJsonObjectField(String fieldName, Function<E, JsonObject> getter, BiConsumer<E, JsonObject> setter) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? v.toString() : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, new JsonObject(value));
                }));
        return this;
    }

    @Override
    public <T> RowMapper.Builder<ID, E> addJsonField(String fieldName, Function<E, T> getter, BiConsumer<E, T> setter, Class<T> clazz) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? Json.encode(v) : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, Json.decodeValue(value, clazz));
                }));
        return this;
    }

    @Override
    public <T> RowMapper.Builder<ID, E> addJsonField(String fieldName, Function<E, T> getter, BiConsumer<E, T> setter, TypeReference<T> type) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? Json.encode(v) : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, Json.decodeValue(value, type));
                }));
        return this;
    }

    @Override
    public RowMapper.Builder<ID, E> addDecimalField(String fieldName, Function<E, BigDecimal> getter, BiConsumer<E, BigDecimal> setter) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? v.toString() : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, new BigDecimal(value));
                }));
        return this;
    }

    @Override
    public RowMapper.Builder<ID, E> addDateField(String fieldName, Function<E, LocalDate> getter, BiConsumer<E, LocalDate> setter) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? v.toString() : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, LocalDate.parse(value));
                }));
        return this;
    }

    @Override
    public RowMapper.Builder<ID, E> addTimeField(String fieldName, Function<E, LocalTime> getter, BiConsumer<E, LocalTime> setter) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? v.toString() : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, LocalTime.parse(value));
                }));
        return this;
    }

    @Override
    public RowMapper.Builder<ID, E> addTimestampField(String fieldName, Function<E, LocalDateTime> getter, BiConsumer<E, LocalDateTime> setter) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? v.toString() : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, LocalDateTime.parse(value));
                }));
        return this;
    }

    @Override
    public RowMapper.Builder<ID, E> addTimestampTzField(String fieldName, Function<E, OffsetDateTime> getter, BiConsumer<E, OffsetDateTime> setter) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? v.toString() : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, OffsetDateTime.parse(value));
                }));
        return this;
    }

    @Override
    public <T extends Temporal> RowMapper.Builder<ID, E> addTemporalField(String fieldName, Function<E, T> getter, BiConsumer<E, T> setter, DateTimeFormatter formatter, TemporalQuery<T> query) {
        mappings.put(fieldName, new FieldMapping<>(fieldName, entity -> {
            var v = getter.apply(entity);
            return v != null ? formatter.format(v) : null;
        },
                (entity, value) -> {
                    if (value != null) setter.accept(entity, query.queryFrom(formatter.parse(value)));
                }));
        return this;
    }

    @Override
    public RowMapper<ID, E> build() {
        return new RowMapperImpl<>(this);
    }

    public String getTableName() {
        return tableName;
    }

    public Supplier<E> getSupplier() {
        return supplier;
    }

    public String getPkName() {
        return pkName;
    }

    public boolean isPkAutoGen() {
        return pkAutoGen;
    }

    public Function<E, ID> getPkGetter() {
        return pkGetter;
    }

    public BiConsumer<E, ID> getPkSetter() {
        return pkSetter;
    }

    public FieldMapping<E, ?> getPkConverter() {
        return pkConverter;
    }

    public Function<ID, ?> getPkGetConverter() {
        return pkGetConverter;
    }

    public Map<String, FieldMapping<E, ?>> getMappings() {
        return mappings;
    }
}