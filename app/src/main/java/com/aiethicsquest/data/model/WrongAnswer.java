package com.aiethicsquest.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 错题记录实体类，记录用户答错的每一道题目信息.
 */
@Entity(
        tableName = "wrong_answers",
        foreignKeys = @ForeignKey(
                entity = QuizQuestion.class,
                parentColumns = "id",
                childColumns = "question_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(value = "question_id")
)
public class WrongAnswer {

    @PrimaryKey(autoGenerate = true)
    private int id;

    /** 关联的题目 ID. */
    @ColumnInfo(name = "question_id")
    private int questionId;

    /** 用户点击的一侧（0=左, 1=右）. */
    @ColumnInfo(name = "user_choice")
    private int userChoice;

    /** 正确答案（AI 所在侧，0=左, 1=右）. */
    @ColumnInfo(name = "correct_answer")
    private int correctAnswer;

    /** 答题时间戳（毫秒）. */
    @ColumnInfo(name = "answered_at")
    private long answeredAt;

    /** 图片编号（文件名数字部分），用于显示"第 N 题". */
    @ColumnInfo(name = "image_index")
    private int imageIndex;

    /** AI 图完整文件名（含扩展名），如 "1.png". */
    @ColumnInfo(name = "ai_file_name")
    private String aiFileName;

    /** 真实图完整文件名（含扩展名），如 "1.png". */
    @ColumnInfo(name = "real_file_name")
    private String realFileName;

    /** 题目描述（冗余）. */
    @ColumnInfo(name = "description")
    private String description;

    // ---------- 构造器 ----------

    public WrongAnswer() {
    }

    /**
     * 完整构造器.
     *
     * <p>Room 使用无参构造器，此构造器供业务代码使用。</p>
     */
    @Ignore
    public WrongAnswer(int questionId, int userChoice, int correctAnswer,
                       long answeredAt, int imageIndex,
                       String aiFileName, String realFileName, String description) {
        this.questionId = questionId;
        this.userChoice = userChoice;
        this.correctAnswer = correctAnswer;
        this.answeredAt = answeredAt;
        this.imageIndex = imageIndex;
        this.aiFileName = aiFileName;
        this.realFileName = realFileName;
        this.description = description;
    }

    // ---------- 便捷路径方法 ----------

    /**
     * 获取左侧图片的 assets 路径.
     */
    public String getLeftImagePath() {
        if (correctAnswer == 0) {
            return "ai_images/" + aiFileName;
        } else {
            return "real_images/" + realFileName;
        }
    }

    /**
     * 获取右侧图片的 assets 路径.
     */
    public String getRightImagePath() {
        if (correctAnswer == 1) {
            return "ai_images/" + aiFileName;
        } else {
            return "real_images/" + realFileName;
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

    public int getImageIndex() { return imageIndex; }
    public void setImageIndex(int imageIndex) { this.imageIndex = imageIndex; }

    public String getAiFileName() { return aiFileName; }
    public void setAiFileName(String aiFileName) { this.aiFileName = aiFileName; }

    public String getRealFileName() { return realFileName; }
    public void setRealFileName(String realFileName) { this.realFileName = realFileName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

