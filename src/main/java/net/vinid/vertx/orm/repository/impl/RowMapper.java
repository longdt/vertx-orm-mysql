package net.vinid.vertx.orm.repository.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQuery;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface RowMapper<ID, E> {

    static <ID, E> Builder<ID, E> builder(String tableName, Supplier<E> supplier) {
        return new BuilderImpl<>(tableName, supplier);
    }

    E map(JsonArray row);

    interface Builder<ID, E> {
        Builder<ID, E> pk(String pkName, Function<E, ID> pkGetter, BiConsumer<E, ID> pkSetter);

        Builder<ID, E> pk(String pkName, Function<E, ID> pkGetter, BiConsumer<E, ID> pkSetter, boolean autogen);

        <T> Builder<ID, E> pkConverter(Function<ID, T> pkGetConverter, Function<T, ID> pkSetConverter);

        <T> Builder<ID, E> addField(String fieldName, Function<? super E, T> getter, BiConsumer<? super E, T> setter);

        <T, D> Builder<ID, E> addField(String fieldName, Function<? super E, T> getter, BiConsumer<? super E, T> setter, Function<T, D> getConverter, Function<D, T> setConverter);

        Builder<ID, E> addUuidField(String fieldName, Function<E, UUID> getter, BiConsumer<E, UUID> setter);

        Builder<ID, E> addJsonObjectField(String fieldName, Function<E, JsonObject> getter, BiConsumer<E, JsonObject> setter);

        <T> Builder<ID, E> addJsonField(String fieldName, Function<E, T> getter, BiConsumer<E, T> setter, Class<T> clazz);

        <T> Builder<ID, E> addJsonField(String fieldName, Function<E, T> getter, BiConsumer<E, T> setter, TypeReference<T> type);

        Builder<ID, E> addDecimalField(String fieldName, Function<E, BigDecimal> getter, BiConsumer<E, BigDecimal> setter);

        Builder<ID, E> addDateField(String fieldName, Function<E, LocalDate> getter, BiConsumer<E, LocalDate> setter);

        Builder<ID, E> addTimeField(String fieldName, Function<E, LocalTime> getter, BiConsumer<E, LocalTime> setter);

        Builder<ID, E> addTimestampField(String fieldName, Function<E, LocalDateTime> getter, BiConsumer<E, LocalDateTime> setter);

        Builder<ID, E> addTimestampTzField(String fieldName, Function<E, OffsetDateTime> getter, BiConsumer<E, OffsetDateTime> setter);

        <T extends Temporal> Builder<ID, E> addTemporalField(String fieldName, Function<E, T> getter, BiConsumer<E, T> setter, DateTimeFormatter formatter, TemporalQuery<T> query);

        RowMapper<ID, E> build();
    }

}
