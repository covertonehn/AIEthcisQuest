package com.aiethicsquest.data.repository;

import androidx.lifecycle.LiveData;

import com.aiethicsquest.data.local.AppDatabase;
import com.aiethicsquest.data.local.VideoSessionRecordDao;
import com.aiethicsquest.data.model.VideoSessionRecord;

import java.util.List;

/**
 * 视频挑战轮次记录 Repository，封装对 VideoSessionRecordDao 的操作.
 */
public class VideoSessionRecordRepository {

    private final VideoSessionRecordDao dao;

    public VideoSessionRecordRepository(AppDatabase db) {
        this.dao = db.videoSessionRecordDao();
    }

    /**
     * 插入一条轮次记录（在后台线程执行）.
     *
     * @param record 轮次记录
     */
    public void insert(VideoSessionRecord record) {
        AppDatabase.DB_EXECUTOR.execute(() -> dao.insert(record));
    }

    /**
     * 获取所有轮次记录（LiveData）.
     */
    public LiveData<List<VideoSessionRecord>> getAllRecords() {
        return dao.getAllRecords();
    }

    /**
     * 获取轮次记录总数（LiveData）.
     */
    public LiveData<Integer> getRecordCount() {
        return dao.getRecordCount();
    }
}

