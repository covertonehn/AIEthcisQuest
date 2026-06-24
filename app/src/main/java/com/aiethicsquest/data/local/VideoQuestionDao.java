package com.aiethicsquest.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aiethicsquest.data.model.VideoQuestion;

import java.util.List;

/**
 * 视频题目数据访问对象（DAO）.
 */
@Dao
public interface VideoQuestionDao {

    /**
     * 插入题目列表，冲突时忽略（避免重复预填充）.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<VideoQuestion> questions);

    /**
     * 随机获取指定数量的【未做过】题目.
     *
     * @param limit 获取数量
     * @return 随机未做题目列表
     */
    @Query("SELECT * FROM video_questions WHERE is_done = 0 ORDER BY RANDOM() LIMIT :limit")
    List<VideoQuestion> getRandomUndoneQuestions(int limit);

    /**
     * 获取【未做过】的题目数量.
     */
    @Query("SELECT COUNT(*) FROM video_questions WHERE is_done = 0")
    int getUndoneCount();

    /**
     * 获取题目总数量.
     */
    @Query("SELECT COUNT(*) FROM video_questions")
    int getCount();

    /**
     * 获取【未做过】的题目数量（LiveData，用于 UI 实时刷新）.
     */
    @Query("SELECT COUNT(*) FROM video_questions WHERE is_done = 0")
    LiveData<Integer> getUndoneCountLive();

    /**
     * 将指定 ID 列表的题目标记为已做.
     *
     * @param ids 题目 ID 列表
     */
    @Query("UPDATE video_questions SET is_done = 1 WHERE id IN (:ids)")
    void markAsDone(List<Integer> ids);

    /**
     * 重置所有题目为未做（清空 is_done 标记）.
     */
    @Query("UPDATE video_questions SET is_done = 0")
    void resetAllDone();

    /**
     * 根据 ID 获取单道题目.
     */
    @Query("SELECT * FROM video_questions WHERE id = :id LIMIT 1")
    VideoQuestion getById(int id);
}

