package com.aiethicsquest.presentation.activity;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 海胆选择控件.
 *
 * <p>用于答题时选择"上方"或"下方"，替代直接点击图片/视频区域的交互方式。</p>
 *
 * <p>外观：4 根斜向刺（左上/右上/左下/右下各一根）划分上方/下方/取消三个区域，
 * 不再有左右方向的额外刺。</p>
 *
 * <p>交互逻辑：</p>
 * <ul>
 *     <li>只有触点落在中心圆形按钮内才响应 ACTION_DOWN（防误触）</li>
 *     <li>按下时出现射线，划分上方/下方/取消三个区域</li>
 *     <li>滑向上方 → 按钮亮黄色，上方扇形黄色底色，文字"↑"</li>
 *     <li>滑向下方 → 按钮亮黄色，下方扇形黄色底色，文字"↓"</li>
 *     <li>在取消区（左右/中心） → 按钮灰色，左右取消扇形灰色底色（与区域匹配），文字"取消"</li>
 *     <li>松开手指在上方/下方区域 → 触发选择回调</li>
 *     <li>松开手指在取消区域 → 取消，不触发</li>
 *     <li>短暂点击（未滑出按钮范围）→ 取消，不触发</li>
 * </ul>
 */
public class SeaUrchinSelectView extends View {

    /** 选择结果回调. */
    public interface OnSelectListener {
        /**
         * 用户完成选择时回调.
         *
         * @param side 0=上方, 1=下方
         */
        void onSelect(int side);

        /**
         * 用户手指移动到某区域时回调，用于高亮对应内容.
         *
         * @param hoveredSide 0=上方, 1=下方, -1=无/取消区域
         */
        void onHover(int hoveredSide);
    }

    // ---------- 常量 ----------

    /** 上方/下方区域的半角（度），以正上方为0°，顺时针. */
    private static final float ZONE_HALF_ANGLE_DEG = 55f;

    /** 射线显示的长度（dp）. */
    private static final float SPIKE_LENGTH_DP = 56f;

    /** 按钮圆圈半径（dp）. 略微放大. */
    private static final float BUTTON_RADIUS_DP = 34f;

    /** 手指离开按钮本体的最小距离才算开始滑动选择（dp）. */
    private static final float MIN_SLIDE_DIST_DP = 20f;

    /** 射线宽度（dp）. */
    private static final float SPIKE_STROKE_DP = 3f;

    /** 文字大小（dp）. */
    private static final float TEXT_SIZE_DP = 11f;

    // ---------- 按钮颜色 ----------

    /** 未按下：紫色. */
    private static final int BTN_COLOR_IDLE    = 0xCC7B52E8;
    /** 按下在取消区：灰色. */
    private static final int BTN_COLOR_CANCEL  = 0xCC666666;
    /** 按下在选择区：亮黄色. */
    private static final int BTN_COLOR_SELECT  = 0xFFFFD600;
    /** 按钮边框（默认）. */
    private static final int BTN_BORDER_IDLE   = 0x99FFFFFF;
    /** 按钮边框（选中）. */
    private static final int BTN_BORDER_SELECT = 0xFFFFFFFF;

    /** 文字颜色（默认/取消）. */
    private static final int TEXT_COLOR_NORMAL = 0xFFFFFFFF;
    /** 文字颜色（选中黄色按钮上）. */
    private static final int TEXT_COLOR_SELECT = 0xFF333333;

    // ---------- 射线颜色 ----------

    /** 取消状态引导刺：亮白色. */
    private static final int SPIKE_SOLID_CANCEL = 0xDDFFFFFF;
    private static final int SPIKE_FADE_CANCEL  = 0x00FFFFFF;
    /** 选择状态引导刺：黄色. */
    private static final int SPIKE_SOLID_SELECT = 0xFFFFD600;
    private static final int SPIKE_FADE_SELECT  = 0x00FFD600;

    // ---------- 区域底色 ----------

    /** 选择区扇形底色（黄色半透明）. */
    private static final int ZONE_SELECT_CENTER = 0x55FFD600;
    private static final int ZONE_SELECT_EDGE   = 0x00FFD600;
    /** 取消区扇形底色（灰色半透明）. */
    private static final int ZONE_CANCEL_CENTER = 0x44888888;
    private static final int ZONE_CANCEL_EDGE   = 0x00888888;

    // ---------- 字段 ----------

    private float density;
    private float spikeLength;
    private float buttonRadius;
    private float minSlideDist;

    private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spikePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zonePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** 当前是否按下（控制射线显示）. */
    private boolean isPressedDown = false;
    /** 当前预选区域：0=上, 1=下, -1=无. */
    private int hoveredSide = -1;
    /** 是否已滑出按钮范围（开始有效选择）. */
    private boolean hasSlid = false;

    /** 射线出现动画进度 0~1. */
    private float spikeAnimProgress = 0f;
    private ValueAnimator spikeAnimator;

    private OnSelectListener selectListener;

    // ---------- 构造 ----------

    public SeaUrchinSelectView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SeaUrchinSelectView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SeaUrchinSelectView(@NonNull Context context, @Nullable AttributeSet attrs,
                               int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density      = context.getResources().getDisplayMetrics().density;
        spikeLength  = SPIKE_LENGTH_DP  * density;
        buttonRadius = BUTTON_RADIUS_DP * density;
        minSlideDist = MIN_SLIDE_DIST_DP * density;

        textPaint.setTextSize(TEXT_SIZE_DP * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        spikePaint.setStrokeCap(Paint.Cap.ROUND);
        spikePaint.setStrokeWidth(SPIKE_STROKE_DP * density);
        spikePaint.setStyle(Paint.Style.STROKE);

        zonePaint.setStyle(Paint.Style.FILL);
    }

    // ---------- 公开接口 ----------

    /** 设置选择结果回调. */
    public void setOnSelectListener(OnSelectListener listener) {
        this.selectListener = listener;
    }

    // ---------- 触摸处理 ----------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        float dx = event.getX() - cx;
        float dy = event.getY() - cy;
        float distFromCenter = (float) Math.sqrt(dx * dx + dy * dy);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // ★ 只有触点在圆形按钮内才响应，防止误触
                if (distFromCenter > buttonRadius) {
                    return false;
                }
                isPressedDown = true;
                hasSlid  = false;
                hoveredSide = -1;
                startSpikeAnimation(true);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isPressedDown) return false;
                if (distFromCenter > minSlideDist) {
                    hasSlid = true;
                }
                int newHovered = computeZone(dx, dy);
                if (newHovered != hoveredSide) {
                    hoveredSide = newHovered;
                    if (selectListener != null) {
                        selectListener.onHover(hoveredSide);
                    }
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isPressedDown) return false;
                isPressedDown = false;
                startSpikeAnimation(false);
                int selected = hasSlid ? hoveredSide : -1;
                hoveredSide = -1;
                if (selectListener != null) {
                    selectListener.onHover(-1);
                    if (selected == 0 || selected == 1) {
                        selectListener.onSelect(selected);
                    }
                }
                invalidate();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    /**
     * 根据手指相对中心的偏移，计算所在区域.
     *
     * @return 0=上方, 1=下方, -1=取消区域（左/右/中心）
     */
    private int computeZone(float dx, float dy) {
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < minSlideDist) {
            return -1; // 还在中心，视为取消
        }
        // atan2(dx, -dy)：正上方=0°，顺时针增大，范围 -180~180
        double angleFromTop = Math.toDegrees(Math.atan2(dx, -dy));
        double absAngle = Math.abs(angleFromTop);
        if (absAngle <= ZONE_HALF_ANGLE_DEG) {
            return 0; // 上方
        } else if (absAngle >= (180 - ZONE_HALF_ANGLE_DEG)) {
            return 1; // 下方
        } else {
            return -1; // 左右取消区
        }
    }

    // ---------- 绘制 ----------

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;

        if (isPressedDown && spikeAnimProgress > 0) {
            // 1. 区域底色（在射线和按钮之下）
            drawZoneBackground(canvas, cx, cy);
            // 2. 射线
            drawSpikes(canvas, cx, cy);
        }

        // 3. 按钮填充
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(getButtonFillColor());
        canvas.drawCircle(cx, cy, buttonRadius, buttonPaint);

        // 4. 按钮边框
        buttonPaint.setStyle(Paint.Style.STROKE);
        buttonPaint.setStrokeWidth(2 * density);
        boolean isSelecting = isPressedDown && (hoveredSide == 0 || hoveredSide == 1);
        buttonPaint.setColor(isSelecting ? BTN_BORDER_SELECT : BTN_BORDER_IDLE);
        canvas.drawCircle(cx, cy, buttonRadius, buttonPaint);

        // 5. 中心文字（两行/单行）
        drawButtonText(canvas, cx, cy);
    }

    /** 根据当前状态返回按钮填充颜色. */
    private int getButtonFillColor() {
        if (!isPressedDown)                          return BTN_COLOR_IDLE;
        if (hoveredSide == 0 || hoveredSide == 1)   return BTN_COLOR_SELECT;
        return BTN_COLOR_CANCEL;
    }

    /**
     * 绘制按钮中心文字.
     *
     * <ul>
     *   <li>未按下或按下取消区：两行文字"滑动\n选择"（白色）</li>
     *   <li>按下取消区（hasSlid）：单行"取消"（白色）</li>
     *   <li>按下选择区：单行箭头"↑"或"↓"（深色）</li>
     * </ul>
     */
    private void drawButtonText(Canvas canvas, float cx, float cy) {
        if (isPressedDown && (hoveredSide == 0 || hoveredSide == 1)) {
            // 选择中：显示方向箭头，深色
            textPaint.setColor(TEXT_COLOR_SELECT);
            textPaint.setTextSize(18 * density);
            String arrow = hoveredSide == 0 ? "↑" : "↓";
            float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
            canvas.drawText(arrow, cx, textY, textPaint);
            textPaint.setTextSize(TEXT_SIZE_DP * density); // 恢复
        } else if (isPressedDown && hasSlid) {
            // 已滑动且在取消区：显示"取消"
            textPaint.setColor(TEXT_COLOR_NORMAL);
            textPaint.setTextSize(TEXT_SIZE_DP * density);
            float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
            canvas.drawText("取消", cx, textY, textPaint);
        } else {
            // 默认：两行"滑动\n选择"
            textPaint.setColor(TEXT_COLOR_NORMAL);
            textPaint.setTextSize(TEXT_SIZE_DP * density);
            float lineHeight = textPaint.descent() - textPaint.ascent();
            float totalHeight = lineHeight * 2 + 2 * density; // 两行+行间距
            float firstY  = cy - totalHeight / 2f - textPaint.ascent();
            float secondY = firstY + lineHeight + 2 * density;
            canvas.drawText("滑动", cx, firstY,  textPaint);
            canvas.drawText("选择", cx, secondY, textPaint);
        }
    }

    /**
     * 绘制区域底色（扇形渐变）.
     *
     * <p>选择区（上/下）：对应方向的扇形黄色径向渐变。
     * 取消区：左右两侧各一个扇形的灰色渐变，角度严格对应左右取消范围（{@link #ZONE_HALF_ANGLE_DEG} ~ 180°-ZONE_HALF_ANGLE_DEG），
     * 不会超出区域边界。</p>
     */
    private void drawZoneBackground(Canvas canvas, float cx, float cy) {
        float radius = (buttonRadius + spikeLength) * spikeAnimProgress * 1.1f;
        if (radius <= 0) return;

        if (hoveredSide == 0 || hoveredSide == 1) {
            // 选择区：上方或下方扇形黄色底色
            RadialGradient grad = new RadialGradient(
                    cx, cy, radius,
                    ZONE_SELECT_CENTER, ZONE_SELECT_EDGE,
                    Shader.TileMode.CLAMP);
            zonePaint.setShader(grad);

            Path path = new Path();
            path.moveTo(cx, cy);
            // hoveredSide==0 上方：Canvas 角度 = -(90 + half) 到 -(90 - half)
            // hoveredSide==1 下方：Canvas 角度 = (90 - half) 到 (90 + half)
            float startAngle = hoveredSide == 0
                    ? -(90 + ZONE_HALF_ANGLE_DEG)
                    :  (90 - ZONE_HALF_ANGLE_DEG);
            float sweepAngle = ZONE_HALF_ANGLE_DEG * 2;
            path.arcTo(cx - radius, cy - radius, cx + radius, cy + radius,
                    startAngle, sweepAngle, false);
            path.close();
            canvas.drawPath(path, zonePaint);
            zonePaint.setShader(null);
        } else if (isPressedDown) {
            // 取消区：仅显示左右两侧的灰色底色扇形
            // 左侧取消区（Canvas 标准角度，正右方=0°，顺时针）：
            //   左侧对应 ZONE_HALF_ANGLE_DEG 到 180-ZONE_HALF_ANGLE_DEG（上半）
            //   + -(180-ZONE_HALF_ANGLE_DEG) 到 -ZONE_HALF_ANGLE_DEG（下半）
            //   简化：左侧区域合并为 startAngle=90+ZONE_HALF_ANGLE_DEG,
            //          sweep=180-2*ZONE_HALF_ANGLE_DEG... 不好。
            //
            // 重新思考：以"正上=0°,顺时针"约定的角度来理解区域：
            //   上方区域：-55° ~ +55° （ZONE_HALF_ANGLE_DEG=55）
            //   下方区域：125° ~ 235°（即 ±(180-55) ~ ±180）
            //   左侧取消：55° ~ 125°
            //   右侧取消：-125° ~ -55°（或等价 235° ~ 305°）
            //
            // Canvas arcTo 使用标准数学角（正右=0°，顺时针）：
            //   约定角(顺时针,正上=0°) → Canvas角 = 约定角 - 90
            //   左侧取消约定角范围：55° ~ 125°  → Canvas: -35° ~ 35°  （顺时针从-35画70°）
            //   右侧取消约定角范围：-125° ~ -55° → Canvas: -215° ~ -145°，等价 145° ~ 215°（顺时针从145画70°）

            float sweepCancel = 180 - 2 * ZONE_HALF_ANGLE_DEG; // = 70°

            // 取消区在左右两侧：
            //   约定角（正上=0°，顺时针）右侧取消区：55° ~ 125°
            //   Canvas 角（正右=0°，顺时针）= 约定角 - 90 → 右侧：-35° ~ 35°，startAngle=-35°
            //   同理左侧取消区：约定角 235° ~ 305° → Canvas: 145° ~ 215°，startAngle=145°
            float cancelRightStart = -(90f - ZONE_HALF_ANGLE_DEG);  // = -35°
            float cancelLeftStart  =  90f + ZONE_HALF_ANGLE_DEG;    // = 145°

            // 右侧取消扇形（Canvas: -35° sweep 70°，即 -35° ~ 35°，对应正右方）
            RadialGradient gradRight = new RadialGradient(
                    cx, cy, radius,
                    ZONE_CANCEL_CENTER, ZONE_CANCEL_EDGE,
                    Shader.TileMode.CLAMP);
            zonePaint.setShader(gradRight);
            Path rightPath = new Path();
            rightPath.moveTo(cx, cy);
            rightPath.arcTo(cx - radius, cy - radius, cx + radius, cy + radius,
                    cancelRightStart, sweepCancel, false);
            rightPath.close();
            canvas.drawPath(rightPath, zonePaint);
            zonePaint.setShader(null);

            // 左侧取消扇形（Canvas: 145° sweep 70°，即 145° ~ 215°，对应正左方）
            RadialGradient gradLeft = new RadialGradient(
                    cx, cy, radius,
                    ZONE_CANCEL_CENTER, ZONE_CANCEL_EDGE,
                    Shader.TileMode.CLAMP);
            zonePaint.setShader(gradLeft);
            Path leftPath = new Path();
            leftPath.moveTo(cx, cy);
            leftPath.arcTo(cx - radius, cy - radius, cx + radius, cy + radius,
                    cancelLeftStart, sweepCancel, false);
            leftPath.close();
            canvas.drawPath(leftPath, zonePaint);
            zonePaint.setShader(null);
        }
    }

    /**
     * 绘制海胆射线（4 根斜向边界刺，不再有左右额外刺）.
     *
     * <p>4 根刺对应上/下区域的两侧边界：
     * 左上边界(−55°)、右上边界(+55°)、左下边界(125°)、右下边界(235°)。</p>
     */
    private void drawSpikes(Canvas canvas, float cx, float cy) {
        // 4根区域边界刺（以"正上=0°，顺时针"为约定）
        float[] spikeAngles = {
                -ZONE_HALF_ANGLE_DEG,       // 左上边界
                 ZONE_HALF_ANGLE_DEG,        // 右上边界
                 180 - ZONE_HALF_ANGLE_DEG,  // 左下边界
                 180 + ZONE_HALF_ANGLE_DEG   // 右下边界
        };

        float startR = buttonRadius * 1.06f;
        float endR   = startR + spikeLength * spikeAnimProgress;

        // 选择区时用黄色刺，取消区时用白色刺
        int solidColor = (hoveredSide == 0 || hoveredSide == 1)
                ? SPIKE_SOLID_SELECT : SPIKE_SOLID_CANCEL;
        int fadeColor  = (hoveredSide == 0 || hoveredSide == 1)
                ? SPIKE_FADE_SELECT  : SPIKE_FADE_CANCEL;

        for (float angleDeg : spikeAngles) {
            // 约定角 → Canvas 弧度：canvasRad = (angleDeg - 90) in radians
            float angleRad = (float) Math.toRadians(angleDeg - 90);
            float sx = cx + startR * (float) Math.cos(angleRad);
            float sy = cy + startR * (float) Math.sin(angleRad);
            float ex = cx + endR   * (float) Math.cos(angleRad);
            float ey = cy + endR   * (float) Math.sin(angleRad);

            LinearGradient grad = new LinearGradient(
                    sx, sy, ex, ey, solidColor, fadeColor, Shader.TileMode.CLAMP);
            spikePaint.setShader(grad);
            canvas.drawLine(sx, sy, ex, ey, spikePaint);
        }
        spikePaint.setShader(null);
    }

    /** 启动射线出现/消失动画. */
    private void startSpikeAnimation(boolean show) {
        if (spikeAnimator != null) {
            spikeAnimator.cancel();
        }
        float from = spikeAnimProgress;
        float to   = show ? 1f : 0f;
        spikeAnimator = ValueAnimator.ofFloat(from, to);
        spikeAnimator.setDuration(show ? 180 : 120);
        spikeAnimator.setInterpolator(new DecelerateInterpolator());
        spikeAnimator.addUpdateListener(anim -> {
            spikeAnimProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
        spikeAnimator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) ((buttonRadius + spikeLength + 8 * density) * 2);
        setMeasuredDimension(
                resolveSize(size, widthMeasureSpec),
                resolveSize(size, heightMeasureSpec));
    }
}

