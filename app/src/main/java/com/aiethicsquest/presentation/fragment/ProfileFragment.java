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
import com.aiethicsquest.databinding.FragmentProfileBinding;
import com.aiethicsquest.presentation.viewmodel.ProfileViewModel;

public class ProfileFragment extends Fragment {
    private ProfileViewModel viewModel;
    private FragmentProfileBinding binding;

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
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        observeData();
    }

    private void observeData() {
        viewModel.getUserName().observe(getViewLifecycleOwner(), name -> {
            binding.tvGreeting.setText(
                    getString(R.string.profile_greeting, name));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
