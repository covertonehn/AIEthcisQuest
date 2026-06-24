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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aiethicsquest.R;
import com.aiethicsquest.data.model.VideoSessionRecord;
import com.aiethicsquest.databinding.FragmentSessionHistoryBinding;
import com.aiethicsquest.presentation.viewmodel.ProfileViewModel;

/**
 * 视频挑战历史列表页 Fragment.
 *
 * <p>展示所有视频挑战历史轮次，每条记录可点击进入该轮详情页。</p>
 */
public class VideoSessionHistoryFragment extends Fragment {

    private FragmentSessionHistoryBinding binding;
    private ProfileViewModel viewModel;
    private VideoSessionRecordAdapter videoSessionRecordAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 复用识图挑战的历史列表布局
        binding = FragmentSessionHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 使用 Activity 作用域，与其他个人中心 Fragment 共享同一 ViewModel 实例
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        initViews();
        observeData();
    }

    private void initViews() {
        videoSessionRecordAdapter = new VideoSessionRecordAdapter(
                record -> navigateToDetail(record));
        binding.rvSessionHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSessionHistory.setAdapter(videoSessionRecordAdapter);
    }

    private void observeData() {
        viewModel.getVideoSessionRecords().observe(getViewLifecycleOwner(), records -> {
            if (records == null || records.isEmpty()) {
                binding.layoutHistoryEmpty.setVisibility(View.VISIBLE);
                binding.rvSessionHistory.setVisibility(View.GONE);
                binding.tvTotalSessions.setText(
                        getString(R.string.history_session_count, 0));
            } else {
                binding.layoutHistoryEmpty.setVisibility(View.GONE);
                binding.rvSessionHistory.setVisibility(View.VISIBLE);
                videoSessionRecordAdapter.submitList(records);
                binding.tvTotalSessions.setText(
                        getString(R.string.history_session_count, records.size()));
            }
        });
    }

    /**
     * 导航到视频挑战历史详情页.
     *
     * @param record 要查看的会话记录
     */
    private void navigateToDetail(VideoSessionRecord record) {
        Bundle args = new Bundle();
        args.putInt(VideoSessionDetailFragment.ARG_SESSION_ID, record.getId());
        args.putLong(VideoSessionDetailFragment.ARG_FINISHED_AT, record.getFinishedAt());
        args.putInt(VideoSessionDetailFragment.ARG_TOTAL_COUNT, record.getTotalCount());
        args.putInt(VideoSessionDetailFragment.ARG_CORRECT_COUNT, record.getCorrectCount());
        args.putString(VideoSessionDetailFragment.ARG_VIDEO_PATHS, record.getVideoPaths());
        args.putString(VideoSessionDetailFragment.ARG_QUESTION_RESULTS, record.getQuestionResults());
        Navigation.findNavController(requireView())
                .navigate(
                        R.id.action_videoSessionHistoryFragment_to_videoSessionDetailFragment,
                        args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

