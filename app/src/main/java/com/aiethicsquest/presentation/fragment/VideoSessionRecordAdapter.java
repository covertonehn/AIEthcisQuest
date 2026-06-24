package com.aiethicsquest.presentation.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aiethicsquest.R;
import com.aiethicsquest.data.model.VideoSessionRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 视频挑战历史记录列表 Adapter.
 *
 * <p>每条记录显示：完成时间、得分、正确率、本轮视频缩略图（横向滚动，点击播放图标）。
 * 支持点击回调，通过 {@link OnItemClickListener} 通知外部。</p>
 */
public class VideoSessionRecordAdapter
        extends ListAdapter<VideoSessionRecord, VideoSessionRecordAdapter.ViewHolder> {

    /** Item 点击回调接口. */
    public interface OnItemClickListener {
        /**
         * 当某条历史记录被点击时调用.
         *
         * @param record 被点击的记录
         */
        void onItemClick(VideoSessionRecord record);
    }

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    /** 缩略图尺寸（dp）. */
    private static final int THUMB_DP = 56;
    /** 缩略图间距（dp）. */
    private static final int THUMB_MARGIN_DP = 4;

    @Nullable
    private final OnItemClickListener clickListener;

    private static final DiffUtil.ItemCallback<VideoSessionRecord> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<VideoSessionRecord>() {
                @Override
                public boolean areItemsTheSame(@NonNull VideoSessionRecord o,
                                               @NonNull VideoSessionRecord n) {
                    return o.getId() == n.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull VideoSessionRecord o,
                                                  @NonNull VideoSessionRecord n) {
                    return o.getFinishedAt() == n.getFinishedAt()
                            && o.getCorrectCount() == n.getCorrectCount();
                }
            };

    public VideoSessionRecordAdapter() {
        this(null);
    }

    public VideoSessionRecordAdapter(@Nullable OnItemClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 复用与识图挑战相同的 item 布局
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoSessionRecord record = getItem(position);
        holder.bind(record);
        if (clickListener != null) {
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
            holder.itemView.setOnClickListener(v -> clickListener.onItemClick(record));
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTime;
        private final TextView tvScore;
        private final TextView tvAccuracy;
        private final LinearLayout llThumbnails;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_session_time);
            tvScore = itemView.findViewById(R.id.tv_session_score);
            tvAccuracy = itemView.findViewById(R.id.tv_accuracy);
            llThumbnails = itemView.findViewById(R.id.ll_thumbnails);
        }

        void bind(VideoSessionRecord record) {
            Context ctx = itemView.getContext();

            // 时间
            tvTime.setText(DATE_FORMAT.format(new Date(record.getFinishedAt())));

            // 得分
            tvScore.setText(ctx.getString(R.string.history_score,
                    record.getCorrectCount(), record.getTotalCount()));

            // 正确率
            int pct = record.getTotalCount() > 0
                    ? Math.round(record.getAccuracy() * 100) : 0;
            tvAccuracy.setText(pct + "%");

            // 缩略图区域：每题展示一个视频占位（视频播放图标）
            llThumbnails.removeAllViews();
            if (record.getVideoPaths() != null && !record.getVideoPaths().isEmpty()) {
                String[] groups = record.getVideoPaths().split("\\|");
                float density = ctx.getResources().getDisplayMetrics().density;
                int sizePx = (int) (THUMB_DP * density);
                int marginPx = (int) (THUMB_MARGIN_DP * density);

                for (int i = 0; i < groups.length; i++) {
                    // 视频缩略图：黑色背景 + 播放图标
                    FrameLayout frame = new FrameLayout(ctx);
                    LinearLayout.LayoutParams lp =
                            new LinearLayout.LayoutParams(sizePx, sizePx);
                    lp.setMarginEnd(marginPx);
                    frame.setLayoutParams(lp);
                    frame.setBackgroundColor(0xFF000000);

                    android.widget.ImageView playIcon = new android.widget.ImageView(ctx);
                    FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(
                            (int) (28 * density), (int) (28 * density));
                    iconLp.gravity = android.view.Gravity.CENTER;
                    playIcon.setLayoutParams(iconLp);
                    playIcon.setImageResource(R.drawable.ic_video_play_circle);
                    frame.addView(playIcon);

                    llThumbnails.addView(frame);
                }
            }
        }
    }
}

