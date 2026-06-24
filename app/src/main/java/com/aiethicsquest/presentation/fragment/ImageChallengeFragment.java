package com.aiethicsquest.presentation.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.aiethicsquest.R;
import com.aiethicsquest.common.util.ImageLoader;
import com.aiethicsquest.data.model.QuizQuestion;
import com.aiethicsquest.databinding.FragmentImageChallengeBinding;
import com.aiethicsquest.presentation.activity.ImageViewerActivity;
import com.aiethicsquest.presentation.activity.SeaUrchinSelectView;
import com.aiethicsquest.presentation.viewmodel.ChallengeViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 识图挑战玩法页 Fragment.
 *
 * <p>交互方式：</p>
 * <ul>
 *     <li>点击图片 → 全屏查看（{@link ImageViewerActivity}）</li>
 *     <li>底部海胆选择控件：长按后滑向上方 → 选择上方图片；滑向下方 → 选择下方图片；取消区域 → 不触发</li>
 *     <li>答题后底部切换为结果反馈 + 下一题按钮</li>
 * </ul>
 */
public class ImageChallengeFragment extends Fragment {

    private FragmentImageChallengeBinding binding;
    private ChallengeViewModel viewModel;

    /** 标记当前是否已答题（防止重复操作）. */
    private boolean hasAnswered = false;

    /** 当前题目的上图路径，用于全屏查看. */
    private String currentLeftImagePath;
    /** 当前题目的下图路径，用于全屏查看. */
    private String currentRightImagePath;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentImageChallengeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 使用 Activity 作用域的 ViewModel，确保返回此页面时保留进度
        viewModel = new ViewModelProvider(requireActivity()).get(ChallengeViewModel.class);
        initViews();
        observeData();
    }

    // ---------- 初始化 ----------

    private void initViews() {
        // 开始挑战
        binding.btnStart.setOnClickListener(v -> {
            binding.tvNoQuestions.setVisibility(View.GONE);
            viewModel.startNewSession();
        });

        // 重置题库
        binding.btnResetBank.setOnClickListener(v -> showResetBankConfirmDialog());

        // 下一题
        binding.btnNext.setOnClickListener(v -> {
            hideResultArea();
            viewModel.nextQuestion();
        });

        // 点击图片：全屏查看
        binding.containerLeft.setOnClickListener(v -> openImageViewer(currentLeftImagePath));
        binding.containerRight.setOnClickListener(v -> openImageViewer(currentRightImagePath));

        // 海胆选择控件
        binding.seaUrchinSelect.setOnSelectListener(new SeaUrchinSelectView.OnSelectListener() {
            @Override
            public void onSelect(int side) {
                // side: 0=上图(left), 1=下图(right)
                onUserSelectSide(side);
            }

            @Override
            public void onHover(int hoveredSide) {
                updateSelectionHighlight(hoveredSide);
            }
        });
    }

    // ---------- 数据观察 ----------

    private void observeData() {
        // 剩余未做题数量 → 更新等待页信息
        viewModel.getUndoneCount().observe(getViewLifecycleOwner(), count -> {
            if (count == null) count = 0;
            int nextCount = Math.min(count, ChallengeViewModel.QUESTIONS_PER_SESSION);
            binding.tvNextCount.setText(
                    getString(R.string.challenge_next_count, nextCount));
            binding.tvRemaining.setText(
                    getString(R.string.challenge_remaining, count));
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading == null) return;
            binding.layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getCurrentQuestion().observe(getViewLifecycleOwner(), question -> {
            if (question == null) return;
            hasAnswered = false;
            // 切换到答题状态
            binding.scrollWaiting.setVisibility(View.GONE);
            binding.layoutPlaying.setVisibility(View.VISIBLE);
            showQuestion(question);
        });

        viewModel.getQuizProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress == null) return;
            binding.layoutProgress.setVisibility(View.VISIBLE);
            binding.tvProgress.setText(
                    getString(R.string.challenge_progress, progress.current, progress.total));
        });

        viewModel.getAnswerResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            showResult(result);
            viewModel.consumeAnswerResult();
        });

        viewModel.getSessionComplete().observe(getViewLifecycleOwner(), correctCount -> {
            if (correctCount == null) return;
            showSessionCompleteDialog(correctCount);
            viewModel.consumeSessionComplete();
        });

        // 题库已用尽
        viewModel.getNoMoreQuestions().observe(getViewLifecycleOwner(), totalCount -> {
            if (totalCount == null) return;
            showNoMoreQuestionsDialog(totalCount);
            viewModel.consumeNoMoreQuestions();
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isEmpty()) return;
            if ("no_questions".equals(msg)) {
                binding.tvNoQuestions.setVisibility(View.VISIBLE);
            }
        });
    }

    // ---------- UI 更新 ----------

    /**
     * 展示题目：加载双图、重置选中状态.
     */
    private void showQuestion(QuizQuestion question) {
        binding.containerLeft.setVisibility(View.VISIBLE);
        binding.containerRight.setVisibility(View.VISIBLE);

        binding.containerLeft.setBackground(
                requireContext().getDrawable(R.drawable.bg_image_unselected));
        binding.containerRight.setBackground(
                requireContext().getDrawable(R.drawable.bg_image_unselected));

        binding.tvLeftLabel.setVisibility(View.GONE);
        binding.tvRightLabel.setVisibility(View.GONE);

        currentLeftImagePath = question.getLeftImagePath();
        currentRightImagePath = question.getRightImagePath();

        ImageLoader.loadFromAssets(requireContext(), binding.ivLeft, currentLeftImagePath);
        ImageLoader.loadFromAssets(requireContext(), binding.ivRight, currentRightImagePath);

        binding.tvDescription.setText(question.getDescription());
        binding.tvDescription.setVisibility(View.VISIBLE);

        // 显示底部操作区（海胆选择）
        binding.layoutBottomAction.setVisibility(View.VISIBLE);
        showSelectArea();
    }

    /**
     * 打开全屏图片查看器.
     *
     * @param imagePath assets 图片路径
     */
    private void openImageViewer(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return;
        Intent intent = new Intent(requireContext(), ImageViewerActivity.class);
        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_PATH, imagePath);
        startActivity(intent);
    }

    /**
     * 用户通过海胆控件选择某一侧，提交答案.
     *
     * @param side 0=上图(left), 1=下图(right)
     */
    private void onUserSelectSide(int side) {
        if (hasAnswered) return;
        hasAnswered = true;

        // 清除高亮
        updateSelectionHighlight(-1);

        if (side == 0) {
            binding.containerLeft.setBackground(
                    requireContext().getDrawable(R.drawable.bg_image_selected));
        } else {
            binding.containerRight.setBackground(
                    requireContext().getDrawable(R.drawable.bg_image_selected));
        }

        viewModel.submitAnswer(side);
    }

    /**
     * 更新海胆悬停高亮（绿色边框）.
     *
     * @param hoveredSide 0=上图高亮, 1=下图高亮, -1=无高亮
     */
    private void updateSelectionHighlight(int hoveredSide) {
        binding.containerLeft.setBackground(
                requireContext().getDrawable(
                        hoveredSide == 0
                                ? R.drawable.bg_image_highlight
                                : R.drawable.bg_image_unselected));
        binding.containerRight.setBackground(
                requireContext().getDrawable(
                        hoveredSide == 1
                                ? R.drawable.bg_image_highlight
                                : R.drawable.bg_image_unselected));
    }

    /** 显示海胆选择区域，隐藏结果区域. */
    private void showSelectArea() {
        binding.layoutSelectArea.setVisibility(View.VISIBLE);
        binding.layoutResultArea.setVisibility(View.GONE);
    }

    /** 显示结果区域，隐藏海胆选择区域. */
    private void showResultArea() {
        binding.layoutSelectArea.setVisibility(View.GONE);
        binding.layoutResultArea.setVisibility(View.VISIBLE);
    }

    private void hideResultArea() {
        binding.layoutResultArea.setVisibility(View.GONE);
        hasAnswered = false;
    }

    /**
     * 展示答题结果反馈.
     */
    private void showResult(ChallengeViewModel.AnswerResult result) {
        // 切换底部区域为结果显示
        showResultArea();

        if (result.correct) {
            binding.tvResult.setText(getString(R.string.result_correct));
            binding.tvResult.setTextColor(requireContext().getColor(R.color.color_correct));
            binding.tvResultHint.setText(getString(R.string.result_correct_hint));
            binding.layoutResultInner.setBackgroundColor(
                    requireContext().getColor(R.color.color_correct_bg));
        } else {
            binding.tvResult.setText(getString(R.string.result_wrong));
            binding.tvResult.setTextColor(requireContext().getColor(R.color.color_wrong));
            binding.tvResultHint.setText(getString(R.string.result_wrong_hint));
            binding.layoutResultInner.setBackgroundColor(
                    requireContext().getColor(R.color.color_wrong_bg));
        }

        showImageLabels(result.correctSide);
        binding.btnNext.setVisibility(View.VISIBLE);
    }

    private void showImageLabels(int aiSide) {
        if (aiSide == 0) {
            binding.tvLeftLabel.setText(getString(R.string.result_ai_label));
            binding.tvLeftLabel.setBackgroundResource(R.drawable.bg_label_ai);
            binding.tvRightLabel.setText(getString(R.string.result_real_label));
            binding.tvRightLabel.setBackgroundResource(R.drawable.bg_label_real);
        } else {
            binding.tvLeftLabel.setText(getString(R.string.result_real_label));
            binding.tvLeftLabel.setBackgroundResource(R.drawable.bg_label_real);
            binding.tvRightLabel.setText(getString(R.string.result_ai_label));
            binding.tvRightLabel.setBackgroundResource(R.drawable.bg_label_ai);
        }
        binding.tvLeftLabel.setVisibility(View.VISIBLE);
        binding.tvRightLabel.setVisibility(View.VISIBLE);
    }

    /**
     * 展示本轮结束弹窗.
     *
     * @param correctCount 本轮答对题数
     */
    private void showSessionCompleteDialog(int correctCount) {
        int total = viewModel.getQuizProgress().getValue() != null
                ? viewModel.getQuizProgress().getValue().total
                : correctCount;

        int accuracyPct = total > 0 ? Math.round(correctCount * 100f / total) : 0;
        String accuracyStr = String.valueOf(accuracyPct);

        String scoreMsg = getString(R.string.session_complete_score, correctCount, total);
        String accuracyMsg = getString(R.string.session_complete_accuracy, accuracyStr);

        String encourageMsg;
        if (correctCount == total) {
            encourageMsg = getString(R.string.session_complete_excellent);
        } else if (correctCount >= Math.ceil(total / 2.0)) {
            encourageMsg = getString(R.string.session_complete_good);
        } else {
            encourageMsg = getString(R.string.session_complete_keep_trying);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.session_complete_title))
                .setMessage(scoreMsg + "　" + accuracyMsg + "\n\n" + encourageMsg)
                .setPositiveButton(getString(R.string.challenge_btn_restart),
                        (dialog, which) -> resetToWaitingState())
                .setNegativeButton(getString(R.string.btn_back_home),
                        (dialog, which) -> navigateBackToHome())
                .setCancelable(false)
                .show();

        // 隐藏答题区
        resetPlayingArea();
    }

    /**
     * 展示"题库已用尽"弹窗.
     *
     * @param totalCount 题库总题数
     */
    private void showNoMoreQuestionsDialog(int totalCount) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.no_more_questions_title))
                .setMessage(getString(R.string.no_more_questions_msg, totalCount))
                .setPositiveButton(getString(R.string.no_more_questions_reset), (dialog, which) -> {
                    viewModel.resetQuestionBank();
                    resetToWaitingState();
                })
                .setNegativeButton(getString(R.string.no_more_questions_back),
                        (dialog, which) -> navigateBackToHome())
                .setCancelable(false)
                .show();
    }

    /**
     * 展示"重置题库"确认弹窗.
     */
    private void showResetBankConfirmDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.challenge_reset_bank_title))
                .setMessage(getString(R.string.challenge_reset_bank_msg))
                .setPositiveButton(getString(R.string.challenge_reset_bank_ok),
                        (dialog, which) -> viewModel.resetQuestionBank())
                .setNegativeButton(getString(R.string.challenge_reset_bank_cancel), null)
                .show();
    }

    /**
     * 重置为"等待开始"状态.
     */
    private void resetToWaitingState() {
        hasAnswered = false;
        viewModel.resetSession();
        binding.scrollWaiting.setVisibility(View.VISIBLE);
        binding.layoutPlaying.setVisibility(View.GONE);
        binding.tvNoQuestions.setVisibility(View.GONE);
        resetPlayingArea();
    }

    /**
     * 清空答题区各元素可见性.
     */
    private void resetPlayingArea() {
        binding.containerLeft.setVisibility(View.GONE);
        binding.containerRight.setVisibility(View.GONE);
        binding.layoutProgress.setVisibility(View.GONE);
        binding.tvDescription.setVisibility(View.GONE);
        binding.layoutBottomAction.setVisibility(View.GONE);
        binding.layoutLoading.setVisibility(View.GONE);
    }

    /**
     * 导航回首页.
     */
    private void navigateBackToHome() {
        viewModel.resetSession();
        if (getView() != null) {
            Navigation.findNavController(getView())
                    .popBackStack(R.id.homeFragment, false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

