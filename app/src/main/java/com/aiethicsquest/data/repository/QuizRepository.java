package com.aiethicsquest.data.repository;

import androidx.lifecycle.LiveData;

import com.aiethicsquest.data.local.AppDatabase;
import com.aiethicsquest.data.local.QuizQuestionDao;
import com.aiethicsquest.data.model.QuizQuestion;

import java.util.List;

/**
 * 题目数据仓库，封装对 {@link QuizQuestionDao} 的访问.
 */
public class QuizRepository {

    private final QuizQuestionDao dao;

    public QuizRepository(AppDatabase database) {
        this.dao = database.quizQuestionDao();
    }

    /**
     * 获取所有题目（LiveData）.
     */
    public LiveData<List<QuizQuestion>> getAllQuestions() {
        return dao.getAllQuestions();
    }

    /**
     * 随机获取指定数量的【未做过】题目（后台线程执行）.
     *
     * @param limit 获取数量
     * @return 随机未做题目列表（可能少于 limit）
     */
    public List<QuizQuestion> getRandomUndoneQuestions(int limit) {
        return dao.getRandomUndoneQuestions(limit);
    }

    /**
     * 获取题目总数（后台线程执行）.
     */
    public int getCount() {
        return dao.getCount();
    }

    /**
     * 获取未做过的题目数量（后台线程执行）.
     */
    public int getUndoneCount() {
        return dao.getUndoneCount();
    }

    /**
     * 获取未做过的题目数量（LiveData，主线程可用）.
     */
    public LiveData<Integer> getUndoneCountLive() {
        return dao.getUndoneCountLive();
    }

    /**
     * 将指定 ID 列表的题目标记为已做（后台线程执行）.
     *
     * @param ids 题目 ID 列表
     */
    public void markAsDone(List<Integer> ids) {
        AppDatabase.DB_EXECUTOR.execute(() -> dao.markAsDone(ids));
    }

    /**
     * 重置所有题目为未做状态（后台线程执行）.
     */
    public void resetAllDone() {
        AppDatabase.DB_EXECUTOR.execute(dao::resetAllDone);
    }
}

