package com.aiethicsquest.data.repository;

import androidx.lifecycle.LiveData;

import com.aiethicsquest.data.local.AppDatabase;
import com.aiethicsquest.data.local.VideoWrongAnswerDao;
import com.aiethicsquest.data.model.VideoWrongAnswer;

import java.util.List;

/**
 * 视频挑战错题记录 Repository，封装对 VideoWrongAnswerDao 的操作.
 */
public class VideoWrongAnswerRepository {

    private final VideoWrongAnswerDao dao;

    public VideoWrongAnswerRepository(AppDatabase db) {
        this.dao = db.videoWrongAnswerDao();
    }

    /**
     * 插入一条错题记录（在后台线程执行）.
     *
     * @param wrongAnswer 错题记录
     */
    public void insert(VideoWrongAnswer wrongAnswer) {
        AppDatabase.DB_EXECUTOR.execute(() -> dao.insert(wrongAnswer));
    }

    /**
     * 获取所有错题记录（LiveData）.
     */
    public LiveData<List<VideoWrongAnswer>> getAllWrongAnswers() {
        return dao.getAllWrongAnswers();
    }

    /**
     * 获取错题总数量（LiveData）.
     */
    public LiveData<Integer> getWrongAnswerCount() {
        return dao.getWrongAnswerCount();
    }

    /**
     * 清除所有错题记录（在后台线程执行）.
     */
    public void clearAll() {
        AppDatabase.DB_EXECUTOR.execute(dao::clearAll);
    }
}

