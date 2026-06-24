package com.aiethicsquest.presentation.activity;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aiethicsquest.R;

import java.io.IOException;

/**
 * 内联视频播放器自定义 View.
 *
 * <p>功能特性：</p>
 * <ul>
 *     <li>从 assets 目录加载视频并播放</li>
 *     <li>加载完成后自动展示首帧作为封面（暂停状态）</li>
 *     <li>单击视频区域：切换底部控制栏显隐（纯净播放模式）</li>
 *     <li>双击视频区域：播放/暂停</li>
 *     <li>底部控制栏：播放/暂停按钮 + 播放进度条 + 全屏按钮</li>
 *     <li>视频循环播放</li>
 *     <li>通过 {@link OnFullscreenRequestListener} 回调请求全屏播放</li>
 * </ul>
 */
public class VideoPlayerView extends FrameLayout
        implements TextureView.SurfaceTextureListener {

    private static final String TAG = "VideoPlayerView";

    /** 进度条刷新间隔（毫秒）. */
    private static final int PROGRESS_UPDATE_INTERVAL = 200;

    /** 控制栏自动隐藏延迟（毫秒）. */
    private static final int CONTROLS_AUTO_HIDE_DELAY = 3000;

    // ---------- Views ----------
    private TextureView textureView;
    private ImageView ivThumbnail;
    private ImageButton btnPlayPause;
    private ImageButton btnFullscreen;
    private ProgressBar progressBarLoading;
    private View layoutControls;
    private View overlayPlayIcon;
    private SeekBar seekBar;

    // ---------- 播放状态 ----------
    private MediaPlayer mediaPlayer;
    private String videoAssetPath;
    private boolean isReady = false;
    private boolean shouldPlayOnReady = false;

    /** 控制栏当前是否显示. */
    private boolean controlsVisible = false;

    // ---------- 手势 ----------
    private GestureDetector gestureDetector;

    // ---------- 进度刷新 ----------
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = this::updateProgress;

    /** 用户正在拖动进度条时，暂停自动刷新. */
    private boolean isSeekBarDragging = false;

    // ---------- 自动隐藏控制栏 ----------
    private final Handler hideControlsHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = this::hideControls;

    // ---------- 全屏回调 ----------
    /** 全屏请求回调. */
    public interface OnFullscreenRequestListener {
        void onFullscreenRequest(String videoAssetPath, boolean isPlaying, int position);
    }

    private OnFullscreenRequestListener fullscreenListener;

    // ---------- 构造 ----------

    public VideoPlayerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // ---------- 初始化 ----------

    private void init(Context context) {
        // TextureView 不支持 background 属性，在父容器 FrameLayout（即本 View）上设置黑色背景
        setBackgroundColor(0xFF000000);
        LayoutInflater.from(context).inflate(R.layout.view_video_player, this, true);

        textureView = findViewById(R.id.texture_view);
        ivThumbnail = findViewById(R.id.iv_thumbnail);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnFullscreen = findViewById(R.id.btn_fullscreen);
        progressBarLoading = findViewById(R.id.progress_bar_video);
        layoutControls = findViewById(R.id.layout_controls);
        overlayPlayIcon = findViewById(R.id.overlay_play_icon);
        seekBar = findViewById(R.id.seek_bar_video);

        textureView.setSurfaceTextureListener(this);

        // 手势检测（单击 = 切换控制栏，双击 = 播放/暂停）
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
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

        // 触摸事件转交手势检测
        textureView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
        overlayPlayIcon.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        // 底部播放/暂停按钮
        btnPlayPause.setOnClickListener(v -> {
            togglePlayPause();
            scheduleHideControls();
        });

        // 全屏按钮
        btnFullscreen.setOnClickListener(v -> {
            if (fullscreenListener != null) {
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
                fullscreenListener.onFullscreenRequest(videoAssetPath, playing, position);
            }
        });

        // 进度条拖动
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && isReady) {
                    try {
                        int target = (int) ((long) progress * mediaPlayer.getDuration()
                                / seekBar.getMax());
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

    // ---------- 公开接口 ----------

    /**
     * 设置视频 assets 路径并加载视频.
     *
     * @param assetPath assets 相对路径，如 "ai_videos/1.mp4"
     */
    public void setVideoPath(String assetPath) {
        if (assetPath == null || assetPath.isEmpty()) return;

        if (videoAssetPath != null && videoAssetPath.equals(assetPath)
                && mediaPlayer != null) {
            return; // 相同路径不重复加载
        }

        this.videoAssetPath = assetPath;
        isReady = false;
        shouldPlayOnReady = false;
        controlsVisible = false;

        progressBarLoading.setVisibility(View.VISIBLE);
        overlayPlayIcon.setVisibility(View.GONE);
        layoutControls.setVisibility(View.GONE);
        if (ivThumbnail != null) {
            ivThumbnail.setVisibility(View.GONE);
        }
        seekBar.setProgress(0);
        btnPlayPause.setImageResource(R.drawable.ic_video_play);

        stopProgressUpdates();
        releaseMediaPlayer();

        // 提前异步提取首帧缩略图
        extractThumbnailAsync(assetPath);

        // 如果 Surface 已就绪，直接加载；否则等 SurfaceTextureListener 回调
        if (textureView.isAvailable()) {
            prepareMediaPlayer(new Surface(textureView.getSurfaceTexture()));
        }
    }

    /**
     * 从指定位置恢复播放（用于全屏返回后同步进度）.
     *
     * @param position 毫秒
     */
    public void seekAndPlay(int position) {
        if (mediaPlayer != null && isReady) {
            try {
                mediaPlayer.seekTo(position);
                mediaPlayer.start();
                updatePlayPauseButton(true);
                overlayPlayIcon.setVisibility(View.GONE);
                hideThumbnail();
                startProgressUpdates();
            } catch (IllegalStateException e) {
                Log.w(TAG, "seekAndPlay 失败", e);
            }
        } else {
            shouldPlayOnReady = true;
        }
    }

    /** 暂停播放. */
    public void pause() {
        if (mediaPlayer != null && isReady) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    updatePlayPauseButton(false);
                    overlayPlayIcon.setVisibility(View.VISIBLE);
                    stopProgressUpdates();
                }
            } catch (IllegalStateException e) {
                Log.w(TAG, "pause 失败", e);
            }
        }
    }

    /** 停止播放并释放资源. */
    public void release() {
        stopProgressUpdates();
        releaseMediaPlayer();
    }

    /** 重置为初始状态（停止播放并重新从头加载当前视频）. */
    public void reset() {
        if (videoAssetPath != null) {
            String path = videoAssetPath;
            videoAssetPath = null;
            setVideoPath(path);
        }
    }

    /** 设置全屏请求回调. */
    public void setOnFullscreenRequestListener(OnFullscreenRequestListener listener) {
        this.fullscreenListener = listener;
    }

    /** 获取当前视频路径. */
    public String getVideoAssetPath() {
        return videoAssetPath;
    }

    /** 当前是否正在播放. */
    public boolean isPlaying() {
        if (mediaPlayer == null || !isReady) return false;
        try {
            return mediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    // ---------- 控制栏显隐 ----------

    /** 切换控制栏显示/隐藏. */
    private void toggleControls() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    /** 显示控制栏，并安排自动隐藏. */
    private void showControls() {
        controlsVisible = true;
        layoutControls.setVisibility(View.VISIBLE);
        scheduleHideControls();
    }

    /** 隐藏控制栏. */
    private void hideControls() {
        controlsVisible = false;
        layoutControls.setVisibility(View.GONE);
        cancelHideControls();
    }

    /** 延迟自动隐藏控制栏（若正在播放）. */
    private void scheduleHideControls() {
        cancelHideControls();
        if (isPlaying()) {
            hideControlsHandler.postDelayed(hideControlsRunnable, CONTROLS_AUTO_HIDE_DELAY);
        }
    }

    private void cancelHideControls() {
        hideControlsHandler.removeCallbacks(hideControlsRunnable);
    }

    // ---------- 首帧缩略图 ----------

    /** 异步提取首帧，加载完后在主线程显示. */
    private void extractThumbnailAsync(String assetPath) {
        if (ivThumbnail == null) return;
        new Thread(() -> {
            Bitmap bmp = extractFirstFrame(assetPath);
            post(() -> {
                // 确认路径没有变化（避免 View 复用时显示错误图）
                if (assetPath.equals(videoAssetPath) && bmp != null && ivThumbnail != null) {
                    ivThumbnail.setImageBitmap(bmp);
                    // 仅在未播放时显示缩略图
                    if (!isReady || !isPlaying()) {
                        ivThumbnail.setVisibility(View.VISIBLE);
                    }
                }
            });
        }).start();
    }

    /** 通过 MediaMetadataRetriever 提取视频首帧. */
    @Nullable
    private Bitmap extractFirstFrame(String assetPath) {
        try {
            AssetFileDescriptor afd = getContext().getAssets().openFd(assetPath);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength());
            afd.close();
            Bitmap bmp = retriever.getFrameAtTime(0,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            retriever.release();
            return bmp;
        } catch (Exception e) {
            Log.w(TAG, "提取首帧失败: " + assetPath, e);
            return null;
        }
    }

    /** 隐藏缩略图（开始播放时）. */
    private void hideThumbnail() {
        if (ivThumbnail != null) {
            ivThumbnail.setVisibility(View.GONE);
        }
    }

    // ---------- MediaPlayer 管理 ----------

    private void prepareMediaPlayer(Surface surface) {
        releaseMediaPlayer();

        mediaPlayer = new MediaPlayer();
        try {
            AssetFileDescriptor afd = getContext().getAssets().openFd(videoAssetPath);
            mediaPlayer.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        } catch (IOException e) {
            Log.e(TAG, "打开视频文件失败: " + videoAssetPath, e);
            progressBarLoading.setVisibility(View.GONE);
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
            progressBarLoading.setVisibility(View.GONE);

            // 调整 TextureView 的宽高比以匹配视频
            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();
            if (videoWidth > 0 && videoHeight > 0) {
                adjustTextureViewSize(videoWidth, videoHeight);
            }

            // 设置进度条最大值
            int duration = mp.getDuration();
            seekBar.setMax(duration > 0 ? duration : 1000);

            if (shouldPlayOnReady) {
                shouldPlayOnReady = false;
                mp.start();
                updatePlayPauseButton(true);
                overlayPlayIcon.setVisibility(View.GONE);
                hideThumbnail();
                startProgressUpdates();
            } else {
                // 暂停状态：显示播放图标覆盖层，缩略图由异步线程负责
                overlayPlayIcon.setVisibility(View.VISIBLE);
                updatePlayPauseButton(false);
            }
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer 错误: what=" + what + ", extra=" + extra);
            progressBarLoading.setVisibility(View.GONE);
            isReady = false;
            return false;
        });

        mediaPlayer.prepareAsync();
    }

    private void adjustTextureViewSize(int videoWidth, int videoHeight) {
        int viewWidth = textureView.getWidth();
        if (viewWidth <= 0) return;
        int newHeight = (int) ((float) viewWidth / videoWidth * videoHeight);

        LayoutParams params = (LayoutParams) textureView.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.MATCH_PARENT, newHeight);
        } else {
            params.height = newHeight;
        }
        textureView.setLayoutParams(params);

        // overlay 和控制栏也要同步高度
        if (overlayPlayIcon != null) {
            LayoutParams op = (LayoutParams) overlayPlayIcon.getLayoutParams();
            if (op != null) {
                op.height = newHeight;
                overlayPlayIcon.setLayoutParams(op);
            }
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null || !isReady) return;
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                updatePlayPauseButton(false);
                overlayPlayIcon.setVisibility(View.VISIBLE);
                stopProgressUpdates();
                cancelHideControls(); // 暂停时不自动隐藏控制栏
            } else {
                mediaPlayer.start();
                updatePlayPauseButton(true);
                overlayPlayIcon.setVisibility(View.GONE);
                hideThumbnail();
                startProgressUpdates();
                scheduleHideControls();
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "togglePlayPause 失败", e);
        }
    }

    private void updatePlayPauseButton(boolean playing) {
        btnPlayPause.setImageResource(
                playing ? R.drawable.ic_video_pause : R.drawable.ic_video_play);
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

    // ---------- 进度条更新 ----------

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
                int max = seekBar.getMax();
                if (max > 0) {
                    seekBar.setProgress(current);
                }
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
        if (videoAssetPath != null && !videoAssetPath.isEmpty()) {
            prepareMediaPlayer(new Surface(surfaceTexture));
        }
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

    /** 在 Fragment/Activity 的 onPause 中调用，暂停播放以节省资源. */
    public void onPause() {
        pause();
    }

    /** 在 Fragment/Activity 的 onDestroyView/onDestroy 中调用，释放所有资源. */
    public void onDestroy() {
        stopProgressUpdates();
        cancelHideControls();
        release();
    }
}

