package com.pixur.android;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

public class ImageViewFetchListener implements ImageFetcher.FetchListener {

    private final String url;
    private final WeakReference<ImageView> wrefImageView;

    public ImageViewFetchListener(final ImageView imageView, final String url) {
        wrefImageView = new WeakReference<ImageView>(imageView);
        this.url = url;
        imageView.setTag(url);
    }

    @Override
    public final void onSuccess(final Bitmap bmp) {
        final ImageView imageView = wrefImageView.get();
        if (imageView != null && isUrlMine(imageView, url)) {
            onSuccess(imageView, bmp);
        }
    }

    public void onSuccess(final ImageView imageView, final Bitmap bmp) {
        imageView.setImageBitmap(bmp);
    }

    @Override
    public final void onFailure() {
        final ImageView imageView = wrefImageView.get();
        if (imageView != null) {
            onFailure(imageView);
        }
    }

    public void onFailure(final ImageView imageView) {
        Log.w("ImageViewFetchListener", "Failed to fetch image with url of "
                + url);
    }

    // Since we will be used with listviews, use the tag of the object to
    // determine if we belong
    private static boolean isUrlMine(final ImageView iv, final String url) {
        return url != null && url.equals(iv.getTag());
    }
}
