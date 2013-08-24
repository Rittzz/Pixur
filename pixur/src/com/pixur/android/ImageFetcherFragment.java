package com.pixur.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.ImageView;

public class ImageFetcherFragment extends Fragment implements
        ImageFetcher {

    private static final String LOG_TAG = "ImageFetchFragment";
    private static final boolean LOG_DEBUG = false;

    private static final String DEFAULT_TAG = "image_fetcher_tag";

    private static final int THREAD_COUNT = 3;
    private final Executor executor = Executors
            .newFixedThreadPool(THREAD_COUNT);

    private static Handler handler = new Handler(Looper.getMainLooper());

    private static CookieStore cookieStore;

    public static void init(final FragmentManager fm) {
        if (fm.findFragmentByTag(DEFAULT_TAG) == null) {
            fm.beginTransaction().add(new ImageFetcherFragment(), DEFAULT_TAG).commit();
            fm.executePendingTransactions();
        }
    }

    public static ImageFetcherFragment get(final FragmentManager fm) {
        return (ImageFetcherFragment) fm.findFragmentByTag(DEFAULT_TAG);
    }

    public static void setCookieStore(final CookieStore cookieStore) {
        ImageFetcherFragment.cookieStore = cookieStore;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disableConnectionReuseIfNecessary();
        enableHttpResponseCache();
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void loadImage(final String url, final ImageView imageView) {
        loadImage(url, new ImageViewFetchListener(imageView, url));
    }

    /**
     * Be very careful in your fetchListener method to not hold any context information!
     *
     * TODO Make the caller not have to worry about leaking context.
     */
    @Override
    public void loadImage(final String url, final FetchListener fetchListener) {
        final Bitmap bmp = ImageCache.getInstance().getBitmapFromMemCache(url);
        if (bmp != null) {
            if (LOG_DEBUG) {
                Log.d(LOG_TAG, "Loading image from memcache " + url);
            }
            fetchListener.onSuccess(bmp);
        }
        else {
            // Fetch Image here
            final FetchRunnable runnable = new FetchRunnable(url, fetchListener);
            executor.execute(runnable);
        }
    }

    private void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private void enableHttpResponseCache() {
        try {
            final long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            final File httpCacheDir = new File(getActivity().getCacheDir(),
                    "http");
            Class.forName("android.net.http.HttpResponseCache")
                    .getMethod("install", File.class, long.class)
                    .invoke(null, httpCacheDir, httpCacheSize);
        }
        catch (final Exception httpResponseCacheNotAvailable) {
        }
    }

    private static class FetchRunnable implements Runnable {

        private static final int CONNECTION_TIMEOUT = 30000;
        private final String url;
        private final FetchListener fetchListener;

        public FetchRunnable(final String url, final FetchListener fetchListener) {
            this.url = url;
            this.fetchListener = fetchListener;
        }

        @Override
        public void run() {
//            final URL urlObj = null;
//            final HttpURLConnection urlConnection = null;

            InputStream is = null;

            try {
                if (LOG_DEBUG) {
                    Log.d(LOG_TAG, "Loading image from " + this.url);
                }

                final DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpConnectionParams.setSoTimeout(httpClient.getParams(), CONNECTION_TIMEOUT);
                if (cookieStore != null) {
                    httpClient.setCookieStore(cookieStore);
                }
                final HttpGet httpGet = new HttpGet(this.url);
                final HttpResponse resp = httpClient.execute(httpGet);

                is = new BufferedInputStream(resp.getEntity().getContent());

//                urlObj = new URL(this.url);
//                urlConnection = (HttpURLConnection) urlObj.openConnection();
//                urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
//
//                final InputStream is = new BufferedInputStream(
//                        urlConnection.getInputStream());

                final Bitmap bmp = BitmapFactory.decodeStream(is);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (bmp != null) {
                            if (LOG_DEBUG) {
                                Log.d(LOG_TAG, "Loaded image from " + url);
                            }
                            ImageCache.getInstance().addBitmapToMemoryCache(
                                    url, bmp);
                            fetchListener.onSuccess(bmp);
                        }
                        else {
                            if (LOG_DEBUG) {
                                Log.w(LOG_TAG, "Couldn't Decode Bitmap from url " + url);
                            }
                            fetchListener.onFailure();
                        }
                    }
                });
            }
            catch (final MalformedURLException ex) {
                if (LOG_DEBUG) {
                    Log.w(LOG_TAG, "Bad URL", ex);
                }
                fetchListener.onFailure();
            }
            catch (final IOException ex) {
                if (LOG_DEBUG) {
                    Log.w(LOG_TAG, "IOException", ex);
                }
                fetchListener.onFailure();
            }
            finally {
                if (is != null) {
                    try {
                        is.close();
                    }
                    catch (final IOException e) {

                    }
                }
            }
        }

    }
}
