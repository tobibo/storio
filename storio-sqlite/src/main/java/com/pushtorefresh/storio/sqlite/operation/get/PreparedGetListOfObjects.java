package com.pushtorefresh.storio.sqlite.operation.get;

import android.database.Cursor;
import android.support.annotation.NonNull;

import com.pushtorefresh.storio.operation.MapFunc;
import com.pushtorefresh.storio.operation.PreparedOperationWithReactiveStream;
import com.pushtorefresh.storio.operation.internal.MapSomethingToExecuteAsBlocking;
import com.pushtorefresh.storio.operation.internal.OnSubscribeExecuteAsBlocking;
import com.pushtorefresh.storio.sqlite.StorIOSQLite;
import com.pushtorefresh.storio.sqlite.query.Query;
import com.pushtorefresh.storio.sqlite.query.RawQuery;
import com.pushtorefresh.storio.util.EnvironmentUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import rx.Observable;

import static com.pushtorefresh.storio.util.Checks.checkNotNull;

/**
 * Represents an Operation for {@link StorIOSQLite} which performs query that retrieves data as list of objects
 * from {@link StorIOSQLite}
 *
 * @param <T> type of result
 */
public class PreparedGetListOfObjects<T> extends PreparedGet<List<T>> {

    @NonNull
    private final MapFunc<Cursor, T> mapFunc;

    PreparedGetListOfObjects(@NonNull StorIOSQLite storIOSQLite, @NonNull Query query, @NonNull GetResolver getResolver, @NonNull MapFunc<Cursor, T> mapFunc) {
        super(storIOSQLite, query, getResolver);
        this.mapFunc = mapFunc;
    }

    PreparedGetListOfObjects(@NonNull StorIOSQLite storIOSQLite, @NonNull RawQuery rawQuery, @NonNull GetResolver getResolver, @NonNull MapFunc<Cursor, T> mapFunc) {
        super(storIOSQLite, rawQuery, getResolver);
        this.mapFunc = mapFunc;
    }

    /**
     * Executes Prepared Operation immediately in current thread
     *
     * @return non-null list with mapped results, can be empty
     */
    @SuppressWarnings("TryFinallyCanBeTryWithResources") // Min SDK :(
    @NonNull
    public List<T> executeAsBlocking() {
        final Cursor cursor;

        if (query != null) {
            cursor = getResolver.performGet(storIOSQLite, query);
        } else if (rawQuery != null) {
            cursor = getResolver.performGet(storIOSQLite, rawQuery);
        } else {
            throw new IllegalStateException("Please specify query");
        }

        try {
            final List<T> list = new ArrayList<T>(cursor.getCount());

            while (cursor.moveToNext()) {
                list.add(mapFunc.map(cursor));
            }

            return list;
        } finally {
            cursor.close();
        }
    }

    /**
     * Creates an {@link Observable} which will emit result of operation
     *
     * @return non-null {@link Observable} which will emit non-null list with mapped results, list can be empty
     */
    @NonNull
    public Observable<List<T>> createObservable() {
        EnvironmentUtil.throwExceptionIfRxJavaIsNotAvailable("createObservable()");
        return Observable.create(OnSubscribeExecuteAsBlocking.newInstance(this));
    }

    /**
     * Creates an {@link Observable} which will be subscribed to changes of query tables
     * and will emit result each time change occurs
     * <p/>
     * First result will be emitted immediately,
     * other emissions will occur only if changes of query tables will occur
     *
     * @return non-null {@link Observable} which will emit non-null list with mapped results and will be subscribed to changes of query tables
     */
    @NonNull
    @Override
    public Observable<List<T>> createObservableStream() {
        EnvironmentUtil.throwExceptionIfRxJavaIsNotAvailable("createObservableStream()");

        final Set<String> tables;

        if (query != null) {
            tables = Collections.singleton(query.table);
        } else if (rawQuery != null) {
            tables = rawQuery.affectedTables;
        } else {
            throw new IllegalStateException("Please specify query");
        }

        if (tables != null && !tables.isEmpty()) {
            return storIOSQLite
                    .observeChangesInTables(tables) // each change triggers executeAsBlocking
                    .map(MapSomethingToExecuteAsBlocking.newInstance(this))
                    .startWith(executeAsBlocking()); // start stream with first query result
        } else {
            return createObservable();
        }
    }

    /**
     * Builder for {@link PreparedOperationWithReactiveStream}
     * <p>
     * Required: Firstly you should specify map function
     *
     * @param <T> type of object for query
     */
    public static class Builder<T> {

        @NonNull
        private final StorIOSQLite storIOSQLite;

        private MapFunc<Cursor, T> mapFunc;
        private Query query;
        private RawQuery rawQuery;
        private GetResolver getResolver;

        Builder(@NonNull StorIOSQLite storIOSQLite, @NonNull Class<T> type) {
            this.storIOSQLite = storIOSQLite;
        }

        /**
         * Required: Specifies map function for Get Operation
         * which will map {@link Cursor} to object of required type
         *
         * @param mapFunc map function which will map {@link Cursor} to object of required type
         * @return builder
         */
        @NonNull
        public QueryBuilder<T> withMapFunc(@NonNull MapFunc<Cursor, T> mapFunc) {
            this.mapFunc = mapFunc;
            return new QueryBuilder<T>(this);
        }

        /**
         * Optional: Specifies {@link GetResolver} for Get Operation
         * which allows you to customize behavior of Get Operation
         * <p>
         * Default value is instance of {@link DefaultGetResolver}
         *
         * @param getResolver get resolver
         * @return builder
         */
        @NonNull
        public Builder<T> withGetResolver(@NonNull GetResolver getResolver) {
            this.getResolver = getResolver;
            return this;
        }

        /**
         * Hidden method for prepare Get Operation
         *
         * @return {@link PreparedGetListOfObjects} instance
         */
        @NonNull
        private PreparedOperationWithReactiveStream<List<T>> prepare() {
            if (getResolver == null) {
                getResolver = DefaultGetResolver.INSTANCE;
            }

            checkNotNull(mapFunc, "Please specify map function");

            if (query != null) {
                return new PreparedGetListOfObjects<T>(storIOSQLite, query, getResolver, mapFunc);
            } else if (rawQuery != null) {
                return new PreparedGetListOfObjects<T>(storIOSQLite, rawQuery, getResolver, mapFunc);
            } else {
                throw new IllegalStateException("Please specify query");
            }
        }
    }

    /**
     * Compile-time safe part of builder for {@link PreparedOperationWithReactiveStream}
     * with specified map function
     * <p>
     * Required: You should specify query by call
     * {@link #withQuery(Query)} or {@link #withQuery(RawQuery)}
     *
     * @param <T> type of object for query
     */
    public static class QueryBuilder<T> {

        private Builder<T> incompleteBuilder;

        public QueryBuilder(@NonNull final Builder<T> builder) {
            this.incompleteBuilder = builder;
        }
        /**
         * Specifies {@link Query} for Get Operation
         *
         * @param query query
         * @return builder
         */
        @NonNull
        public CompleteBuilder<T> withQuery(@NonNull Query query) {
            incompleteBuilder.query = query;
            return new CompleteBuilder<T>(this);
        }

        /**
         * Specifies {@link RawQuery} for Get Operation,
         * you can use it for "joins" and same constructions which are not allowed in {@link Query}
         *
         * @param rawQuery query
         * @return builder
         */
        @NonNull
        public CompleteBuilder<T> withQuery(@NonNull RawQuery rawQuery) {
            incompleteBuilder.rawQuery = rawQuery;
            return new CompleteBuilder<T>(this);
        }

        /**
         * Optional: Specifies {@link GetResolver} for Get Operation
         * which allows you to customize behavior of Get Operation
         * <p>
         * Default value is instance of {@link DefaultGetResolver}
         *
         * @param getResolver get resolver
         * @return builder
         */
        @NonNull
        public QueryBuilder<T> withGetResolver(@NonNull GetResolver getResolver) {
            incompleteBuilder.withGetResolver(getResolver);
            return this;
        }

        /**
         * Hidden method for prepare Get Operation
         *
         * @return {@link PreparedGetListOfObjects} instance
         */
        @NonNull
        private PreparedOperationWithReactiveStream<List<T>> prepare() {
            return incompleteBuilder.prepare();
        }
    }

    /**
     * Compile-time safe part of builder for {@link PreparedOperationWithReactiveStream}
     *
     * @param <T> type of object for query
     */
    public static class CompleteBuilder<T> {

        private QueryBuilder<T> queryBuilder;

        public CompleteBuilder(@NonNull final QueryBuilder<T> builder) {
            this.queryBuilder = builder;
        }

        /**
         * Optional: Specifies {@link GetResolver} for Get Operation
         * which allows you to customize behavior of Get Operation
         * <p>
         * Default value is instance of {@link DefaultGetResolver}
         *
         * @param getResolver get resolver
         * @return builder
         */
        @NonNull
        public CompleteBuilder<T> withGetResolver(@NonNull GetResolver getResolver) {
            queryBuilder.withGetResolver(getResolver);
            return this;
        }

        /**
         * Prepares Get Operation
         *
         * @return {@link PreparedGetListOfObjects} instance
         */
        @NonNull
        public PreparedOperationWithReactiveStream<List<T>> prepare() {
            return queryBuilder.prepare();
        }
    }
}