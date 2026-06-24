package com.aiethicsquest.presentation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aiethicsquest.R;
import com.aiethicsquest.databinding.FragmentSessionDetailBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 视频挑战历史详情页 Fragment.
 *
 * <p>展示某一轮视频挑战的详细情况：汇总数据（时间、得分、正确率）和每道题的双视频详情。</p>
 *
 * <p>通过 Bundle 参数传入数据（参见 ARG_* 常量）。</p>
 */
public class VideoSessionDetailFragment extends Fragment {

    /** Bundle key：VideoSessionRecord ID. */
    public static final String ARG_SESSION_ID = "arg_video_session_id";
    /** Bundle key：完成时间戳（毫秒）. */
    public static final String ARG_FINISHED_AT = "arg_video_finished_at";
    /** Bundle key：总题数. */
    public static final String ARG_TOTAL_COUNT = "arg_video_total_count";
    /** Bundle key：答对题数. */
    public static final String ARG_CORRECT_COUNT = "arg_video_correct_count";
    /** Bundle key：视频路径字符串. */
    public static final String ARG_VIDEO_PATHS = "arg_video_paths";
    /** Bundle key：每题答题结果字符串. */
    public static final String ARG_QUESTION_RESULTS = "arg_video_question_results";

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    private FragmentSessionDetailBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 复用识图挑战的详情页布局
        binding = FragmentSessionDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        long finishedAt = args.getLong(ARG_FINISHED_AT, 0);
        int totalCount = args.getInt(ARG_TOTAL_COUNT, 0);
        int correctCount = args.getInt(ARG_CORRECT_COUNT, 0);
        String videoPaths = args.getString(ARG_VIDEO_PATHS, "");
        String questionResults = args.getString(ARG_QUESTION_RESULTS, "");

        bindSummary(finishedAt, totalCount, correctCount);
        bindQuestionList(videoPaths, questionResults);
    }

    /**
     * 绑定汇总信息（时间、得分、正确率）.
     */
    private void bindSummary(long finishedAt, int totalCount, int correctCount) {
        binding.tvDetailTime.setText(DATE_FORMAT.format(new Date(finishedAt)));

        binding.tvDetailScore.setText(
                getString(R.string.session_complete_score, correctCount, totalCount));

        int pct = totalCount > 0 ? Math.round((float) correctCount / totalCount * 100) : 0;
        binding.tvDetailAccuracy.setText(pct + "%");
    }

    /**
     * 解析 videoPaths 和 questionResults，构建每题数据并展示列表.
     */
    private void bindQuestionList(String videoPaths, String questionResults) {
        List<VideoSessionDetailAdapter.VideoQuestionItem> items = new ArrayList<>();

        if (videoPaths == null || videoPaths.isEmpty()) {
            return;
        }

        String[] pathGroups = videoPaths.split("\\|");
        String[] resultGroups = questionResults != null && !questionResults.isEmpty()
                ? questionResults.split("\\|")
                : new String[0];

        for (int i = 0; i < pathGroups.length; i++) {
            String pathGroup = pathGroups[i];
            String[] paths = pathGroup.split(",");
            if (paths.length < 2) continue;

            String topPath = paths[0].trim();
            String bottomPath = paths[1].trim();

            int aiSide = 0;
            boolean isCorrect = true;

            if (i < resultGroups.length) {
                String[] parts = resultGroups[i].split(":");
                if (parts.length >= 2) {
                    try {
                        aiSide = Integer.parseInt(parts[0].trim());
                        isCorrect = Integer.parseInt(parts[1].trim()) == 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            items.add(new VideoSessionDetailAdapter.VideoQuestionItem(
                    i + 1, topPath, bottomPath, aiSide, isCorrect));
        }

        VideoSessionDetailAdapter adapter = new VideoSessionDetailAdapter(items);
        binding.rvSessionDetail.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessionDetail.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

