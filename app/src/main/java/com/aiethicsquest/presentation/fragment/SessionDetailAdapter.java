package com.aiethicsquest.presentation.fragment;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aiethicsquest.R;
import com.aiethicsquest.common.util.ImageLoader;
import com.aiethicsquest.databinding.ItemSessionDetailQuestionBinding;
import com.aiethicsquest.presentation.activity.ImageViewerActivity;

import java.util.List;

/**
 * 挑战历史详情页 - 每道题列表适配器.
 *
 * <p>展示该轮每道题的双图缩略图（可点击放大）、题号、答题结果标注。</p>
 */
public class SessionDetailAdapter
        extends RecyclerView.Adapter<SessionDetailAdapter.ViewHolder> {

    /**
     * 单道题目的数据模型.
     */
    public static class QuestionItem {
        /** 题目序号（从 1 开始）. */
        public final int index;
        /** 上图（左图）的 assets 路径. */
        public final String topPath;
        /** 下图（右图）的 assets 路径. */
        public final String bottomPath;
        /** AI 图所在侧（0=上图, 1=下图）. */
        public final int aiSide;
        /** 用户是否答对. */
        public final boolean isCorrect;

        public QuestionItem(int index, String topPath, String bottomPath,
                            int aiSide, boolean isCorrect) {
            this.index = index;
            this.topPath = topPath;
            this.bottomPath = bottomPath;
            this.aiSide = aiSide;
            this.isCorrect = isCorrect;
        }
    }

    private final List<QuestionItem> items;

    public SessionDetailAdapter(List<QuestionItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSessionDetailQuestionBinding binding = ItemSessionDetailQuestionBinding.inflate(
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

        private final ItemSessionDetailQuestionBinding binding;

        ViewHolder(ItemSessionDetailQuestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(QuestionItem item) {
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

            // 加载图片
            ImageLoader.loadFromAssets(ctx, binding.ivTop, item.topPath);
            ImageLoader.loadFromAssets(ctx, binding.ivBottom, item.bottomPath);

            // 图片标签（AI/真实）
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

            // 点击图片放大
            binding.ivTop.setOnClickListener(v -> openImageViewer(ctx, item.topPath));
            binding.ivBottom.setOnClickListener(v -> openImageViewer(ctx, item.bottomPath));
        }

        private void openImageViewer(Context ctx, String imagePath) {
            Intent intent = new Intent(ctx, ImageViewerActivity.class);
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_PATH, imagePath);
            ctx.startActivity(intent);
        }
    }
}

