package com.aiethicsquest.presentation.fragment;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aiethicsquest.R;
import com.aiethicsquest.data.model.VideoWrongAnswer;
import com.aiethicsquest.databinding.ItemVideoWrongAnswerBinding;
import com.aiethicsquest.presentation.activity.VideoViewerActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 视频挑战错题记录 RecyclerView 适配器.
 *
 * <p>使用 {@link ListAdapter} + {@link DiffUtil} 自动处理列表更新动画。
 * 每个 item 展示双视频缩略图（点击可全屏播放）、题目编号、时间、错误原因。</p>
 */
public class VideoWrongAnswerAdapter
        extends ListAdapter<VideoWrongAnswer, VideoWrongAnswerAdapter.ViewHolder> {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    public VideoWrongAnswerAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVideoWrongAnswerBinding binding = ItemVideoWrongAnswerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    // ---------- ViewHolder ----------

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemVideoWrongAnswerBinding binding;

        ViewHolder(ItemVideoWrongAnswerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 将视频错题数据绑定到视图.
         */
        void bind(VideoWrongAnswer item) {
            Context context = binding.getRoot().getContext();

            // 题目编号标签
            binding.tvVideoIndex.setText(
                    context.getString(R.string.wrong_item_image_index, item.getVideoIndex()));

            // 答题时间
            binding.tvTime.setText(DATE_FORMAT.format(new Date(item.getAnsweredAt())));

            // 上方视频标签（AI/真实）
            boolean topIsAi = (item.getCorrectAnswer() == 0);
            binding.tvTopLabel.setText(topIsAi
                    ? context.getString(R.string.result_ai_label)
                    : context.getString(R.string.result_real_label));
            binding.tvTopLabel.setBackgroundResource(
                    topIsAi ? R.drawable.bg_label_ai : R.drawable.bg_label_real);

            // 下方视频标签（AI/真实）
            boolean bottomIsAi = (item.getCorrectAnswer() == 1);
            binding.tvBottomLabel.setText(bottomIsAi
                    ? context.getString(R.string.result_ai_label)
                    : context.getString(R.string.result_real_label));
            binding.tvBottomLabel.setBackgroundResource(
                    bottomIsAi ? R.drawable.bg_label_ai : R.drawable.bg_label_real);

            // 错误提示
            String errorHint = item.getUserChoice() == 0
                    ? context.getString(R.string.video_wrong_item_answer_top)
                    : context.getString(R.string.video_wrong_item_answer_bottom);
            binding.tvErrorHint.setText(errorHint);

            // 点击视频区域 → 全屏播放
            binding.touchTopVideo.setOnClickListener(v ->
                    openVideoViewer(context, item.getTopVideoPath()));
            binding.touchBottomVideo.setOnClickListener(v ->
                    openVideoViewer(context, item.getBottomVideoPath()));
        }

        private void openVideoViewer(Context ctx, String videoPath) {
            Intent intent = new Intent(ctx, VideoViewerActivity.class);
            intent.putExtra(VideoViewerActivity.EXTRA_VIDEO_PATH, videoPath);
            intent.putExtra(VideoViewerActivity.EXTRA_AUTO_PLAY, true);
            intent.putExtra(VideoViewerActivity.EXTRA_START_POSITION, 0);
            ctx.startActivity(intent);
        }
    }

    // ---------- DiffUtil ----------

    private static final DiffUtil.ItemCallback<VideoWrongAnswer> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<VideoWrongAnswer>() {
                @Override
                public boolean areItemsTheSame(@NonNull VideoWrongAnswer o,
                                               @NonNull VideoWrongAnswer n) {
                    return o.getId() == n.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull VideoWrongAnswer o,
                                                  @NonNull VideoWrongAnswer n) {
                    return o.getId() == n.getId() && o.getAnsweredAt() == n.getAnsweredAt();
                }
            };
}

