package com.aiethicsquest.presentation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aiethicsquest.R;
import com.aiethicsquest.databinding.FragmentHomeBinding;

/**
 * 首页 Fragment：玩法入口卡片列表.
 *
 * <p>展示所有可用的玩法卡片，点击卡片后导航到对应玩法页面。
 * 目前包含：识图挑战。后续可在此处继续添加更多玩法卡片。</p>
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
    }

    /**
     * 绑定各玩法卡片的点击事件.
     */
    private void initViews() {
        // 识图挑战卡片 → 导航到识图挑战页
        binding.cardImageChallenge.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_homeFragment_to_imageChallengeFragment)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
