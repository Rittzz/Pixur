package com.pixur.android;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class ImageCache {

    private static ImageCache instance;

    private static int CACHE_SIZE = 1 * 1024 * 1024; // Default of 1mb

    private LruCache<String, Bitmap> mMemoryCache;

    public static synchronized void init(final Application context) {
        instance = new ImageCache(context);
    }

    public static synchronized ImageCache getInstance() {
        if (instance == null) {
            throw new NullPointerException("You must call ImageCache.init before this method");
        }

        return instance;
    }

    private ImageCache(final Application ctx) {
        final int memClass = ((ActivityManager) ctx
                .getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

        // Use 1/8th of the available memory for this memory cache.
        CACHE_SIZE = 1024 * 1024 * memClass / 8;

        mMemoryCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
            @Override
            protected int sizeOf(final String key, final Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return getSizeInBytes(bitmap);
            }
        };
    }

    public void addBitmapToMemoryCache(final String key, final Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(final String key) {
        return mMemoryCache.get(key);
    }

    public Bitmap removeBitmap(final String key) {
        return mMemoryCache.remove(key);
    }

    public void clearCache() {
        mMemoryCache.evictAll();
    }

    private static int getSizeInBytes(final Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }
}
