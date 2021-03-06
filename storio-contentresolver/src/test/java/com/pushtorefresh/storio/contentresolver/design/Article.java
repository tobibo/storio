package com.pushtorefresh.storio.contentresolver.design;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pushtorefresh.storio.contentresolver.operation.put.DefaultPutResolver;
import com.pushtorefresh.storio.contentresolver.operation.put.PutResolver;
import com.pushtorefresh.storio.contentresolver.query.DeleteQuery;
import com.pushtorefresh.storio.operation.MapFunc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class that represents an object stored in ContentProvider
 */
class Article {

    static final Uri CONTENT_URI = mock(Uri.class);

    static final MapFunc<Cursor, Article> MAP_FROM_CURSOR = new MapFunc<Cursor, Article>() {
        @Override
        @NonNull
        public Article map(@NonNull Cursor cursor) {
            return new Article(); // parse cursor here
        }
    };

    static final MapFunc<Article, ContentValues> MAP_TO_CONTENT_VALUES = new MapFunc<Article, ContentValues>() {
        @NonNull
        @Override
        public ContentValues map(@NonNull Article article) {
            final ContentValues contentValues = mock(ContentValues.class);

            when(contentValues.get(BaseColumns._ID))
                    .thenReturn(article.getId());

            return contentValues;
        }
    };

    static final MapFunc<Article, DeleteQuery> MAP_TO_DELETE_QUERY = new MapFunc<Article, DeleteQuery>() {
        @NonNull
        @Override
        public DeleteQuery map(@NonNull Article article) {
            return new DeleteQuery.Builder()
                    .uri(mock(Uri.class))
                    .build();
        }
    };

    static final PutResolver<Article> PUT_RESOLVER = new DefaultPutResolver<Article>() {
        @NonNull
        @Override
        protected Uri getUri(@NonNull ContentValues contentValues) {
            return CONTENT_URI;
        }
    };

    static final PutResolver<ContentValues> PUT_RESOLVER_FOR_CONTENT_VALUES = new DefaultPutResolver<ContentValues>() {
        @NonNull
        @Override
        protected Uri getUri(@NonNull ContentValues contentValues) {
            return CONTENT_URI;
        }
    };

    @Nullable
    private Long id;

    @NonNull
    private String title;

    @Nullable
    public Long getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }
}
