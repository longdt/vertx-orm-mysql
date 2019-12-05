package net.vinid.vertx.orm.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SQLHelper {
    public static <T, V, R> void executeInConnection(SQLClient sqlClient, QuadConsumer<SQLConnection, T, V, Handler<AsyncResult<R>>> consumer, T arg1, V arg2, Handler<AsyncResult<R>> resultHandler) {
        sqlClient.getConnection(getConn -> {
            if (getConn.failed()) {
                resultHandler.handle(Future.failedFuture(getConn.cause()));
            } else {
                final SQLConnection conn = getConn.result();
                consumer.accept(conn, arg1, arg2, res -> {
                    if (res.failed()) {
                        conn.close(close -> {
                            if (close.failed()) {
                                resultHandler.handle(Future.failedFuture(close.cause()));
                            } else {
                                resultHandler.handle(Future.failedFuture(res.cause()));
                            }
                        });
                    } else {
                        conn.close(close -> {
                            if (close.failed()) {
                                resultHandler.handle(Future.failedFuture(close.cause()));
                            } else {
                                resultHandler.handle(Future.succeededFuture(res.result()));
                            }
                        });
                    }
                });
            }
        });
    }

    public static <T, R> void executeInConnection(SQLClient sqlClient, TriConsumer<SQLConnection, T, Handler<AsyncResult<R>>> consumer, T arg, Handler<AsyncResult<R>> resultHandler) {
        sqlClient.getConnection(getConn -> {
            if (getConn.failed()) {
                resultHandler.handle(Future.failedFuture(getConn.cause()));
            } else {
                final SQLConnection conn = getConn.result();
                consumer.accept(conn, arg, res -> {
                    if (res.failed()) {
                        conn.close(close -> {
                            if (close.failed()) {
                                resultHandler.handle(Future.failedFuture(close.cause()));
                            } else {
                                resultHandler.handle(Future.failedFuture(res.cause()));
                            }
                        });
                    } else {
                        conn.close(close -> {
                            if (close.failed()) {
                                resultHandler.handle(Future.failedFuture(close.cause()));
                            } else {
                                resultHandler.handle(Future.succeededFuture(res.result()));
                            }
                        });
                    }
                });
            }
        });
    }

    public static <T, R> void executeInConnection(SQLClient sqlClient, BiConsumer<SQLConnection, Handler<AsyncResult<R>>> consumer, Handler<AsyncResult<R>> resultHandler) {
        sqlClient.getConnection(getConn -> {
            if (getConn.failed()) {
                resultHandler.handle(Future.failedFuture(getConn.cause()));
            } else {
                final SQLConnection conn = getConn.result();
                consumer.accept(conn, res -> {
                    if (res.failed()) {
                        conn.close(close -> {
                            if (close.failed()) {
                                resultHandler.handle(Future.failedFuture(close.cause()));
                            } else {
                                resultHandler.handle(Future.failedFuture(res.cause()));
                            }
                        });
                    } else {
                        conn.close(close -> {
                            if (close.failed()) {
                                resultHandler.handle(Future.failedFuture(close.cause()));
                            } else {
                                resultHandler.handle(Future.succeededFuture(res.result()));
                            }
                        });
                    }
                });
            }
        });
    }

    public static <T> void executeInConnection(SQLClient sqlClient, Function<SQLConnection, Future<T>> action, Handler<AsyncResult<T>> resultHandler) {
        sqlClient.getConnection(getConn -> {
            if (getConn.failed()) {
                resultHandler.handle(Future.failedFuture(getConn.cause()));
            } else {
                final SQLConnection conn = getConn.result();
                action.apply(conn).setHandler(res -> {
                    if (res.failed()) {
                        conn.close(close -> {
                            if (close.failed()) {
                                resultHandler.handle(Future.failedFuture(close.cause()));
                            } else {
                                resultHandler.handle(Future.failedFuture(res.cause()));
                            }
                        });
                    } else {
                        conn.close(close -> {
                            if (close.failed()) {
                                resultHandler.handle(Future.failedFuture(close.cause()));
                            } else {
                                resultHandler.handle(Future.succeededFuture(res.result()));
                            }
                        });
                    }
                });
            }
        });
    }

    public static <T> void inTransactionSingle(SQLClient sqlClient, Function<SQLConnection, Future<T>> action, Handler<AsyncResult<T>> resultHandler) {
        executeInConnection(sqlClient, conn -> Futures.toFuture(conn::setAutoCommit, false).map(conn).compose(action), resultHandler);
    }
}
