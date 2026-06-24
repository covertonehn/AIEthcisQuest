package com.aiethicsquest.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 视频挑战错题记录实体类，记录用户在视频挑战中答错的每一道题目信息.
 */
@Entity(
        tableName = "video_wrong_answers",
        foreignKeys = @ForeignKey(
                entity = VideoQuestion.class,
                parentColumns = "id",
                childColumns = "question_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(value = "question_id")
)
public class VideoWrongAnswer {

    @PrimaryKey(autoGenerate = true)
    private int id;

    /** 关联的题目 ID. */
    @ColumnInfo(name = "question_id")
    private int questionId;

    /** 用户点击的一侧（0=上方, 1=下方）. */
    @ColumnInfo(name = "user_choice")
    private int userChoice;

    /** 正确答案（AI 所在侧，0=上方, 1=下方）. */
    @ColumnInfo(name = "correct_answer")
    private int correctAnswer;

    /** 答题时间戳（毫秒）. */
    @ColumnInfo(name = "answered_at")
    private long answeredAt;

    /** 视频编号（文件名数字部分），用于显示"第 N 题". */
    @ColumnInfo(name = "video_index")
    private int videoIndex;

    /** AI 视频完整文件名（含扩展名），如 "1.mp4". */
    @ColumnInfo(name = "ai_file_name")
    private String aiFileName;

    /** 真实视频完整文件名（含扩展名），如 "1.mp4". */
    @ColumnInfo(name = "real_file_name")
    private String realFileName;

    /** 题目描述（冗余）. */
    @ColumnInfo(name = "description")
    private String description;

    // ---------- 构造器 ----------

    public VideoWrongAnswer() {
    }

    /**
     * 完整构造器.
     *
     * <p>Room 使用无参构造器，此构造器供业务代码使用。</p>
     */
    @Ignore
    public VideoWrongAnswer(int questionId, int userChoice, int correctAnswer,
                            long answeredAt, int videoIndex,
                            String aiFileName, String realFileName, String description) {
        this.questionId = questionId;
        this.userChoice = userChoice;
        this.correctAnswer = correctAnswer;
        this.answeredAt = answeredAt;
        this.videoIndex = videoIndex;
        this.aiFileName = aiFileName;
        this.realFileName = realFileName;
        this.description = description;
    }

    // ---------- 便捷路径方法 ----------

    /**
     * 获取上方视频的 assets 路径.
     */
    public String getTopVideoPath() {
        if (correctAnswer == 0) {
            return "ai_videos/" + aiFileName;
        } else {
            return "real_videos/" + realFileName;
        }
    }

    /**
     * 获取下方视频的 assets 路径.
     */
    public String getBottomVideoPath() {
        if (correctAnswer == 1) {
            return "ai_videos/" + aiFileName;
        } else {
            return "real_videos/" + realFileName;
        }
    }

    // ---------- Getter / Setter ----------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public int getUserChoice() { return userChoice; }
    public void setUserChoice(int userChoice) { this.userChoice = userChoice; }

    public int getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(int correctAnswer) { this.correctAnswer = correctAnswer; }

    public long getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(long answeredAt) { this.answeredAt = answeredAt; }

    public int getVideoIndex() { return videoIndex; }
    public void setVideoIndex(int videoIndex) { this.videoIndex = videoIndex; }

    public String getAiFileName() { return aiFileName; }
    public void setAiFileName(String aiFileName) { this.aiFileName = aiFileName; }

    public String getRealFileName() { return realFileName; }
    public void setRealFileName(String realFileName) { this.realFileName = realFileName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

