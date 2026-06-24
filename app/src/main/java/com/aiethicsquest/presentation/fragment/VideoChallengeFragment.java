package com.aiethicsquest.presentation.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.aiethicsquest.R;
import com.aiethicsquest.data.model.VideoQuestion;
import com.aiethicsquest.databinding.FragmentVideoChallengeBinding;
import com.aiethicsquest.presentation.activity.SeaUrchinSelectView;
import com.aiethicsquest.presentation.activity.VideoViewerActivity;
import com.aiethicsquest.presentation.viewmodel.VideoChallengeViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 视频挑战玩法页 Fragment.
 *
 * <p>交互方式：</p>
 * <ul>
 *     <li>视频区域单击 → 切换控制栏显隐；双击 → 播放/暂停</li>
 *     <li>底部海胆选择控件：长按后滑向上方 → 选择上方视频；滑向下方 → 选择下方视频；取消区域松开 → 不触发</li>
 *     <li>答题后底部切换为结果反馈 + 下一题按钮</li>
 *     <li>全屏按钮进入全屏播放，返回后同步进度</li>
 * </ul>
 */
public class VideoChallengeFragment extends Fragment {

    private FragmentVideoChallengeBinding binding;
    private VideoChallengeViewModel viewModel;

    /** 标记当前是否已答题（防止重复操作）. */
    private boolean hasAnswered = false;

    /** 当前正在请求全屏的是哪一侧（0=上方, 1=下方，-1=无请求）. */
    private int fullscreenRequestSide = -1;

    /** 全屏播放 ActivityResultLauncher. */
    private ActivityResultLauncher<Intent> fullscreenLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fullscreenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        int position = result.getData()
                                .getIntExtra(VideoViewerActivity.RESULT_POSITION, 0);
                        boolean isPlaying = result.getData()
                                .getBooleanExtra(VideoViewerActivity.RESULT_IS_PLAYING, false);
                        onFullscreenReturned(fullscreenRequestSide, position, isPlaying);
                    }
                    fullscreenRequestSide = -1;
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVideoChallengeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(VideoChallengeViewModel.class);
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
            stopAllVideos();
            viewModel.nextQuestion();
        });

        // 视频播放器全屏请求
        binding.videoPlayerTop.setOnFullscreenRequestListener(
                (path, playing, position) -> openFullscreen(0, path, playing, position));
        binding.videoPlayerBottom.setOnFullscreenRequestListener(
                (path, playing, position) -> openFullscreen(1, path, playing, position));

        // 海胆选择控件
        binding.seaUrchinSelect.setOnSelectListener(new SeaUrchinSelectView.OnSelectListener() {
            @Override
            public void onSelect(int side) {
                // side: 0=上方视频, 1=下方视频
                onUserSelectSide(side);
            }

            @Override
            public void onHover(int hoveredSide) {
                // 高亮对应视频容器边框
                updateSelectionHighlight(hoveredSide);
            }
        });
    }

    // ---------- 数据观察 ----------

    private void observeData() {
        viewModel.getUndoneCount().observe(getViewLifecycleOwner(), count -> {
            if (count == null) count = 0;
            int nextCount = Math.min(count, VideoChallengeViewModel.QUESTIONS_PER_SESSION);
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
            // 切换到答题状态：隐藏等待区，显示答题区
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
     * 展示题目：加载双视频播放器、显示底部海胆选择区.
     */
    private void showQuestion(VideoQuestion question) {
        binding.containerTop.setVisibility(View.VISIBLE);
        binding.containerBottom.setVisibility(View.VISIBLE);

        // 重置边框为未选中状态
        binding.containerTop.setBackground(
                requireContext().getDrawable(R.drawable.bg_image_unselected));
        binding.containerBottom.setBackground(
                requireContext().getDrawable(R.drawable.bg_image_unselected));

        binding.tvTopLabel.setVisibility(View.GONE);
        binding.tvBottomLabel.setVisibility(View.GONE);

        // 加载视频
        binding.videoPlayerTop.setVideoPath(question.getTopVideoPath());
        binding.videoPlayerBottom.setVideoPath(question.getBottomVideoPath());

        binding.tvDescription.setText(question.getDescription());
        binding.tvDescription.setVisibility(View.VISIBLE);

        // 显示底部操作区，并显示海胆选择控件
        binding.layoutBottomAction.setVisibility(View.VISIBLE);
        showSelectArea();
    }

    /**
     * 用户通过海胆控件选择某一侧，提交答案.
     *
     * @param side 0=上方视频, 1=下方视频
     */
    private void onUserSelectSide(int side) {
        if (hasAnswered) return;
        hasAnswered = true;

        // 清除高亮
        updateSelectionHighlight(-1);

        // 标记已选边框
        if (side == 0) {
            binding.containerTop.setBackground(
                    requireContext().getDrawable(R.drawable.bg_image_selected));
        } else {
            binding.containerBottom.setBackground(
                    requireContext().getDrawable(R.drawable.bg_image_selected));
        }

        viewModel.submitAnswer(side);
    }

    /**
     * 更新海胆悬停高亮（绿色边框）.
     *
     * @param hoveredSide 0=上方高亮, 1=下方高亮, -1=无高亮
     */
    private void updateSelectionHighlight(int hoveredSide) {
        binding.containerTop.setBackground(
                requireContext().getDrawable(
                        hoveredSide == 0
                                ? R.drawable.bg_image_highlight
                                : R.drawable.bg_image_unselected));
        binding.containerBottom.setBackground(
                requireContext().getDrawable(
                        hoveredSide == 1
                                ? R.drawable.bg_image_highlight
                                : R.drawable.bg_image_unselected));
    }

    /**
     * 打开全屏播放页.
     */
    private void openFullscreen(int side, String path, boolean playing, int position) {
        fullscreenRequestSide = side;
        if (side == 0) {
            binding.videoPlayerBottom.pause();
        } else {
            binding.videoPlayerTop.pause();
        }

        Intent intent = new Intent(requireContext(), VideoViewerActivity.class);
        intent.putExtra(VideoViewerActivity.EXTRA_VIDEO_PATH, path);
        intent.putExtra(VideoViewerActivity.EXTRA_START_POSITION, position);
        intent.putExtra(VideoViewerActivity.EXTRA_AUTO_PLAY, playing);
        fullscreenLauncher.launch(intent);
    }

    /**
     * 从全屏返回后同步播放进度.
     */
    private void onFullscreenReturned(int side, int position, boolean isPlaying) {
        if (side == 0) {
            if (isPlaying) binding.videoPlayerTop.seekAndPlay(position);
        } else if (side == 1) {
            if (isPlaying) binding.videoPlayerBottom.seekAndPlay(position);
        }
    }

    /** 停止双侧视频播放. */
    private void stopAllVideos() {
        binding.videoPlayerTop.pause();
        binding.videoPlayerBottom.pause();
    }

    /**
     * 显示海胆选择区域，隐藏结果区域.
     */
    private void showSelectArea() {
        binding.layoutSelectArea.setVisibility(View.VISIBLE);
        binding.layoutResultArea.setVisibility(View.GONE);
    }

    /**
     * 显示结果区域，隐藏海胆选择区域.
     */
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
    private void showResult(VideoChallengeViewModel.AnswerResult result) {
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

        showVideoLabels(result.correctSide);
        binding.btnNext.setVisibility(View.VISIBLE);
    }

    private void showVideoLabels(int aiSide) {
        if (aiSide == 0) {
            binding.tvTopLabel.setText(getString(R.string.result_ai_label));
            binding.tvTopLabel.setBackgroundResource(R.drawable.bg_label_ai);
            binding.tvBottomLabel.setText(getString(R.string.result_real_label));
            binding.tvBottomLabel.setBackgroundResource(R.drawable.bg_label_real);
        } else {
            binding.tvTopLabel.setText(getString(R.string.result_real_label));
            binding.tvTopLabel.setBackgroundResource(R.drawable.bg_label_real);
            binding.tvBottomLabel.setText(getString(R.string.result_ai_label));
            binding.tvBottomLabel.setBackgroundResource(R.drawable.bg_label_ai);
        }
        binding.tvTopLabel.setVisibility(View.VISIBLE);
        binding.tvBottomLabel.setVisibility(View.VISIBLE);
    }

    /**
     * 展示本轮结束弹窗.
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

        resetPlayingArea();
    }

    /** 展示"题库已用尽"弹窗. */
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

    /** 展示"重置题库"确认弹窗. */
    private void showResetBankConfirmDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.challenge_reset_bank_title))
                .setMessage(getString(R.string.challenge_reset_bank_msg))
                .setPositiveButton(getString(R.string.challenge_reset_bank_ok),
                        (dialog, which) -> viewModel.resetQuestionBank())
                .setNegativeButton(getString(R.string.challenge_reset_bank_cancel), null)
                .show();
    }

    /** 重置为"等待开始"状态. */
    private void resetToWaitingState() {
        hasAnswered = false;
        viewModel.resetSession();
        binding.scrollWaiting.setVisibility(View.VISIBLE);
        binding.layoutPlaying.setVisibility(View.GONE);
        binding.tvNoQuestions.setVisibility(View.GONE);
        resetPlayingArea();
    }

    /** 清空答题区各元素可见性，并释放视频资源. */
    private void resetPlayingArea() {
        stopAllVideos();
        binding.containerTop.setVisibility(View.GONE);
        binding.containerBottom.setVisibility(View.GONE);
        binding.layoutProgress.setVisibility(View.GONE);
        binding.tvDescription.setVisibility(View.GONE);
        binding.layoutBottomAction.setVisibility(View.GONE);
        binding.layoutLoading.setVisibility(View.GONE);
    }

    /** 导航回首页. */
    private void navigateBackToHome() {
        viewModel.resetSession();
        if (getView() != null) {
            Navigation.findNavController(getView())
                    .popBackStack(R.id.homeFragment, false);
        }
    }

    // ---------- 生命周期 ----------

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) {
            binding.videoPlayerTop.onPause();
            binding.videoPlayerBottom.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.videoPlayerTop.onDestroy();
            binding.videoPlayerBottom.onDestroy();
        }
        binding = null;
    }
}

