package com.aiethicsquest.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 挑战轮次记录实体类.
 *
 * <p>每完成一轮识图挑战，写入一条记录，包含：</p>
 * <ul>
 *     <li>完成时间戳</li>
 *     <li>本轮总题数</li>
 *     <li>答对题数</li>
 *     <li>所有出过的图片路径（逗号分隔，存上图路径）</li>
 * </ul>
 */
@Entity(tableName = "session_records")
public class SessionRecord {

    @PrimaryKey(autoGenerate = true)
    private int id;

    /** 完成时间戳（毫秒）. */
    @ColumnInfo(name = "finished_at")
    private long finishedAt;

    /** 本轮总题数. */
    @ColumnInfo(name = "total_count")
    private int totalCount;

    /** 本轮答对题数. */
    @ColumnInfo(name = "correct_count")
    private int correctCount;

    /**
     * 本轮出现的所有图片路径，以逗号分隔.
     * 格式：每题存两条路径（上图,下图），多题用"|"分组.
     * 例："ai_images/1.png,real_images/1.png|ai_images/2.png,real_images/2.png"
     */
    @ColumnInfo(name = "image_paths")
    private String imagePaths;

    /**
     * 每题的答题结果，用"|"分组，每题格式："aiSide:isCorrect".
     * aiSide: 0=AI在上图, 1=AI在下图.
     * isCorrect: 0=答错, 1=答对.
     * 例："0:1|1:0|0:1"
     */
    @ColumnInfo(name = "question_results")
    private String questionResults;

    // ---------- 构造器 ----------

    public SessionRecord() {
    }

    /**
     * 完整构造器（供业务代码使用）.
     */
    @Ignore
    public SessionRecord(long finishedAt, int totalCount, int correctCount,
                         String imagePaths, String questionResults) {
        this.finishedAt = finishedAt;
        this.totalCount = totalCount;
        this.correctCount = correctCount;
        this.imagePaths = imagePaths;
        this.questionResults = questionResults;
    }

    // ---------- 便捷方法 ----------

    /**
     * 获取正确率（0.0 ~ 1.0）.
     *
     * @return 正确率
     */
    public float getAccuracy() {
        if (totalCount == 0) return 0f;
        return (float) correctCount / totalCount;
    }

    // ---------- Getter / Setter ----------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getFinishedAt() { return finishedAt; }
    public void setFinishedAt(long finishedAt) { this.finishedAt = finishedAt; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }

    public String getImagePaths() { return imagePaths; }
    public void setImagePaths(String imagePaths) { this.imagePaths = imagePaths; }

    public String getQuestionResults() { return questionResults; }
    public void setQuestionResults(String questionResults) { this.questionResults = questionResults; }
}

