package com.aiethicsquest.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 视频挑战题目实体类，代表一道"AI生成视频 vs 真实视频"的对比题目.
 *
 * <p>视频存储方式：</p>
 * <ul>
 *     <li>AI 视频放在 assets/ai_videos/ 目录，以数字命名，如 1.mp4</li>
 *     <li>真实视频放在 assets/real_videos/ 目录，同名文件视为同一题目</li>
 *     <li>{@link #videoIndex} 记录题目编号（文件名中的数字）</li>
 *     <li>{@link #aiFileName} 存储 AI 视频完整文件名（含扩展名），如 "1.mp4"</li>
 *     <li>{@link #realFileName} 存储真实视频完整文件名（含扩展名），如 "1.mp4"</li>
 *     <li>{@link #aiSide} 由系统随机决定，0=AI视频在上方，1=AI视频在下方</li>
 * </ul>
 */
@Entity(tableName = "video_questions")
public class VideoQuestion {

    @PrimaryKey(autoGenerate = true)
    private int id;

    /** 视频编号（文件名数字部分），用于展示"第 N 题". */
    @ColumnInfo(name = "video_index")
    private int videoIndex;

    /** AI 视频完整文件名（含扩展名），如 "1.mp4". */
    @ColumnInfo(name = "ai_file_name")
    private String aiFileName;

    /** 真实视频完整文件名（含扩展名），如 "1.mp4". */
    @ColumnInfo(name = "real_file_name")
    private String realFileName;

    /**
     * AI 视频所在侧（系统随机分配）.
     * 0 = AI视频在上方，1 = AI视频在下方
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

    public VideoQuestion() {
    }

    /**
     * 完整构造器.
     *
     * <p>Room 使用无参构造器，此构造器供业务代码构造对象时使用。</p>
     *
     * @param videoIndex   视频编号
     * @param aiFileName   AI 视频完整文件名（含扩展名）
     * @param realFileName 真实视频完整文件名（含扩展名）
     * @param aiSide       AI视频所在侧（0=上方, 1=下方）
     * @param description  题目描述
     */
    @Ignore
    public VideoQuestion(int videoIndex, String aiFileName, String realFileName,
                         int aiSide, String description) {
        this.videoIndex = videoIndex;
        this.aiFileName = aiFileName;
        this.realFileName = realFileName;
        this.aiSide = aiSide;
        this.description = description;
        this.isDone = 0;
    }

    // ---------- 便捷路径方法 ----------

    /**
     * 获取上方视频的 assets 路径.
     *
     * @return assets 相对路径，如 "ai_videos/1.mp4"
     */
    public String getTopVideoPath() {
        if (aiSide == 0) {
            return "ai_videos/" + aiFileName;
        } else {
            return "real_videos/" + realFileName;
        }
    }

    /**
     * 获取下方视频的 assets 路径.
     *
     * @return assets 相对路径
     */
    public String getBottomVideoPath() {
        if (aiSide == 1) {
            return "ai_videos/" + aiFileName;
        } else {
            return "real_videos/" + realFileName;
        }
    }

    // ---------- Getter / Setter ----------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVideoIndex() { return videoIndex; }
    public void setVideoIndex(int videoIndex) { this.videoIndex = videoIndex; }

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

