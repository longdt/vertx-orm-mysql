package net.vinid.vertx.orm.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Futures {
    public static <T> T join(AsyncResult<T> asyncResult) {
        if (asyncResult.succeeded()) {
            return asyncResult.result();
        } else if (asyncResult.failed()) {
            throw new CompletionException(asyncResult.cause());
        }
        return null;
    }

    public static <T> T join(Future<T> future) {
        if (future.succeeded()) {
            return future.result();
        } else if (future.failed()) {
            throw new CompletionException(future.cause());
        }
        CountDownLatch lock = new CountDownLatch(1);
        future.setHandler(event -> lock.countDown());
        try {
            lock.await();
            return join(future);
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    public static <T, V, U, R> Future<R> toFuture(QuadConsumer<T, V, U, Handler<AsyncResult<R>>> consumer, T arg1, V arg2, U arg3) {
        Promise<R> promise = Promise.promise();
        consumer.accept(arg1, arg2, arg3, promise);
        return promise.future();
    }

    public static <T, V, R> Future<R> toFuture(TriConsumer<T, V, Handler<AsyncResult<R>>> consumer, T arg1, V arg2) {
        Promise<R> promise = Promise.promise();
        consumer.accept(arg1, arg2, promise);
        return promise.future();
    }

    public static <T, R> Future<R> toFuture(BiConsumer<T, Handler<AsyncResult<R>>> consumer, T arg) {
        Promise<R> promise = Promise.promise();
        consumer.accept(arg, promise);
        return promise.future();
    }

    public static <R> Future<R> toFuture(Consumer<Handler<AsyncResult<R>>> consumer) {
        Promise<R> promise = Promise.promise();
        consumer.accept(promise);
        return promise.future();
    }

    public static <T, V, R> R sync(TriConsumer<T, V, Handler<AsyncResult<R>>> consumer, T arg1, V arg2) {
        return join(toFuture(consumer, arg1, arg2));
    }

    public static <T, R> R sync(BiConsumer<T, Handler<AsyncResult<R>>> consumer, T arg) {
        return join(toFuture(consumer, arg));
    }

    public static <R> R sync(Consumer<Handler<AsyncResult<R>>> consumer) {
        return join(toFuture(consumer));
    }
}
