package com.aiethicsquest.presentation.fragment;

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
import com.aiethicsquest.databinding.FragmentProfileBinding;
import com.aiethicsquest.presentation.viewmodel.ProfileViewModel;

/**
 * 个人中心页面 Fragment.
 *
 * <p>展示：用户信息、错题本入口卡片、挑战历史入口卡片。</p>
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 使用 Activity 作用域，与 WrongBookFragment 等共享同一 ViewModel 实例
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        initViews();
        observeData();
    }

    private void initViews() {
        // 错题本入口卡片
        binding.cardWrongBook.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_profileFragment_to_wrongBookFragment)
        );

        // 识图挑战历史入口卡片
        binding.cardChallengeHistory.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_profileFragment_to_sessionHistoryFragment)
        );

        // 视频挑战历史入口卡片
        binding.cardVideoHistory.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_profileFragment_to_videoSessionHistoryFragment)
        );
    }

    private void observeData() {
        // 用户昵称
        viewModel.getUserName().observe(getViewLifecycleOwner(), name ->
                binding.tvGreeting.setText(getString(R.string.profile_greeting, name))
        );

        // 识图挑战错题总数
        viewModel.getWrongAnswerCount().observe(getViewLifecycleOwner(), count -> {
            if (count == null) count = 0;
            if (count > 0) {
                binding.tvWrongCount.setText(
                        getString(R.string.profile_wrong_book_entry_count, count));
            } else {
                binding.tvWrongCount.setText(
                        getString(R.string.profile_wrong_book_entry_empty));
            }
        });

        // 识图挑战历史轮次总数
        viewModel.getSessionRecords().observe(getViewLifecycleOwner(), records -> {
            int count = (records == null) ? 0 : records.size();
            if (count > 0) {
                binding.tvHistoryCount.setText(
                        getString(R.string.profile_history_entry_count, count));
            } else {
                binding.tvHistoryCount.setText(
                        getString(R.string.profile_history_entry_empty));
            }
        });

        // 视频挑战历史轮次总数
        viewModel.getVideoSessionRecords().observe(getViewLifecycleOwner(), records -> {
            int count = (records == null) ? 0 : records.size();
            if (count > 0) {
                binding.tvVideoHistoryCount.setText(
                        getString(R.string.profile_history_entry_count, count));
            } else {
                binding.tvVideoHistoryCount.setText(
                        getString(R.string.profile_history_entry_empty));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
