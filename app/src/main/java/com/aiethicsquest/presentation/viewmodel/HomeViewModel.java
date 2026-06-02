package com.aiethicsquest.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {
    private final MutableLiveData<Integer> count = new MutableLiveData<>(0);

    public LiveData<Integer> getCount() {
        return count;
    }

    public void increment(){
        Integer current = count.getValue();
        count.setValue(current != null ? current + 1 : 1);
    }

    public void reset(){
        count.setValue(0);
    }
}
