package com.aiethicsquest.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ProfileViewModel extends ViewModel {
    private final MutableLiveData<String> userName = new MutableLiveData<>();

    public ProfileViewModel() {
        loadUserName();
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    private void loadUserName() {
        userName.setValue("探索者");
    }
}
