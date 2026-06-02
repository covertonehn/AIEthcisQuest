package com.aiethicsquest.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aiethicsquest.data.model.WrongAnswer;

import java.util.List;

/**
 * 错题记录数据访问对象（DAO）.
 *
 * <p>提供对 wrong_answers 表的增删查操作。</p>
 */
@Dao
public interface WrongAnswerDao {

    /**
     * 插入一条错题记录.
     *
     * @param wrongAnswer 错题记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WrongAnswer wrongAnswer);

    /**
     * 获取所有错题记录，按答题时间倒序排列（最新的在前）.
     *
     * @return 错题记录列表 LiveData
     */
    @Query("SELECT * FROM wrong_answers ORDER BY answered_at DESC")
    LiveData<List<WrongAnswer>> getAllWrongAnswers();

    /**
     * 获取错题总数量.
     *
     * @return 错题数量 LiveData
     */
    @Query("SELECT COUNT(*) FROM wrong_answers")
    LiveData<Integer> getWrongAnswerCount();

    /**
     * 清除所有错题记录.
     */
    @Query("DELETE FROM wrong_answers")
    void clearAll();

    /**
     * 根据 ID 删除单条错题记录.
     *
     * @param id 错题记录 ID
     */
    @Query("DELETE FROM wrong_answers WHERE id = :id")
    void deleteById(int id);
}

