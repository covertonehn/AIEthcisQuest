package com.aiethicsquest.data.repository;

import androidx.lifecycle.LiveData;

import com.aiethicsquest.data.local.AppDatabase;
import com.aiethicsquest.data.local.VideoQuestionDao;
import com.aiethicsquest.data.model.VideoQuestion;

import java.util.List;

/**
 * 视频题目 Repository，封装对 VideoQuestionDao 的操作.
 *
 * <p>ViewModel 通过此类获取题目数据，不直接调用 DAO。</p>
 */
public class VideoQuizRepository {

    private final VideoQuestionDao dao;

    public VideoQuizRepository(AppDatabase db) {
        this.dao = db.videoQuestionDao();
    }

    /**
     * 随机获取指定数量未做过的题目.
     *
     * @param limit 数量上限
     * @return 题目列表
     */
    public List<VideoQuestion> getRandomUndoneQuestions(int limit) {
        return dao.getRandomUndoneQuestions(limit);
    }

    /**
     * 获取题目总数量.
     */
    public int getCount() {
        return dao.getCount();
    }

    /**
     * 获取未做过的题目数量（实时 LiveData）.
     */
    public LiveData<Integer> getUndoneCountLive() {
        return dao.getUndoneCountLive();
    }

    /**
     * 将指定 ID 列表的题目标记为已做.
     *
     * @param ids 题目 ID 列表
     */
    public void markAsDone(List<Integer> ids) {
        dao.markAsDone(ids);
    }

    /**
     * 重置所有题目为未做状态.
     */
    public void resetAllDone() {
        AppDatabase.DB_EXECUTOR.execute(dao::resetAllDone);
    }
}

