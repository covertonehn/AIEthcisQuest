package com.aiethicsquest.presentation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aiethicsquest.R;
import com.aiethicsquest.data.model.VideoWrongAnswer;
import com.aiethicsquest.data.model.WrongAnswer;
import com.aiethicsquest.databinding.FragmentWrongBookBinding;
import com.aiethicsquest.presentation.viewmodel.ProfileViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.Collections;
import java.util.List;

/**
 * 错题本页面 Fragment.
 *
 * <p>顶部 TabLayout 区分"识图挑战"和"视频挑战"两个错题栏目。
 * 点击 Tab 切换后展示对应玩法的错题列表。</p>
 *
 * <p>修复说明：</p>
 * <ul>
 *     <li>使用 {@code requireActivity()} 作用域的 ViewModel，保证 Fragment
 *         重建后 ViewModel 不丢失，LiveData 不需要重新查询数据库。</li>
 *     <li>ViewModel 内部缓存 LiveData 实例，保证所有 observe 订阅的是同一对象。</li>
 *     <li>切换 Tab 时直接调用 {@link #renderCurrentTab()} 用 LiveData
 *         当前缓存值（已到达则立刻更新，未到达则等待回调），不依赖 getValue() 同步读取。</li>
 * </ul>
 */
public class WrongBookFragment extends Fragment {

    /** Tab 位置：识图挑战. */
    private static final int TAB_IMAGE = 0;
    /** Tab 位置：视频挑战. */
    private static final int TAB_VIDEO = 1;

    private FragmentWrongBookBinding binding;
    private ProfileViewModel viewModel;
    private WrongAnswerAdapter wrongAnswerAdapter;
    private VideoWrongAnswerAdapter videoWrongAnswerAdapter;

    /** 当前激活的 Tab（默认识图挑战）. */
    private int currentTab = TAB_IMAGE;

    // 保存 LiveData 最新数据，用于切换 Tab 时即时渲染
    private List<WrongAnswer> latestImageList = Collections.emptyList();
    private int latestImageCount = 0;
    private List<VideoWrongAnswer> latestVideoList = Collections.emptyList();
    private int latestVideoCount = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentWrongBookBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ★ 关键：使用 Activity 作用域，Fragment 重建后 ViewModel 不丢失
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        initViews();
        setupTabs();
        observeData();
    }

    // ---------- 初始化 ----------

    private void initViews() {
        wrongAnswerAdapter = new WrongAnswerAdapter();
        videoWrongAnswerAdapter = new VideoWrongAnswerAdapter();

        binding.rvWrongAnswers.setLayoutManager(new LinearLayoutManager(requireContext()));
        // 默认显示识图 Tab 的 Adapter
        binding.rvWrongAnswers.setAdapter(wrongAnswerAdapter);

        binding.btnClearWrong.setOnClickListener(v -> showClearConfirmDialog());
    }

    /**
     * 设置 Tab 栏目.
     */
    private void setupTabs() {
        binding.tabLayout.addTab(
                binding.tabLayout.newTab()
                        .setText(getString(R.string.wrong_book_tab_image_challenge)));
        binding.tabLayout.addTab(
                binding.tabLayout.newTab()
                        .setText(getString(R.string.wrong_book_tab_video_challenge)));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                // 切换 Adapter 并立刻用已缓存的数据渲染
                switchAdapterAndRender();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    // ---------- 数据观察 ----------

    private void observeData() {
        // ----- 识图挑战错题列表 -----
        viewModel.getWrongAnswers().observe(getViewLifecycleOwner(), wrongAnswers -> {
            // 保存最新数据到本地缓存
            latestImageList  = wrongAnswers != null ? wrongAnswers : Collections.emptyList();
            latestImageCount = latestImageList.size();

            // 始终更新 Adapter 数据（即使当前不在识图 Tab 也要同步，以便切换时立刻显示）
            wrongAnswerAdapter.submitList(latestImageList);

            // 仅在当前是识图 Tab 时更新 UI 状态（数量文字、可见性）
            if (currentTab == TAB_IMAGE) {
                renderImageTab();
            }
        });

        // ----- 视频挑战错题列表 -----
        viewModel.getVideoWrongAnswers().observe(getViewLifecycleOwner(), videoWrongAnswers -> {
            latestVideoList  = videoWrongAnswers != null ? videoWrongAnswers : Collections.emptyList();
            latestVideoCount = latestVideoList.size();

            videoWrongAnswerAdapter.submitList(latestVideoList);

            if (currentTab == TAB_VIDEO) {
                renderVideoTab();
            }
        });
    }

    // ---------- Tab 切换 ----------

    /**
     * 切换 Adapter 并用当前缓存数据立刻渲染 UI.
     *
     * <p>此时 latestImageList / latestVideoList 已由 LiveData 回调填充，
     * 可以直接使用，无需等待异步数据库查询。</p>
     */
    private void switchAdapterAndRender() {
        if (currentTab == TAB_VIDEO) {
            binding.rvWrongAnswers.setAdapter(videoWrongAnswerAdapter);
            renderVideoTab();
        } else {
            binding.rvWrongAnswers.setAdapter(wrongAnswerAdapter);
            renderImageTab();
        }
    }

    /** 渲染识图错题 Tab 的 UI 状态. */
    private void renderImageTab() {
        binding.tvWrongCount.setText(
                getString(R.string.wrong_book_count, latestImageCount));
        binding.btnClearWrong.setVisibility(
                latestImageCount > 0 ? View.VISIBLE : View.GONE);
        boolean empty = latestImageList.isEmpty();
        binding.rvWrongAnswers.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    /** 渲染视频错题 Tab 的 UI 状态. */
    private void renderVideoTab() {
        binding.tvWrongCount.setText(
                getString(R.string.wrong_book_count, latestVideoCount));
        binding.btnClearWrong.setVisibility(
                latestVideoCount > 0 ? View.VISIBLE : View.GONE);
        boolean empty = latestVideoList.isEmpty();
        binding.rvWrongAnswers.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // ---------- 操作 ----------

    private void showClearConfirmDialog() {
        String msg = currentTab == TAB_VIDEO
                ? getString(R.string.wrong_book_clear_confirm_msg_video)
                : getString(R.string.wrong_book_clear_confirm_msg);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.wrong_book_clear_confirm_title))
                .setMessage(msg)
                .setPositiveButton(getString(R.string.wrong_book_clear_confirm_ok),
                        (dialog, which) -> {
                            if (currentTab == TAB_VIDEO) {
                                viewModel.clearAllVideoWrongAnswers();
                            } else {
                                viewModel.clearAllWrongAnswers();
                            }
                        })
                .setNegativeButton(getString(R.string.wrong_book_clear_confirm_cancel), null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

