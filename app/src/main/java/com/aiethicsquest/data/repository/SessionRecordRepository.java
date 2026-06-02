package com.aiethicsquest.data.repository;

import androidx.lifecycle.LiveData;

import com.aiethicsquest.data.local.AppDatabase;
import com.aiethicsquest.data.local.SessionRecordDao;
import com.aiethicsquest.data.model.SessionRecord;

import java.util.List;

/**
 * 挑战轮次记录仓库，封装对 {@link SessionRecordDao} 的访问.
 */
public class SessionRecordRepository {

    private final SessionRecordDao dao;

    public SessionRecordRepository(AppDatabase database) {
        this.dao = database.sessionRecordDao();
    }

    /**
     * 插入一条轮次记录（在后台线程执行）.
     */
    public void insert(SessionRecord record) {
        AppDatabase.DB_EXECUTOR.execute(() -> dao.insert(record));
    }

    /**
     * 获取所有轮次记录（LiveData，按时间倒序）.
     */
    public LiveData<List<SessionRecord>> getAllRecords() {
        return dao.getAllRecords();
    }

    /**
     * 获取轮次记录总数（LiveData）.
     */
    public LiveData<Integer> getRecordCount() {
        return dao.getRecordCount();
    }

    /**
     * 清除所有轮次记录（在后台线程执行）.
     */
    public void clearAll() {
        AppDatabase.DB_EXECUTOR.execute(dao::clearAll);
    }
}

