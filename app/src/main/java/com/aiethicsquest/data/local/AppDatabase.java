package com.aiethicsquest.data.local;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.aiethicsquest.data.model.QuizQuestion;
import com.aiethicsquest.data.model.SessionRecord;
import com.aiethicsquest.data.model.VideoQuestion;
import com.aiethicsquest.data.model.VideoSessionRecord;
import com.aiethicsquest.data.model.VideoWrongAnswer;
import com.aiethicsquest.data.model.WrongAnswer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room 数据库单例类.
 *
 * <p>包含以下表：</p>
 * <ul>
 *     <li>quiz_questions — 识图挑战题目</li>
 *     <li>wrong_answers — 识图挑战错题记录</li>
 *     <li>session_records — 识图挑战轮次记录</li>
 *     <li>video_questions — 视频挑战题目</li>
 *     <li>video_wrong_answers — 视频挑战错题记录</li>
 *     <li>video_session_records — 视频挑战轮次记录</li>
 * </ul>
 *
 * <p>题目数据来源：扫描 assets/ai_images/、assets/real_images/、
 * assets/ai_videos/、assets/real_videos/ 目录，取各类型目录中数字编号的交集。</p>
 *
 * <p>升级策略为 fallbackToDestructiveMigration，开发阶段直接清空重建。</p>
 */
@Database(
        entities = {
                QuizQuestion.class,
                WrongAnswer.class,
                SessionRecord.class,
                VideoQuestion.class,
                VideoWrongAnswer.class,
                VideoSessionRecord.class
        },
        version = 6,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static final String DB_NAME = "ai_ethics_quest.db";

    /** AI 图片所在 assets 子目录. */
    public static final String AI_IMAGES_DIR = "ai_images";
    /** 真实图片所在 assets 子目录. */
    public static final String REAL_IMAGES_DIR = "real_images";
    /** AI 视频所在 assets 子目录. */
    public static final String AI_VIDEOS_DIR = "ai_videos";
    /** 真实视频所在 assets 子目录. */
    public static final String REAL_VIDEOS_DIR = "real_videos";

    private static volatile AppDatabase instance;
    private static Context appContext;

    /** 数据库操作线程池（4线程）. */
    public static final ExecutorService DB_EXECUTOR = Executors.newFixedThreadPool(4);

    public abstract QuizQuestionDao quizQuestionDao();

    public abstract WrongAnswerDao wrongAnswerDao();

    public abstract SessionRecordDao sessionRecordDao();

    public abstract VideoQuestionDao videoQuestionDao();

    public abstract VideoWrongAnswerDao videoWrongAnswerDao();

    public abstract VideoSessionRecordDao videoSessionRecordDao();

    /**
     * 获取数据库单例（线程安全）.
     *
     * @param context Application Context
     * @return AppDatabase 单例
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    appContext = context.getApplicationContext();
                    instance = Room.databaseBuilder(
                                    appContext,
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            // 数据模型结构变更时直接清空重建，开发阶段使用
                            .fallbackToDestructiveMigration()
                            .addCallback(new PrepopulateCallback())
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * 数据库首次创建时预填充题目数据的回调.
     *
     * <p>同时预填充识图挑战（图片）和视频挑战（视频）的题目。</p>
     *
     * <p>注意：Room 的 onCreate 在 build() 内部同步调用，此时 instance 字段尚未赋值，
     * 因此不能在 onCreate 中直接引用 instance。正确做法是在 onOpen 中执行预填充，
     * onOpen 在每次打开数据库时触发，通过 getCount() == 0 的幂等判断保证只执行一次。</p>
     */
    private static class PrepopulateCallback extends Callback {

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            DB_EXECUTOR.execute(() -> {
                if (instance != null && appContext != null) {
                    prepopulateImageQuestions(instance.quizQuestionDao(), appContext);
                    prepopulateVideoQuestions(instance.videoQuestionDao(), appContext);
                }
            });
        }

        // ---------- 识图挑战题目预填充 ----------

        /**
         * 扫描 assets/ai_images/ 和 assets/real_images/，生成识图挑战题目列表.
         *
         * <p>支持 .jpg、.jpeg、.png 格式，文件名必须是纯数字，如 1.png、2.jpg。</p>
         */
        private void prepopulateImageQuestions(QuizQuestionDao dao, Context context) {
            if (dao.getCount() > 0) {
                return;
            }

            Map<Integer, String> aiFiles = listFiles(context.getAssets(), AI_IMAGES_DIR,
                    new String[]{"jpg", "jpeg", "png"});
            Map<Integer, String> realFiles = listFiles(context.getAssets(), REAL_IMAGES_DIR,
                    new String[]{"jpg", "jpeg", "png"});

            if (aiFiles.isEmpty() || realFiles.isEmpty()) {
                Log.w(TAG, "未找到图片，请在 assets/ai_images/ 和 assets/real_images/ 中放置数字命名的图片");
                return;
            }

            Random random = new Random();
            List<QuizQuestion> questions = new ArrayList<>();

            for (Map.Entry<Integer, String> aiEntry : aiFiles.entrySet()) {
                int index = aiEntry.getKey();
                if (!realFiles.containsKey(index)) continue;
                int aiSide = random.nextInt(2);
                questions.add(new QuizQuestion(
                        index,
                        aiEntry.getValue(),
                        realFiles.get(index),
                        aiSide,
                        "这两张图片，哪张是 AI 生成的？"
                ));
            }

            if (questions.isEmpty()) {
                Log.w(TAG, "ai_images 和 real_images 中没有同编号的图片对");
                return;
            }

            dao.insertAll(questions);
            Log.d(TAG, "识图挑战题目预填充完成，共 " + questions.size() + " 道题");
        }

        // ---------- 视频挑战题目预填充 ----------

        /**
         * 扫描 assets/ai_videos/ 和 assets/real_videos/，生成视频挑战题目列表.
         *
         * <p>支持 .mp4、.webm、.3gp 格式，文件名必须是纯数字，如 1.mp4。</p>
         */
        private void prepopulateVideoQuestions(VideoQuestionDao dao, Context context) {
            if (dao.getCount() > 0) {
                return;
            }

            Map<Integer, String> aiFiles = listFiles(context.getAssets(), AI_VIDEOS_DIR,
                    new String[]{"mp4", "webm", "3gp"});
            Map<Integer, String> realFiles = listFiles(context.getAssets(), REAL_VIDEOS_DIR,
                    new String[]{"mp4", "webm", "3gp"});

            if (aiFiles.isEmpty() || realFiles.isEmpty()) {
                Log.w(TAG, "未找到视频，请在 assets/ai_videos/ 和 assets/real_videos/ 中放置数字命名的视频");
                return;
            }

            Random random = new Random();
            List<VideoQuestion> questions = new ArrayList<>();

            for (Map.Entry<Integer, String> aiEntry : aiFiles.entrySet()) {
                int index = aiEntry.getKey();
                if (!realFiles.containsKey(index)) continue;
                int aiSide = random.nextInt(2);
                questions.add(new VideoQuestion(
                        index,
                        aiEntry.getValue(),
                        realFiles.get(index),
                        aiSide,
                        "这两段视频，哪段是 AI 生成的？"
                ));
            }

            if (questions.isEmpty()) {
                Log.w(TAG, "ai_videos 和 real_videos 中没有同编号的视频对");
                return;
            }

            dao.insertAll(questions);
            Log.d(TAG, "视频挑战题目预填充完成，共 " + questions.size() + " 道题");
        }

        // ---------- 通用工具方法 ----------

        /**
         * 列出指定 assets 目录中所有匹配扩展名的文件，返回 {数字编号 -> 完整文件名} 的 Map.
         *
         * <p>文件名必须是纯数字（含扩展名），如 1.mp4、2.png。
         * 若同一数字有多个格式，取遍历到的第一个。</p>
         *
         * @param assets     AssetManager
         * @param directory  assets 子目录名
         * @param extensions 允许的文件扩展名（小写）
         * @return {编号 -> 文件名} Map
         */
        private Map<Integer, String> listFiles(AssetManager assets, String directory,
                                               String[] extensions) {
            Map<Integer, String> result = new HashMap<>();
            try {
                String[] files = assets.list(directory);
                if (files == null) return result;
                for (String file : files) {
                    int dotIndex = file.lastIndexOf('.');
                    if (dotIndex <= 0) continue;
                    String ext = file.substring(dotIndex + 1).toLowerCase();
                    boolean matched = false;
                    for (String e : extensions) {
                        if (e.equals(ext)) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) continue;
                    String name = file.substring(0, dotIndex);
                    try {
                        int index = Integer.parseInt(name);
                        result.putIfAbsent(index, file);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "跳过非数字命名文件：" + directory + "/" + file);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "读取 assets/" + directory + " 失败", e);
            }
            return result;
        }
    }
}

