package com.aiethicsquest.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 题目实体类，代表一道"AI生成 vs 真实拍摄"的图片对比题目.
 *
 * <p>图片存储方式：</p>
 * <ul>
 *     <li>AI 图片放在 assets/ai_images/ 目录，以数字命名，如 1.png、1.jpg</li>
 *     <li>真实图片放在 assets/real_images/ 目录，同名文件视为同一题目</li>
 *     <li>{@link #imageIndex} 记录题目编号（文件名中的数字）</li>
 *     <li>{@link #aiFileName} 存储 AI 图完整文件名（含扩展名），如 "1.png"</li>
 *     <li>{@link #realFileName} 存储真实图完整文件名（含扩展名），如 "1.png"</li>
 *     <li>{@link #aiSide} 由系统随机决定，0=AI图在左侧，1=AI图在右侧</li>
 * </ul>
 */
@Entity(tableName = "quiz_questions")
public class QuizQuestion {

    @PrimaryKey(autoGenerate = true)
    private int id;

    /** 图片编号（文件名数字部分），用于展示"第 N 题". */
    @ColumnInfo(name = "image_index")
    private int imageIndex;

    /** AI 图完整文件名（含扩展名），如 "1.png" 或 "1.jpg". */
    @ColumnInfo(name = "ai_file_name")
    private String aiFileName;

    /** 真实图完整文件名（含扩展名），如 "1.png" 或 "1.jpg". */
    @ColumnInfo(name = "real_file_name")
    private String realFileName;

    /**
     * AI 图所在侧（系统随机分配）.
     * 0 = AI图在左侧，1 = AI图在右侧
     */
    @ColumnInfo(name = "ai_side")
    private int aiSide;

    /** 题目描述文字. */
    @ColumnInfo(name = "description")
    private String description;

    /**
     * 是否已经在某轮挑战中出现过（0=未做, 1=已做）.
     *
     * <p>每轮出题后标记为已做，题库全部做完后可重置。</p>
     */
    @ColumnInfo(name = "is_done", defaultValue = "0")
    private int isDone;

    // ---------- 构造器 ----------

    public QuizQuestion() {
    }

    /**
     * 完整构造器.
     *
     * <p>Room 使用无参构造器，此构造器供业务代码构造对象时使用。</p>
     *
     * @param imageIndex   图片编号
     * @param aiFileName   AI 图完整文件名（含扩展名）
     * @param realFileName 真实图完整文件名（含扩展名）
     * @param aiSide       AI图所在侧（0=左, 1=右）
     * @param description  题目描述
     */
    @Ignore
    public QuizQuestion(int imageIndex, String aiFileName, String realFileName,
                        int aiSide, String description) {
        this.imageIndex = imageIndex;
        this.aiFileName = aiFileName;
        this.realFileName = realFileName;
        this.aiSide = aiSide;
        this.description = description;
        this.isDone = 0;
    }

    // ---------- 便捷路径方法 ----------

    /**
     * 获取左侧图片的 assets 路径.
     *
     * @return assets 相对路径，如 "ai_images/1.png"
     */
    public String getLeftImagePath() {
        if (aiSide == 0) {
            return "ai_images/" + aiFileName;
        } else {
            return "real_images/" + realFileName;
        }
    }

    /**
     * 获取右侧图片的 assets 路径.
     *
     * @return assets 相对路径
     */
    public String getRightImagePath() {
        if (aiSide == 1) {
            return "ai_images/" + aiFileName;
        } else {
            return "real_images/" + realFileName;
        }
    }

    // ---------- Getter / Setter ----------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getImageIndex() { return imageIndex; }
    public void setImageIndex(int imageIndex) { this.imageIndex = imageIndex; }

    public String getAiFileName() { return aiFileName; }
    public void setAiFileName(String aiFileName) { this.aiFileName = aiFileName; }

    public String getRealFileName() { return realFileName; }
    public void setRealFileName(String realFileName) { this.realFileName = realFileName; }

    public int getAiSide() { return aiSide; }
    public void setAiSide(int aiSide) { this.aiSide = aiSide; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getIsDone() { return isDone; }
    public void setIsDone(int isDone) { this.isDone = isDone; }
}

