package com.aiethicsquest.presentation.activity;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.aiethicsquest.common.util.ImageLoader;
import com.aiethicsquest.databinding.ActivityImageViewerBinding;

/**
 * 图片全屏查看 Activity.
 *
 * <p>通过 Intent Extra 传入图片 assets 路径，全屏展示并支持：</p>
 * <ul>
 *     <li>双指捏合/展开缩放</li>
 *     <li>单指拖拽平移（放大后）</li>
 *     <li>双击在原始大小与 2.5x 之间切换</li>
 *     <li>点击关闭按钮或按返回键退出</li>
 * </ul>
 *
 * <p>启动方式：</p>
 * <pre>
 *     Intent intent = new Intent(context, ImageViewerActivity.class);
 *     intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_PATH, "ai_images/1.png");
 *     context.startActivity(intent);
 * </pre>
 */
public class ImageViewerActivity extends AppCompatActivity {

    /** Intent Extra Key：图片在 assets 中的路径. */
    public static final String EXTRA_IMAGE_PATH = "extra_image_path";

    private ActivityImageViewerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏显示
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        binding = ActivityImageViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 隐藏 ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        if (imagePath != null && !imagePath.isEmpty()) {
            ImageLoader.loadFromAssets(this, binding.ivZoom, imagePath);
        }

        binding.btnClose.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

