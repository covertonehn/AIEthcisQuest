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
import com.aiethicsquest.common.util.ImageLoader;
import com.aiethicsquest.data.model.WrongAnswer;
import com.aiethicsquest.databinding.ItemWrongAnswerBinding;
import com.aiethicsquest.presentation.activity.ImageViewerActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 错题记录 RecyclerView 适配器.
 *
 * <p>使用 {@link ListAdapter} + {@link DiffUtil} 自动处理列表更新动画。
 * 每个 item 展示双图缩略图（从 assets 加载）、题目编号、时间、错误原因。</p>
 */
public class WrongAnswerAdapter extends ListAdapter<WrongAnswer, WrongAnswerAdapter.ViewHolder> {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    public WrongAnswerAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWrongAnswerBinding binding = ItemWrongAnswerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    // ---------- ViewHolder ----------

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemWrongAnswerBinding binding;

        ViewHolder(ItemWrongAnswerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 将错题数据绑定到视图.
         */
        void bind(WrongAnswer item) {
            Context context = binding.getRoot().getContext();

            // 题目编号标签
            binding.tvImageIndex.setText(
                    context.getString(R.string.wrong_item_image_index, item.getImageIndex()));

            // 答题时间
            binding.tvTime.setText(DATE_FORMAT.format(new Date(item.getAnsweredAt())));

            // 加载双图缩略图（从 assets），点击可放大
            ImageLoader.loadFromAssets(context, binding.ivLeft, item.getLeftImagePath());
            ImageLoader.loadFromAssets(context, binding.ivRight, item.getRightImagePath());

            // 点击放大
            binding.ivLeft.setOnClickListener(v ->
                    openImageViewer(context, item.getLeftImagePath()));
            binding.ivRight.setOnClickListener(v ->
                    openImageViewer(context, item.getRightImagePath()));

            // 左图标签（AI/真实）
            boolean leftIsAi = (item.getCorrectAnswer() == 0);
            binding.tvLeftLabel.setText(leftIsAi
                    ? context.getString(R.string.result_ai_label)
                    : context.getString(R.string.result_real_label));
            binding.tvLeftLabel.setBackgroundResource(
                    leftIsAi ? R.drawable.bg_label_ai : R.drawable.bg_label_real);

            // 右图标签（AI/真实）
            boolean rightIsAi = (item.getCorrectAnswer() == 1);
            binding.tvRightLabel.setText(rightIsAi
                    ? context.getString(R.string.result_ai_label)
                    : context.getString(R.string.result_real_label));
            binding.tvRightLabel.setBackgroundResource(
                    rightIsAi ? R.drawable.bg_label_ai : R.drawable.bg_label_real);

            // 错误提示
            String errorHint = item.getUserChoice() == 0
                    ? context.getString(R.string.wrong_item_answer_left)
                    : context.getString(R.string.wrong_item_answer_right);
            binding.tvErrorHint.setText(errorHint);
        }

        private void openImageViewer(Context ctx, String imagePath) {
            Intent intent = new Intent(ctx, ImageViewerActivity.class);
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_PATH, imagePath);
            ctx.startActivity(intent);
        }
    }

    // ---------- DiffUtil ----------

    private static final DiffUtil.ItemCallback<WrongAnswer> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WrongAnswer>() {
                @Override
                public boolean areItemsTheSame(@NonNull WrongAnswer o, @NonNull WrongAnswer n) {
                    return o.getId() == n.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull WrongAnswer o, @NonNull WrongAnswer n) {
                    return o.getId() == n.getId() && o.getAnsweredAt() == n.getAnsweredAt();
                }
            };
}

