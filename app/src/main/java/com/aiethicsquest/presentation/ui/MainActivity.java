package com.aiethicsquest.presentation.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.aiethicsquest.databinding.ActivityMainBinding;

/**
 * 应用主界面 Activity.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

