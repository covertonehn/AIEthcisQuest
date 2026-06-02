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
 * <p>包含 quiz_questions 和 wrong_answers 两张表。</p>
 *
 * <p>题目数据来源：扫描 assets/ai_images/ 和 assets/real_images/ 目录，
 * 取两个目录中数字编号的交集，每道题由系统随机决定 AI 图放左侧还是右侧。</p>
 *
 * <p>数据库版本升级到 2（数据模型结构变更：移除 category/image_left_res_name 等旧字段，
 * 改用 imageIndex）。升级策略为 fallbackToDestructiveMigration，即清空重建。</p>
 */
@Database(
        entities = {QuizQuestion.class, WrongAnswer.class, SessionRecord.class},
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static final String DB_NAME = "ai_ethics_quest.db";

    /** AI 图片所在 assets 子目录. */
    public static final String AI_IMAGES_DIR = "ai_images";
    /** 真实图片所在 assets 子目录. */
    public static final String REAL_IMAGES_DIR = "real_images";

    private static volatile AppDatabase instance;
    private static Context appContext;

    /** 数据库操作线程池（4线程）. */
    public static final ExecutorService DB_EXECUTOR = Executors.newFixedThreadPool(4);

    public abstract QuizQuestionDao quizQuestionDao();

    public abstract WrongAnswerDao wrongAnswerDao();

    public abstract SessionRecordDao sessionRecordDao();

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
     * <p>自动扫描 assets/ai_images/ 和 assets/real_images/ 目录，
     * 取两目录中文件名数字编号的交集，为每道题随机分配 AI 图的左右位置。</p>
     */
    private static class PrepopulateCallback extends Callback {

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            DB_EXECUTOR.execute(() -> {
                if (instance != null && appContext != null) {
                    prepopulateQuestions(instance.quizQuestionDao(), appContext);
                }
            });
        }

        /**
         * 扫描 assets 目录，生成题目列表并写入数据库.
         *
         * @param dao     题目 DAO
         * @param context Application Context
         */
        /**
         * 扫描 assets 目录，生成题目列表并写入数据库.
         *
         * <p>扫描 ai_images 和 real_images，取数字编号交集，
         * 存储完整文件名（含扩展名），支持 jpg/jpeg/png 混用。</p>
         */
        private void prepopulateQuestions(QuizQuestionDao dao, Context context) {
            if (dao.getCount() > 0) {
                return;
            }

            // key=数字编号, value=完整文件名（如 "1.png"）
            Map<Integer, String> aiFiles = listImageFiles(context.getAssets(), AI_IMAGES_DIR);
            Map<Integer, String> realFiles = listImageFiles(context.getAssets(), REAL_IMAGES_DIR);

            if (aiFiles.isEmpty() || realFiles.isEmpty()) {
                Log.w(TAG, "未找到图片，请在 assets/ai_images/ 和 assets/real_images/ 中放置数字命名的图片");
                return;
            }

            Random random = new Random();
            List<QuizQuestion> questions = new ArrayList<>();

            // 取两目录中数字编号的交集
            for (Map.Entry<Integer, String> aiEntry : aiFiles.entrySet()) {
                int index = aiEntry.getKey();
                if (!realFiles.containsKey(index)) {
                    continue; // real_images 中没有对应编号，跳过
                }
                int aiSide = random.nextInt(2);
                questions.add(new QuizQuestion(
                        index,
                        aiEntry.getValue(),       // AI 图完整文件名，如 "1.png"
                        realFiles.get(index),     // 真实图完整文件名，如 "1.png"
                        aiSide,
                        "这两张图片，哪张是 AI 生成的？"
                ));
            }

            if (questions.isEmpty()) {
                Log.w(TAG, "ai_images 和 real_images 中没有同编号的图片对");
                return;
            }

            dao.insertAll(questions);
            Log.d(TAG, "预填充题目完成，共 " + questions.size() + " 道题");
        }

        /**
         * 列出指定 assets 目录中所有图片，返回 {数字编号 -> 完整文件名} 的 Map.
         *
         * <p>支持 .jpg、.jpeg、.png 格式，文件名必须是纯数字，如 1.png、2.jpg。
         * 若同一数字有多个格式，取遍历到的第一个。</p>
         *
         * @param assets    AssetManager
         * @param directory assets 子目录名
         * @return {编号 -> 文件名} Map
         */
        private Map<Integer, String> listImageFiles(AssetManager assets, String directory) {
            Map<Integer, String> result = new HashMap<>();
            try {
                String[] files = assets.list(directory);
                if (files == null) return result;
                for (String file : files) {
                    int dotIndex = file.lastIndexOf('.');
                    if (dotIndex <= 0) continue;
                    String ext = file.substring(dotIndex + 1).toLowerCase();
                    if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png")) {
                        continue;
                    }
                    String name = file.substring(0, dotIndex);
                    try {
                        int index = Integer.parseInt(name);
                        // 同编号有多个格式时取第一个（不重复添加）
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

