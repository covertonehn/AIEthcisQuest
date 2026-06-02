package com.aiethicsquest.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aiethicsquest.data.local.AppDatabase;
import com.aiethicsquest.data.model.SessionRecord;
import com.aiethicsquest.data.model.WrongAnswer;
import com.aiethicsquest.data.repository.SessionRecordRepository;
import com.aiethicsquest.data.repository.WrongAnswerRepository;

import java.util.List;

/**
 * 个人中心页面 ViewModel.
 *
 * <p>提供用户昵称、错题数量、挑战历史记录等数据。</p>
 */
public class ProfileViewModel extends AndroidViewModel {

    private final WrongAnswerRepository wrongAnswerRepository;
    private final SessionRecordRepository sessionRecordRepository;

    private final MutableLiveData<String> userName = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        wrongAnswerRepository = new WrongAnswerRepository(db);
        sessionRecordRepository = new SessionRecordRepository(db);
        loadUserName();
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    /**
     * 获取所有错题记录（LiveData，按时间倒序）.
     */
    public LiveData<List<WrongAnswer>> getWrongAnswers() {
        return wrongAnswerRepository.getAllWrongAnswers();
    }

    /**
     * 获取错题总数（LiveData）.
     */
    public LiveData<Integer> getWrongAnswerCount() {
        return wrongAnswerRepository.getWrongAnswerCount();
    }

    /**
     * 获取所有挑战历史记录（LiveData，按完成时间倒序）.
     */
    public LiveData<List<SessionRecord>> getSessionRecords() {
        return sessionRecordRepository.getAllRecords();
    }

    /**
     * 清除所有错题记录.
     */
    public void clearAllWrongAnswers() {
        wrongAnswerRepository.clearAll();
    }

    /**
     * 删除单条错题记录.
     */
    public void deleteWrongAnswer(int id) {
        wrongAnswerRepository.deleteById(id);
    }

    private void loadUserName() {
        userName.setValue("探索者");
    }
}
