package com.aiethicsquest.presentation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.aiethicsquest.R;
import com.aiethicsquest.databinding.FragmentHomeBinding;
import com.aiethicsquest.presentation.viewmodel.HomeViewModel;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    private HomeViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        initViews();
        observeData();
    }

    private void initViews(){
        binding.btnIncrement.setOnClickListener(v -> viewModel.increment());
        binding.btnReset.setOnClickListener(v -> viewModel.reset());
    }

    private void observeData(){
        viewModel.getCount().observe(getViewLifecycleOwner(), count -> {
            binding.tvCount.setText(String.valueOf(count));
            binding.tvHint.setText(getString(R.string.home_click_hint, count));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
