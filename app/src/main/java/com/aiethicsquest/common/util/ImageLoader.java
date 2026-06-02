package com.aiethicsquest.common.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.aiethicsquest.R;

import java.io.IOException;
import java.io.InputStream;

/**
 * 图片加载工具类.
 *
 * <p>从 assets 目录加载图片并设置到 ImageView。
 * 若资源不存在则显示占位图 {@code ic_image_placeholder}。</p>
 *
 * <p>调用方式：</p>
 * <pre>
 *     // 加载 assets/ai_images/1.jpg
 *     ImageLoader.loadFromAssets(context, imageView, "ai_images/1.jpg");
 *
 *     // 或使用 QuizQuestion 的便捷方法获取路径
 *     ImageLoader.loadFromAssets(context, imageView, question.getLeftImagePath());
 * </pre>
 */
public class ImageLoader {

    private static final String TAG = "ImageLoader";

    private ImageLoader() {
        // 工具类，禁止实例化
    }

    /**
     * 从 assets 目录加载图片到 ImageView.
     *
     * <p>此方法在调用线程执行（通常是主线程），图片较大时建议在后台线程预加载。
     * 当前版本为简单同步加载，适用于小图场景（缩略图、挑战图等）。</p>
     *
     * @param context   Context
     * @param imageView 目标 ImageView
     * @param assetPath assets 相对路径，如 "ai_images/1.jpg"
     */
    public static void loadFromAssets(Context context, ImageView imageView, String assetPath) {
        if (assetPath == null || assetPath.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_image_placeholder);
            }
        } catch (IOException e) {
            // 文件不存在（图片素材尚未添加）
            Log.w(TAG, "assets 图片不存在：" + assetPath);
            imageView.setImageResource(R.drawable.ic_image_placeholder);
        }
    }
}

