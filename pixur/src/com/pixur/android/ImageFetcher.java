package com.pixur.android;

import android.graphics.Bitmap;
import android.widget.ImageView;

public interface ImageFetcher {
    public void loadImage(final String url, final ImageView imageView);

    public void loadImage(final String url, final FetchListener fetchListener);

    public interface FetchListener {
        public void onSuccess(final Bitmap bmp);

        public void onFailure();
    }
}
