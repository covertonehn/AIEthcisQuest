package com.aiethicsquest.presentation.activity;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * 支持双指缩放和单指拖拽的 ImageView.
 *
 * <p>功能：</p>
 * <ul>
 *     <li>双指捏合/展开：缩放图片（0.5x ~ 5x）</li>
 *     <li>单指拖拽：平移图片（仅在放大后生效）</li>
 *     <li>双击：在初始缩放和 2x 之间切换</li>
 * </ul>
 */
public class ZoomableImageView extends AppCompatImageView {

    /** 最小缩放倍数. */
    private static final float MIN_SCALE = 1.0f;
    /** 最大缩放倍数. */
    private static final float MAX_SCALE = 5.0f;
    /** 双击放大倍数. */
    private static final float DOUBLE_TAP_SCALE = 2.5f;

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    /** 上一次触摸点（用于拖拽计算）. */
    private float lastTouchX;
    private float lastTouchY;

    /** 当前是否处于多指触摸状态. */
    private boolean isScaling = false;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);

        // 缩放手势检测
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float scaleFactor = detector.getScaleFactor();
                        float currentScale = getCurrentScale();
                        float newScale = currentScale * scaleFactor;

                        // 限制缩放范围
                        if (newScale < MIN_SCALE) {
                            scaleFactor = MIN_SCALE / currentScale;
                        } else if (newScale > MAX_SCALE) {
                            scaleFactor = MAX_SCALE / currentScale;
                        }

                        matrix.postScale(scaleFactor, scaleFactor,
                                detector.getFocusX(), detector.getFocusY());
                        constrainMatrix();
                        setImageMatrix(matrix);
                        return true;
                    }
                });

        // 双击手势检测
        gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        float currentScale = getCurrentScale();
                        float targetScale;
                        if (currentScale < DOUBLE_TAP_SCALE - 0.1f) {
                            targetScale = DOUBLE_TAP_SCALE;
                        } else {
                            targetScale = 1.0f;
                        }
                        float scaleFactor = targetScale / currentScale;
                        matrix.postScale(scaleFactor, scaleFactor, e.getX(), e.getY());
                        constrainMatrix();
                        setImageMatrix(matrix);
                        return true;
                    }
                });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && getDrawable() != null) {
            resetMatrix();
        }
    }

    /**
     * 将图片以 fitCenter 方式居中显示，并初始化 Matrix.
     */
    private void resetMatrix() {
        if (getDrawable() == null) return;
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int drawableWidth = getDrawable().getIntrinsicWidth();
        int drawableHeight = getDrawable().getIntrinsicHeight();

        if (drawableWidth <= 0 || drawableHeight <= 0) return;

        float scaleX = (float) viewWidth / drawableWidth;
        float scaleY = (float) viewHeight / drawableHeight;
        float scale = Math.min(scaleX, scaleY);

        float dx = (viewWidth - drawableWidth * scale) / 2f;
        float dy = (viewHeight - drawableHeight * scale) / 2f;

        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isScaling = false;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                isScaling = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!isScaling && event.getPointerCount() == 1) {
                    // 单指拖拽
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    matrix.postTranslate(dx, dy);
                    constrainMatrix();
                    setImageMatrix(matrix);
                }
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                isScaling = false;
                if (event.getPointerCount() > 1) {
                    // 抬起一根手指后，更新参考点为剩余手指位置
                    int remaining = event.getActionIndex() == 0 ? 1 : 0;
                    lastTouchX = event.getX(remaining);
                    lastTouchY = event.getY(remaining);
                }
                break;

            default:
                break;
        }

        return true;
    }

    /**
     * 约束矩阵平移，防止图片超出边界太多.
     */
    private void constrainMatrix() {
        if (getDrawable() == null) return;

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        matrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scale = matrixValues[Matrix.MSCALE_X];

        int drawableWidth = getDrawable().getIntrinsicWidth();
        int drawableHeight = getDrawable().getIntrinsicHeight();

        float scaledWidth = drawableWidth * scale;
        float scaledHeight = drawableHeight * scale;

        float dx = 0;
        float dy = 0;

        if (scaledWidth <= viewWidth) {
            // 图片宽度小于等于视图宽度，居中
            dx = (viewWidth - scaledWidth) / 2f - transX;
        } else {
            // 图片宽度大于视图宽度，限制边界
            if (transX > 0) {
                dx = -transX;
            } else if (transX + scaledWidth < viewWidth) {
                dx = viewWidth - (transX + scaledWidth);
            }
        }

        if (scaledHeight <= viewHeight) {
            // 图片高度小于等于视图高度，居中
            dy = (viewHeight - scaledHeight) / 2f - transY;
        } else {
            // 图片高度大于视图高度，限制边界
            if (transY > 0) {
                dy = -transY;
            } else if (transY + scaledHeight < viewHeight) {
                dy = viewHeight - (transY + scaledHeight);
            }
        }

        matrix.postTranslate(dx, dy);
    }

    /**
     * 获取当前缩放倍数.
     */
    private float getCurrentScale() {
        matrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }
}

