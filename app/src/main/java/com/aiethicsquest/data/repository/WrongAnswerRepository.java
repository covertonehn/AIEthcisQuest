package com.aiethicsquest.data.repository;

import androidx.lifecycle.LiveData;

import com.aiethicsquest.data.local.AppDatabase;
import com.aiethicsquest.data.local.WrongAnswerDao;
import com.aiethicsquest.data.model.WrongAnswer;

import java.util.List;

/**
 * 错题记录仓库，封装对 {@link WrongAnswerDao} 的访问.
 *
 * <p>ViewModel 通过此类操作错题数据，不直接调用 DAO。</p>
 */
public class WrongAnswerRepository {

    private final WrongAnswerDao dao;

    /**
     * 构造器.
     *
     * @param database AppDatabase 实例
     */
    public WrongAnswerRepository(AppDatabase database) {
        this.dao = database.wrongAnswerDao();
    }

    /**
     * 插入一条错题记录（在后台线程执行）.
     *
     * @param wrongAnswer 错题记录
     */
    public void insert(WrongAnswer wrongAnswer) {
        AppDatabase.DB_EXECUTOR.execute(() -> dao.insert(wrongAnswer));
    }

    /**
     * 获取所有错题记录（LiveData，按时间倒序）.
     *
     * @return 错题记录列表 LiveData
     */
    public LiveData<List<WrongAnswer>> getAllWrongAnswers() {
        return dao.getAllWrongAnswers();
    }

    /**
     * 获取错题总数（LiveData）.
     *
     * @return 错题数量 LiveData
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

    /**
     * 根据 ID 删除单条错题记录（在后台线程执行）.
     *
     * @param id 错题记录 ID
     */
    public void deleteById(int id) {
        AppDatabase.DB_EXECUTOR.execute(() -> dao.deleteById(id));
    }
}

