package ch.haikou.pogomapclient.utils;

/**
 * Created by Zoruk on 08.08.2016.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * Cache wrapper around the Glide image loader.
 * 29.07.16: for now, the second layer cache is removed, caching is handled by Glide.
 * <p>
 * Created by aronhomberg on 26.07.16.
 */
public class RemoteImageLoader {

    public static void loadMapIcon(Context context, final String url, int pxWidth, int pxHeight,
                                   Drawable placeholderDrawable, final Callback onFetch) {
        Glide.with(context).load(url)
                .asBitmap()
                .skipMemoryCache(false)
                .placeholder(placeholderDrawable)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .into(new SimpleTarget<Bitmap>(pxWidth, pxHeight) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        onFetch.onFetch(bitmap);
                    }
                });
    }

    public static void loadMapIcon(Context context, final String url, int pxWidth, int pxHeight,
                                   final Callback onFetch) {
        Glide.with(context).load(url)
                .asBitmap()
                .skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .into(new SimpleTarget<Bitmap>(pxWidth, pxHeight) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        onFetch.onFetch(bitmap);
                    }
                });
    }

    public static Bitmap loadSyncIcon(Context context, final String url) {
        try {
            return  Glide.with(context)
                    .load(url)
                    .asBitmap()
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void loadInto(ImageView view, String url, Drawable placeholderDrawable) {
        Glide.with(view.getContext()).load(url)
                .asBitmap()
                .skipMemoryCache(false)
                .placeholder(placeholderDrawable)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(view);
    }

    public static void loadInto(ImageView view, String url) {
        Glide.with(view.getContext()).load(url)
                .asBitmap()
                .skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(view);
    }

    public static Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public interface Callback {
        void onFetch(Bitmap bitmap);
    }
}
