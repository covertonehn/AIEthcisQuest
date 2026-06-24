package com.aiethicsquest.presentation.fragment;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aiethicsquest.R;
import com.aiethicsquest.databinding.ItemVideoSessionDetailBinding;
import com.aiethicsquest.presentation.activity.VideoViewerActivity;

import java.util.List;

/**
 * 视频挑战历史详情页 - 每道题列表适配器.
 *
 * <p>展示该轮每道题的双视频（点击全屏播放）、题号、答题结果标注。</p>
 */
public class VideoSessionDetailAdapter
        extends RecyclerView.Adapter<VideoSessionDetailAdapter.ViewHolder> {

    /**
     * 单道视频题目的数据模型.
     */
    public static class VideoQuestionItem {
        /** 题目序号（从 1 开始）. */
        public final int index;
        /** 上方视频的 assets 路径. */
        public final String topPath;
        /** 下方视频的 assets 路径. */
        public final String bottomPath;
        /** AI 视频所在侧（0=上方, 1=下方）. */
        public final int aiSide;
        /** 用户是否答对. */
        public final boolean isCorrect;

        public VideoQuestionItem(int index, String topPath, String bottomPath,
                                 int aiSide, boolean isCorrect) {
            this.index = index;
            this.topPath = topPath;
            this.bottomPath = bottomPath;
            this.aiSide = aiSide;
            this.isCorrect = isCorrect;
        }
    }

    private final List<VideoQuestionItem> items;

    public VideoSessionDetailAdapter(List<VideoQuestionItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVideoSessionDetailBinding binding = ItemVideoSessionDetailBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ---------- ViewHolder ----------

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemVideoSessionDetailBinding binding;

        ViewHolder(ItemVideoSessionDetailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VideoQuestionItem item) {
            Context ctx = binding.getRoot().getContext();

            // 题号
            binding.tvQuestionIndex.setText(
                    ctx.getString(R.string.wrong_item_image_index, item.index));

            // 答对/答错标签
            if (item.isCorrect) {
                binding.tvResultLabel.setText(ctx.getString(R.string.result_correct));
                binding.tvResultLabel.setTextColor(ctx.getColor(R.color.color_correct));
            } else {
                binding.tvResultLabel.setText(ctx.getString(R.string.result_wrong));
                binding.tvResultLabel.setTextColor(ctx.getColor(R.color.color_wrong));
            }

            // 视频标签（AI/真实）
            boolean topIsAi = (item.aiSide == 0);
            binding.tvTopLabel.setText(topIsAi
                    ? ctx.getString(R.string.result_ai_label)
                    : ctx.getString(R.string.result_real_label));
            binding.tvTopLabel.setBackgroundResource(
                    topIsAi ? R.drawable.bg_label_ai : R.drawable.bg_label_real);

            boolean bottomIsAi = (item.aiSide == 1);
            binding.tvBottomLabel.setText(bottomIsAi
                    ? ctx.getString(R.string.result_ai_label)
                    : ctx.getString(R.string.result_real_label));
            binding.tvBottomLabel.setBackgroundResource(
                    bottomIsAi ? R.drawable.bg_label_ai : R.drawable.bg_label_real);

            // 答题说明
            String hint;
            if (item.isCorrect) {
                hint = ctx.getString(R.string.detail_answer_correct_hint,
                        item.aiSide == 0
                                ? ctx.getString(R.string.label_top_image)
                                : ctx.getString(R.string.label_bottom_image));
            } else {
                hint = ctx.getString(R.string.detail_answer_wrong_hint,
                        item.aiSide == 0
                                ? ctx.getString(R.string.label_top_image)
                                : ctx.getString(R.string.label_bottom_image));
            }
            binding.tvAnswerHint.setText(hint);
            binding.tvAnswerHint.setTextColor(
                    item.isCorrect
                            ? ctx.getColor(R.color.color_correct)
                            : ctx.getColor(R.color.color_wrong));

            // 点击上方视频 → 全屏播放
            binding.touchTop.setOnClickListener(v -> openVideoViewer(ctx, item.topPath));
            // 点击下方视频 → 全屏播放
            binding.touchBottom.setOnClickListener(v -> openVideoViewer(ctx, item.bottomPath));
        }

        private void openVideoViewer(Context ctx, String videoPath) {
            Intent intent = new Intent(ctx, VideoViewerActivity.class);
            intent.putExtra(VideoViewerActivity.EXTRA_VIDEO_PATH, videoPath);
            intent.putExtra(VideoViewerActivity.EXTRA_AUTO_PLAY, true);
            intent.putExtra(VideoViewerActivity.EXTRA_START_POSITION, 0);
            ctx.startActivity(intent);
        }
    }
}

