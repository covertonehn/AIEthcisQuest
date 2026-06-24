package com.aiethicsquest.presentation.activity;

import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.aiethicsquest.R;
import com.aiethicsquest.databinding.ActivityVideoViewerBinding;

import java.io.IOException;

/**
 * 全屏视频播放 Activity.
 *
 * <p>功能：</p>
 * <ul>
 *     <li>视频循环播放</li>
 *     <li>单击屏幕：切换顶部/底部控制栏显隐（沉浸式播放）</li>
 *     <li>双击屏幕：播放/暂停</li>
 *     <li>底部进度条支持拖动</li>
 *     <li>播放时控制栏 3 秒后自动隐藏</li>
 *     <li>点击返回或退出全屏按钮关闭全屏，回传播放进度</li>
 *     <li>TextureView 居中显示（与视频宽高比匹配）</li>
 * </ul>
 */
public class VideoViewerActivity extends AppCompatActivity
        implements TextureView.SurfaceTextureListener {

    private static final String TAG = "VideoViewerActivity";

    /** Intent Extra Key：视频在 assets 中的路径. */
    public static final String EXTRA_VIDEO_PATH = "extra_video_path";
    /** Intent Extra Key：起始播放位置（毫秒）. */
    public static final String EXTRA_START_POSITION = "extra_start_position";
    /** Intent Extra Key：是否自动播放. */
    public static final String EXTRA_AUTO_PLAY = "extra_auto_play";

    /** Result Extra Key：退出时的播放位置（毫秒）. */
    public static final String RESULT_POSITION = "result_position";
    /** Result Extra Key：退出时是否正在播放. */
    public static final String RESULT_IS_PLAYING = "result_is_playing";

    private static final int PROGRESS_UPDATE_INTERVAL = 200;
    private static final int CONTROLS_AUTO_HIDE_DELAY = 3000;

    private ActivityVideoViewerBinding binding;
    private MediaPlayer mediaPlayer;
    private String videoPath;
    private int startPosition;
    private boolean autoPlay;
    private boolean isReady = false;

    /** 控制栏当前是否显示. */
    private boolean controlsVisible = false;

    private GestureDetector gestureDetector;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = this::updateProgress;

    private final Handler hideControlsHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = this::hideControls;

    private boolean isSeekBarDragging = false;

    // ---------- 生命周期 ----------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏显示
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        binding = ActivityVideoViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        startPosition = getIntent().getIntExtra(EXTRA_START_POSITION, 0);
        autoPlay = getIntent().getBooleanExtra(EXTRA_AUTO_PLAY, true);

        binding.textureViewFullscreen.setSurfaceTextureListener(this);
        binding.progressBarFullscreen.setVisibility(View.VISIBLE);
        binding.overlayPlayFullscreen.setVisibility(View.GONE);

        setupGestures();
        setupControls();
    }

    // ---------- 手势 ----------

    private void setupGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControls();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                togglePlayPause();
                return true;
            }
        });

        binding.textureViewFullscreen.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
        binding.overlayPlayFullscreen.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    // ---------- 控制栏 ----------

    private void setupControls() {
        binding.btnPlayPauseFullscreen.setOnClickListener(v -> {
            togglePlayPause();
            scheduleHideControls();
        });

        binding.btnBackFullscreen.setOnClickListener(v -> finishWithResult());
        binding.btnExitFullscreen.setOnClickListener(v -> finishWithResult());

        binding.seekBarFullscreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && isReady) {
                    try {
                        int target = (int) ((long) progress * mediaPlayer.getDuration()
                                / sb.getMax());
                        mediaPlayer.seekTo(target);
                    } catch (IllegalStateException e) {
                        Log.w(TAG, "seek 失败", e);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                isSeekBarDragging = true;
                cancelHideControls();
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                isSeekBarDragging = false;
                scheduleHideControls();
            }
        });
    }

    private void toggleControls() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        controlsVisible = true;
        binding.layoutTopBar.setVisibility(View.VISIBLE);
        binding.layoutBottomControls.setVisibility(View.VISIBLE);
        scheduleHideControls();
    }

    private void hideControls() {
        controlsVisible = false;
        binding.layoutTopBar.setVisibility(View.GONE);
        binding.layoutBottomControls.setVisibility(View.GONE);
        cancelHideControls();
    }

    private void scheduleHideControls() {
        cancelHideControls();
        if (isPlaying()) {
            hideControlsHandler.postDelayed(hideControlsRunnable, CONTROLS_AUTO_HIDE_DELAY);
        }
    }

    private void cancelHideControls() {
        hideControlsHandler.removeCallbacks(hideControlsRunnable);
    }

    // ---------- MediaPlayer ----------

    private void prepareMediaPlayer(Surface surface) {
        if (videoPath == null || videoPath.isEmpty()) return;

        releaseMediaPlayer();
        mediaPlayer = new MediaPlayer();

        try {
            AssetFileDescriptor afd = getAssets().openFd(videoPath);
            mediaPlayer.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        } catch (IOException e) {
            Log.e(TAG, "打开视频文件失败: " + videoPath, e);
            binding.progressBarFullscreen.setVisibility(View.GONE);
            return;
        }

        mediaPlayer.setSurface(surface);
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build());
        mediaPlayer.setLooping(true);

        mediaPlayer.setOnPreparedListener(mp -> {
            isReady = true;
            binding.progressBarFullscreen.setVisibility(View.GONE);

            int w = mp.getVideoWidth();
            int h = mp.getVideoHeight();
            if (w > 0 && h > 0) {
                adjustTextureViewAspectRatio(w, h);
            }

            int duration = mp.getDuration();
            binding.seekBarFullscreen.setMax(duration > 0 ? duration : 1000);

            if (startPosition > 0) {
                mp.seekTo(startPosition);
            }

            if (autoPlay) {
                mp.start();
                updatePlayPauseButton(true);
                binding.overlayPlayFullscreen.setVisibility(View.GONE);
                startProgressUpdates();
            } else {
                updatePlayPauseButton(false);
                binding.overlayPlayFullscreen.setVisibility(View.VISIBLE);
            }
        });

        mediaPlayer.setOnVideoSizeChangedListener((mp, width, height) ->
                adjustTextureViewAspectRatio(width, height));

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer 错误: what=" + what + " extra=" + extra);
            binding.progressBarFullscreen.setVisibility(View.GONE);
            isReady = false;
            return false;
        });

        try {
            mediaPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            Log.e(TAG, "prepareAsync 失败", e);
        }
    }

    /**
     * 调整 TextureView 尺寸以保持视频宽高比，并居中显示.
     * TextureView 的 layout_gravity=center 已在 XML 中设置。
     */
    private void adjustTextureViewAspectRatio(int videoWidth, int videoHeight) {
        if (videoWidth <= 0 || videoHeight <= 0) return;

        int screenWidth = getWindow().getDecorView().getWidth();
        int screenHeight = getWindow().getDecorView().getHeight();
        if (screenWidth <= 0 || screenHeight <= 0) return;

        float videoRatio = (float) videoWidth / videoHeight;
        float screenRatio = (float) screenWidth / screenHeight;

        int finalWidth, finalHeight;
        if (videoRatio > screenRatio) {
            finalWidth = screenWidth;
            finalHeight = (int) (screenWidth / videoRatio);
        } else {
            finalHeight = screenHeight;
            finalWidth = (int) (screenHeight * videoRatio);
        }

        android.view.ViewGroup.LayoutParams lp = binding.textureViewFullscreen.getLayoutParams();
        lp.width = finalWidth;
        lp.height = finalHeight;
        binding.textureViewFullscreen.setLayoutParams(lp);
    }

    private void togglePlayPause() {
        if (mediaPlayer == null || !isReady) return;
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                updatePlayPauseButton(false);
                binding.overlayPlayFullscreen.setVisibility(View.VISIBLE);
                stopProgressUpdates();
                cancelHideControls();
            } else {
                mediaPlayer.start();
                updatePlayPauseButton(true);
                binding.overlayPlayFullscreen.setVisibility(View.GONE);
                startProgressUpdates();
                scheduleHideControls();
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "togglePlayPause 失败", e);
        }
    }

    private boolean isPlaying() {
        if (mediaPlayer == null || !isReady) return false;
        try {
            return mediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private void updatePlayPauseButton(boolean playing) {
        binding.btnPlayPauseFullscreen.setImageResource(
                playing ? R.drawable.ic_video_pause : R.drawable.ic_video_play);
    }

    private void finishWithResult() {
        android.content.Intent result = new android.content.Intent();
        int position = 0;
        boolean playing = false;
        if (mediaPlayer != null && isReady) {
            try {
                position = mediaPlayer.getCurrentPosition();
                playing = mediaPlayer.isPlaying();
            } catch (IllegalStateException e) {
                Log.w(TAG, "获取播放状态失败", e);
            }
        }
        result.putExtra(RESULT_POSITION, position);
        result.putExtra(RESULT_IS_PLAYING, playing);
        setResult(RESULT_OK, result);
        finish();
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isReady = false;
    }

    // ---------- 进度更新 ----------

    private void startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.postDelayed(progressRunnable, PROGRESS_UPDATE_INTERVAL);
    }

    private void stopProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    private void updateProgress() {
        if (mediaPlayer != null && isReady && !isSeekBarDragging) {
            try {
                int current = mediaPlayer.getCurrentPosition();
                binding.seekBarFullscreen.setProgress(current);
            } catch (IllegalStateException ignored) {
            }
        }
        if (isPlaying()) {
            progressHandler.postDelayed(progressRunnable, PROGRESS_UPDATE_INTERVAL);
        }
    }

    // ---------- TextureView.SurfaceTextureListener ----------

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture,
                                          int width, int height) {
        prepareMediaPlayer(new Surface(surfaceTexture));
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture,
                                            int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        releaseMediaPlayer();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
    }

    // ---------- 生命周期 ----------

    @Override
    public void onBackPressed() {
        finishWithResult();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdates();
        cancelHideControls();
        releaseMediaPlayer();
        binding = null;
    }
}

