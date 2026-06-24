package com.aiethicsquest.presentation.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aiethicsquest.data.local.AppDatabase;
import com.aiethicsquest.data.model.VideoQuestion;
import com.aiethicsquest.data.model.VideoSessionRecord;
import com.aiethicsquest.data.model.VideoWrongAnswer;
import com.aiethicsquest.data.repository.VideoQuizRepository;
import com.aiethicsquest.data.repository.VideoSessionRecordRepository;
import com.aiethicsquest.data.repository.VideoWrongAnswerRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频挑战玩法 ViewModel.
 *
 * <p>数据流与 ChallengeViewModel 保持一致的模式：</p>
 * <ul>
 *     <li>{@link #currentQuestion} — 当前题目</li>
 *     <li>{@link #answerResult} — 答题结果事件</li>
 *     <li>{@link #quizProgress} — 答题进度</li>
 *     <li>{@link #sessionComplete} — 本轮题目全部答完</li>
 *     <li>{@link #noMoreQuestions} — 题库已全部做完事件</li>
 *     <li>{@link #undoneCount} — 剩余未做题目数（LiveData，供等待页显示）</li>
 * </ul>
 *
 * <p>题库逻辑与识图挑战相同：</p>
 * <ul>
 *     <li>每轮随机取最多 {@value #QUESTIONS_PER_SESSION} 道未做过的题</li>
 *     <li>本轮题目开始前，将它们标记为已做，防止重复出现</li>
 *     <li>若剩余未做题不足 {@value #QUESTIONS_PER_SESSION}，取全部剩余题</li>
 *     <li>若剩余未做题为 0，发出 {@link #noMoreQuestions} 事件</li>
 *     <li>用户点击"重置题库"后，所有题目恢复为未做状态</li>
 * </ul>
 */
public class VideoChallengeViewModel extends AndroidViewModel {

    /** 每轮最大出题数量. */
    public static final int QUESTIONS_PER_SESSION = 10;

    private final VideoQuizRepository videoQuizRepository;
    private final VideoWrongAnswerRepository videoWrongAnswerRepository;
    private final VideoSessionRecordRepository videoSessionRecordRepository;

    private List<VideoQuestion> sessionQuestions = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;

    /** 每题的答题结果：true=答对, false=答错（按题目顺序存放）. */
    private final List<Boolean> questionCorrectResults = new ArrayList<>();

    /** 本轮开始时间（毫秒），用于会话记录. */
    private long sessionStartTime = 0;

    private final MutableLiveData<VideoQuestion> currentQuestion = new MutableLiveData<>();

    /** 答题结果封装类. */
    public static class AnswerResult {
        public final boolean correct;
        /** 正确答案（AI 视频）所在侧（0=上方, 1=下方）. */
        public final int correctSide;

        public AnswerResult(boolean correct, int correctSide) {
            this.correct = correct;
            this.correctSide = correctSide;
        }
    }

    private final MutableLiveData<AnswerResult> answerResult = new MutableLiveData<>();

    /** 答题进度封装类. */
    public static class QuizProgress {
        public final int current;
        public final int total;

        public QuizProgress(int current, int total) {
            this.current = current;
            this.total = total;
        }
    }

    private final MutableLiveData<QuizProgress> quizProgress = new MutableLiveData<>();
    private final MutableLiveData<Integer> sessionComplete = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /**
     * 题库已全部做完事件（值为总题库数量，用于提示）.
     */
    private final MutableLiveData<Integer> noMoreQuestions = new MutableLiveData<>();

    /** 剩余未做题目数（LiveData，等待页实时显示）. */
    private final LiveData<Integer> undoneCount;

    public VideoChallengeViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        videoQuizRepository = new VideoQuizRepository(db);
        videoWrongAnswerRepository = new VideoWrongAnswerRepository(db);
        videoSessionRecordRepository = new VideoSessionRecordRepository(db);
        undoneCount = videoQuizRepository.getUndoneCountLive();
    }

    // ---------- 公开 LiveData Getter ----------

    public LiveData<VideoQuestion> getCurrentQuestion() { return currentQuestion; }
    public LiveData<AnswerResult> getAnswerResult() { return answerResult; }
    public LiveData<QuizProgress> getQuizProgress() { return quizProgress; }
    public LiveData<Integer> getSessionComplete() { return sessionComplete; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Integer> getNoMoreQuestions() { return noMoreQuestions; }
    public LiveData<Integer> getUndoneCount() { return undoneCount; }

    // ---------- 公开业务方法 ----------

    /**
     * 开始新一轮挑战：随机取未做题，从第一题开始.
     *
     * <p>★ 不再提前将题目标记为已做；只有完整答完一轮（{@link #nextQuestion}
     * 推进到最后一题）才会真正标记，确保中途退出不影响题库状态。</p>
     */
    public void startNewSession() {
        isLoading.setValue(true);
        correctCount = 0;
        currentIndex = 0;
        sessionQuestions.clear();
        questionCorrectResults.clear();
        sessionStartTime = System.currentTimeMillis();

        AppDatabase.DB_EXECUTOR.execute(() -> {
            List<VideoQuestion> questions =
                    videoQuizRepository.getRandomUndoneQuestions(QUESTIONS_PER_SESSION);

            if (questions == null || questions.isEmpty()) {
                // 题库已全部做完
                int total = videoQuizRepository.getCount();
                noMoreQuestions.postValue(total);
                isLoading.postValue(false);
                return;
            }

            // ★ 不再提前 markAsDone；等到轮次正常结束时再标记
            sessionQuestions = questions;
            isLoading.postValue(false);
            quizProgress.postValue(new QuizProgress(1, sessionQuestions.size()));
            currentQuestion.postValue(sessionQuestions.get(0));
        });
    }

    /**
     * 用户提交答案.
     *
     * @param userChoice 用户点击的一侧（0=上方, 1=下方）
     */
    public void submitAnswer(int userChoice) {
        VideoQuestion question = currentQuestion.getValue();
        if (question == null) return;

        boolean isCorrect = (userChoice == question.getAiSide());
        questionCorrectResults.add(isCorrect);
        if (isCorrect) {
            correctCount++;
        } else {
            recordWrongAnswer(question, userChoice);
        }
        answerResult.setValue(new AnswerResult(isCorrect, question.getAiSide()));
    }

    /**
     * 前进到下一题；若已是最后一题则标记本轮题目为已做、保存轮次记录并发出完成事件.
     *
     * <p>只有在此方法触发"轮次完成"分支时，才把本轮题目标记为已做（从题库中去除）。
     * 若用户中途退出，题目不会被标记，下轮可以再次抽到。</p>
     */
    public void nextQuestion() {
        currentIndex++;
        if (currentIndex >= sessionQuestions.size()) {
            // ★ 轮次正常完成：将本轮所有题目标记为已做
            markSessionQuestionsAsDone();
            saveSessionRecord();
            sessionComplete.setValue(correctCount);
        } else {
            quizProgress.setValue(new QuizProgress(currentIndex + 1, sessionQuestions.size()));
            currentQuestion.setValue(sessionQuestions.get(currentIndex));
        }
    }

    /**
     * 将本轮的所有题目标记为已做（在后台线程执行）.
     */
    private void markSessionQuestionsAsDone() {
        List<Integer> ids = new ArrayList<>();
        for (VideoQuestion q : sessionQuestions) {
            ids.add(q.getId());
        }
        if (!ids.isEmpty()) {
            final List<Integer> idsCopy = ids;
            AppDatabase.DB_EXECUTOR.execute(() ->
                    videoQuizRepository.markAsDone(idsCopy));
        }
    }

    /**
     * 判断当前是否正在进行一轮挑战（已有题目在进行中，尚未完成）.
     *
     * @return true=正在进行中
     */
    public boolean isSessionInProgress() {
        return !sessionQuestions.isEmpty();
    }

    /**
     * 放弃当前轮次（中途退出），不保存记录，不标记题目为已做.
     */
    public void abandonSession() {
        resetSession();
    }

    /**
     * 重置题库：所有题目恢复为未做状态.
     */
    public void resetQuestionBank() {
        videoQuizRepository.resetAllDone();
    }

    /**
     * 消费答题结果事件.
     */
    public void consumeAnswerResult() {
        answerResult.setValue(null);
    }

    /**
     * 消费会话完成事件.
     */
    public void consumeSessionComplete() {
        sessionComplete.setValue(null);
    }

    /**
     * 消费"题库已用尽"事件.
     */
    public void consumeNoMoreQuestions() {
        noMoreQuestions.setValue(null);
    }

    /**
     * 重置为"等待开始"状态，清空所有 LiveData（不重置题库）.
     */
    public void resetSession() {
        currentQuestion.setValue(null);
        quizProgress.setValue(null);
        answerResult.setValue(null);
        sessionQuestions.clear();
        questionCorrectResults.clear();
        currentIndex = 0;
        correctCount = 0;
    }

    // ---------- 私有方法 ----------

    /**
     * 保存本轮挑战的轮次记录.
     */
    private void saveSessionRecord() {
        // 构建视频路径字符串（每题用"|"分隔，上下视频用","分隔）
        StringBuilder pathsSb = new StringBuilder();
        // 构建答题结果字符串（每题格式：aiSide:isCorrect）
        StringBuilder resultsSb = new StringBuilder();

        for (int i = 0; i < sessionQuestions.size(); i++) {
            if (i > 0) {
                pathsSb.append("|");
                resultsSb.append("|");
            }
            VideoQuestion q = sessionQuestions.get(i);
            pathsSb.append(q.getTopVideoPath()).append(",").append(q.getBottomVideoPath());

            int aiSide = q.getAiSide();
            int isCorrect = (i < questionCorrectResults.size() && questionCorrectResults.get(i))
                    ? 1 : 0;
            resultsSb.append(aiSide).append(":").append(isCorrect);
        }

        VideoSessionRecord record = new VideoSessionRecord(
                System.currentTimeMillis(),
                sessionQuestions.size(),
                correctCount,
                pathsSb.toString(),
                resultsSb.toString()
        );
        videoSessionRecordRepository.insert(record);
    }

    private void recordWrongAnswer(VideoQuestion question, int userChoice) {
        VideoWrongAnswer wrong = new VideoWrongAnswer(
                question.getId(),
                userChoice,
                question.getAiSide(),
                System.currentTimeMillis(),
                question.getVideoIndex(),
                question.getAiFileName(),
                question.getRealFileName(),
                question.getDescription()
        );
        videoWrongAnswerRepository.insert(wrong);
    }
}

