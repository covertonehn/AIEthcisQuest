package com.aiethicsquest.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aiethicsquest.data.model.SessionRecord;

import java.util.List;

/**
 * 挑战轮次记录数据访问对象（DAO）.
 */
@Dao
public interface SessionRecordDao {

    /**
     * 插入一条轮次记录.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SessionRecord record);

    /**
     * 获取所有轮次记录，按完成时间倒序排列（最新的在前）.
     *
     * @return 轮次记录列表 LiveData
     */
    @Query("SELECT * FROM session_records ORDER BY finished_at DESC")
    LiveData<List<SessionRecord>> getAllRecords();

    /**
     * 获取轮次记录总数.
     */
    @Query("SELECT COUNT(*) FROM session_records")
    LiveData<Integer> getRecordCount();

    /**
     * 清除所有轮次记录.
     */
    @Query("DELETE FROM session_records")
    void clearAll();
}

