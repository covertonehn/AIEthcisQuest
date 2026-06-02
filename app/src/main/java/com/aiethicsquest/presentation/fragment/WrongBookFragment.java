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
import com.aiethicsquest.databinding.FragmentWrongBookBinding;
import com.aiethicsquest.presentation.viewmodel.ProfileViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.Collections;

/**
 * 错题本页面 Fragment.
 *
 * <p>顶部 TabLayout 对应不同玩法的错题栏目，目前只有"识图挑战"一栏。
 * 点击 Tab 切换后展示对应玩法的错题列表（当前只有一栏，Tab 为预留扩展结构）。</p>
 *
 * <p>后续新增玩法时，只需：</p>
 * <ol>
 *     <li>在 {@link #setupTabs()} 中 addTab 新栏目</li>
 *     <li>在 {@link TabLayout.OnTabSelectedListener} 中处理新栏目的数据加载</li>
 *     <li>在 DAO / Repository 中添加对应查询</li>
 * </ol>
 */
public class WrongBookFragment extends Fragment {

    private FragmentWrongBookBinding binding;
    private ProfileViewModel viewModel;
    private WrongAnswerAdapter wrongAnswerAdapter;

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
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        initViews();
        setupTabs();
        observeData();
    }

    private void initViews() {
        wrongAnswerAdapter = new WrongAnswerAdapter();
        binding.rvWrongAnswers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWrongAnswers.setAdapter(wrongAnswerAdapter);

        binding.btnClearWrong.setOnClickListener(v -> showClearConfirmDialog());
    }

    /**
     * 设置 Tab 栏目（每个 Tab 对应一个玩法的错题）.
     */
    private void setupTabs() {
        // 识图挑战栏目（目前唯一栏目）
        binding.tabLayout.addTab(
                binding.tabLayout.newTab().setText(getString(R.string.wrong_book_tab_image_challenge))
        );

        // Tab 切换监听
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 目前只有识图挑战一栏，切换时不需要额外处理
                // 后续新增玩法栏目时在此处根据 tab.getPosition() 切换数据源
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void observeData() {
        // 错题数量
        viewModel.getWrongAnswerCount().observe(getViewLifecycleOwner(), count -> {
            if (count == null) count = 0;
            binding.tvWrongCount.setText(getString(R.string.wrong_book_count, count));
            binding.btnClearWrong.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        });

        // 错题列表（识图挑战）
        viewModel.getWrongAnswers().observe(getViewLifecycleOwner(), wrongAnswers -> {
            if (wrongAnswers == null || wrongAnswers.isEmpty()) {
                binding.rvWrongAnswers.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                wrongAnswerAdapter.submitList(Collections.emptyList());
            } else {
                binding.rvWrongAnswers.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
                wrongAnswerAdapter.submitList(wrongAnswers);
            }
        });
    }

    private void showClearConfirmDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.wrong_book_clear_confirm_title))
                .setMessage(getString(R.string.wrong_book_clear_confirm_msg))
                .setPositiveButton(getString(R.string.wrong_book_clear_confirm_ok),
                        (dialog, which) -> viewModel.clearAllWrongAnswers())
                .setNegativeButton(getString(R.string.wrong_book_clear_confirm_cancel), null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

