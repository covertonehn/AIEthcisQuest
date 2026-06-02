package com.aiethicsquest.app;

import android.app.Application;
import android.util.Log;

import com.aiethicsquest.data.local.AppDatabase;

/**
 * 应用程序入口类.
 *
 * <p>在 onCreate 中提前初始化 Room 数据库实例，触发首次创建时的预填充回调。</p>
 */
public class AIEthicsQuestApplication extends Application {

    private static final String TAG = "AIEthicsQuestApp";

    @Override
    public void onCreate() {
        super.onCreate();
        initDatabase();
    }

    /**
     * 初始化 Room 数据库.
     *
     * <p>在后台线程中触发数据库创建，Room 会自动执行预填充回调。
     * 使用 DB_EXECUTOR 确保在后台线程中执行，不阻塞主线程。</p>
     */
    private void initDatabase() {
        AppDatabase.DB_EXECUTOR.execute(() -> {
            try {
                // 获取数据库实例（首次调用时创建并触发预填充）
                AppDatabase db = AppDatabase.getInstance(this);
                Log.d(TAG, "数据库初始化完成，题目数量：" + db.quizQuestionDao().getCount());
            } catch (Exception e) {
                Log.e(TAG, "数据库初始化失败", e);
            }
        });
    }
}

