package com.aiethicsquest.presentation.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aiethicsquest.R;
import com.aiethicsquest.common.util.ImageLoader;
import com.aiethicsquest.data.model.SessionRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 挑战历史记录列表 Adapter.
 *
 * <p>每条记录显示：完成时间、得分、正确率、本轮缩略图（横向滚动）。
 * 支持点击回调，通过 {@link OnItemClickListener} 通知外部。</p>
 */
public class SessionRecordAdapter
        extends ListAdapter<SessionRecord, SessionRecordAdapter.ViewHolder> {

    /** Item 点击回调接口. */
    public interface OnItemClickListener {
        /**
         * 当某条历史记录被点击时调用.
         *
         * @param record 被点击的记录
         */
        void onItemClick(SessionRecord record);
    }

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    /** 缩略图尺寸（dp，代码中转换为 px）. */
    private static final int THUMB_DP = 56;
    /** 缩略图间距（dp）. */
    private static final int THUMB_MARGIN_DP = 4;

    @Nullable
    private final OnItemClickListener clickListener;

    private static final DiffUtil.ItemCallback<SessionRecord> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SessionRecord>() {
                @Override
                public boolean areItemsTheSame(@NonNull SessionRecord oldItem,
                                               @NonNull SessionRecord newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull SessionRecord oldItem,
                                                  @NonNull SessionRecord newItem) {
                    return oldItem.getFinishedAt() == newItem.getFinishedAt()
                            && oldItem.getCorrectCount() == newItem.getCorrectCount();
                }
            };

    /**
     * 创建 Adapter，无点击回调（仅展示，不可点击进入详情）.
     */
    public SessionRecordAdapter() {
        this(null);
    }

    /**
     * 创建 Adapter，带点击回调.
     *
     * @param clickListener item 点击回调，为 null 则不处理点击
     */
    public SessionRecordAdapter(@Nullable OnItemClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SessionRecord record = getItem(position);
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

        void bind(SessionRecord record) {
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

            // 缩略图（每题取上图，横向排列）
            llThumbnails.removeAllViews();
            if (record.getImagePaths() != null && !record.getImagePaths().isEmpty()) {
                String[] groups = record.getImagePaths().split("\\|");
                float density = ctx.getResources().getDisplayMetrics().density;
                int sizePx = (int) (THUMB_DP * density);
                int marginPx = (int) (THUMB_MARGIN_DP * density);

                for (String group : groups) {
                    // group 格式：上图路径,下图路径；取上图
                    String topPath = group.contains(",") ? group.split(",")[0] : group;

                    ImageView imageView = new ImageView(ctx);
                    LinearLayout.LayoutParams lp =
                            new LinearLayout.LayoutParams(sizePx, sizePx);
                    lp.setMarginEnd(marginPx);
                    imageView.setLayoutParams(lp);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setBackgroundColor(
                            ctx.getColor(R.color.color_image_placeholder));

                    ImageLoader.loadFromAssets(ctx, imageView, topPath);
                    llThumbnails.addView(imageView);
                }
            }
        }
    }
}

