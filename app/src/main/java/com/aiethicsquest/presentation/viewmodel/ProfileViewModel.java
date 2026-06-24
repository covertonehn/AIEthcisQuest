package com.aiethicsquest.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aiethicsquest.data.local.AppDatabase;
import com.aiethicsquest.data.model.SessionRecord;
import com.aiethicsquest.data.model.VideoSessionRecord;
import com.aiethicsquest.data.model.VideoWrongAnswer;
import com.aiethicsquest.data.model.WrongAnswer;
import com.aiethicsquest.data.repository.SessionRecordRepository;
import com.aiethicsquest.data.repository.VideoSessionRecordRepository;
import com.aiethicsquest.data.repository.VideoWrongAnswerRepository;
import com.aiethicsquest.data.repository.WrongAnswerRepository;

import java.util.List;

/**
 * 个人中心页面 ViewModel.
 *
 * <p>提供用户昵称、识图挑战/视频挑战的错题数量、挑战历史记录等数据。</p>
 */
public class ProfileViewModel extends AndroidViewModel {

    private final WrongAnswerRepository wrongAnswerRepository;
    private final SessionRecordRepository sessionRecordRepository;
    private final VideoWrongAnswerRepository videoWrongAnswerRepository;
    private final VideoSessionRecordRepository videoSessionRecordRepository;

    private final MutableLiveData<String> userName = new MutableLiveData<>();

    // ★ 缓存 LiveData 实例：避免每次调用 Repository 返回新对象，
    //   保证多个 Fragment 复用同一个 LiveData，防止重新订阅时数据闪失。
    private final LiveData<List<WrongAnswer>> wrongAnswers;
    private final LiveData<Integer> wrongAnswerCount;
    private final LiveData<List<SessionRecord>> sessionRecords;
    private final LiveData<List<VideoWrongAnswer>> videoWrongAnswers;
    private final LiveData<Integer> videoWrongAnswerCount;
    private final LiveData<List<VideoSessionRecord>> videoSessionRecords;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        wrongAnswerRepository = new WrongAnswerRepository(db);
        sessionRecordRepository = new SessionRecordRepository(db);
        videoWrongAnswerRepository = new VideoWrongAnswerRepository(db);
        videoSessionRecordRepository = new VideoSessionRecordRepository(db);

        // 在构造时一次性获取并缓存所有 LiveData
        wrongAnswers          = wrongAnswerRepository.getAllWrongAnswers();
        wrongAnswerCount      = wrongAnswerRepository.getWrongAnswerCount();
        sessionRecords        = sessionRecordRepository.getAllRecords();
        videoWrongAnswers     = videoWrongAnswerRepository.getAllWrongAnswers();
        videoWrongAnswerCount = videoWrongAnswerRepository.getWrongAnswerCount();
        videoSessionRecords   = videoSessionRecordRepository.getAllRecords();

        loadUserName();
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    // ===== 识图挑战 =====

    /**
     * 获取识图挑战所有错题记录（LiveData，按时间倒序）.
     */
    public LiveData<List<WrongAnswer>> getWrongAnswers() {
        return wrongAnswers;
    }

    /**
     * 获取识图挑战错题总数（LiveData）.
     */
    public LiveData<Integer> getWrongAnswerCount() {
        return wrongAnswerCount;
    }

    /**
     * 获取识图挑战所有历史记录（LiveData，按完成时间倒序）.
     */
    public LiveData<List<SessionRecord>> getSessionRecords() {
        return sessionRecords;
    }

    /**
     * 清除识图挑战所有错题记录.
     */
    public void clearAllWrongAnswers() {
        wrongAnswerRepository.clearAll();
    }

    /**
     * 删除识图挑战单条错题记录.
     */
    public void deleteWrongAnswer(int id) {
        wrongAnswerRepository.deleteById(id);
    }

    // ===== 视频挑战 =====

    /**
     * 获取视频挑战所有错题记录（LiveData，按时间倒序）.
     */
    public LiveData<List<VideoWrongAnswer>> getVideoWrongAnswers() {
        return videoWrongAnswers;
    }

    /**
     * 获取视频挑战错题总数（LiveData）.
     */
    public LiveData<Integer> getVideoWrongAnswerCount() {
        return videoWrongAnswerCount;
    }

    /**
     * 获取视频挑战所有历史记录（LiveData，按完成时间倒序）.
     */
    public LiveData<List<VideoSessionRecord>> getVideoSessionRecords() {
        return videoSessionRecords;
    }

    /**
     * 清除视频挑战所有错题记录.
     */
    public void clearAllVideoWrongAnswers() {
        videoWrongAnswerRepository.clearAll();
    }

    private void loadUserName() {
        userName.setValue("探索者");
    }
}
