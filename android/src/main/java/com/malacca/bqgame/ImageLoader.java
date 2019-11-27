package com.malacca.bqgame;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import com.cmcm.cmgame.IImageLoader;

/**
 * 这个类是 sdk 将 ImageLoader 的外置的接口实现。
 * 因为 sdk 使用的 Glide 版本较老，为了避免与接入方使用不同版本导致的冲突，所以将这个功能延迟到 sdk 外面来提供。
 * 如果不存在冲突问题，使用这个默认的实现即可。
 */
public class ImageLoader implements IImageLoader {

    @Override
    public void loadImage(Context context, String imageUrl, ImageView imageView, int defRsid) {
        try {
            Glide.with(context).load(imageUrl).apply(
                    new RequestOptions().placeholder(defRsid).error(defRsid)
            ).into(imageView);
        } catch (Throwable e) {
            // do nothing
        }
    }
}
