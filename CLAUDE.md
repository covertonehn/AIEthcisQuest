# CLAUDE.md — AIEthcisQuest 项目开发约束文档

> 本文件是 AI 编程助手（Agent）在参与本项目开发时**必须首先阅读并严格遵守**的规则文档。
> 每次开始工作前，Agent 应优先阅读本文件，确保所有代码、架构、命名等决策符合以下规范。

---

## 1. 项目概述 (Project Overview)

### 1.1 项目名称
AIEthcisQuest

### 1.2 项目简介
一款以"让用户辨别 AI 生成内容与真实内容"作为核心玩法，渗透工程师职业素养的教育型 Android 应用。

### 1.3 目标平台
- **最低 SDK**: API 24 (Android 7.0)
- **目标 SDK**: API 34 (Android 14)
- **编译 SDK**: API 34
- **语言**: Java（严格使用 Java，不使用 Kotlin）
- **最小 AGP 版本**: 8.2.0
- **Gradle JDK**: 17

### 1.4 核心功能模块
| 模块   | 说明                       |
|------|--------------------------|
| 题目挑战 | 展示 AI 生成 vs 真实内容，让用户判断真伪 |
| 学习中心 | AI 伦理相关知识科普              |
| 成绩统计 | 用户答题记录与能力分析              |
| 个人中心 | 用户个人信息展示                 |

---

## 2. 技术栈约束 (Tech Stack Constraints)

### 2.1 必须使用的技术
- **语言**: Java 17（纯 Java 项目，禁止引入 Kotlin 代码）
- **构建工具**: Gradle (Kotlin DSL)
- **UI 框架**: XML 布局 + View 系统（ViewBinding）
- **架构模式**: MVVM (Model-View-ViewModel)
- **本地存储**: Room（数据库）
- **导航组件**: Navigation Component
- **LiveData + ViewModel**: Jetpack Lifecycle 组件

### 2.2 暂不引入的技术（待后续按需添加）
- 网络请求（Retrofit / OkHttp）
- 依赖注入（Hilt）
- 图片加载（Glide）
- 异步框架（RxJava）
- DataStore

### 2.3 禁止使用的技术
- ❌ Kotlin 代码（本项目为纯 Java 项目）
- ❌ 未在技术栈中列出的第三方库（引入新库需评估）
- ❌ 过时的库（如 HttpClient、AsyncTask、ViewPager 等）

### 2.4 Gradle 依赖管理
- 所有依赖版本统一在 `gradle/libs.versions.toml`（Version Catalog）中管理
- 引入新依赖时必须同步更新版本管理文件

---

## 3. 架构规范 (Architecture Rules)

### 3.1 项目包结构
```
com.aiethicsquest/
├── app/                    # Application 入口类
├── data/
│   ├── model/              # 数据模型（Room Entity）
│   ├── local/              # Room DAO + Database
│   └── repository/         # Repository 实现（供 ViewModel 调用）
├── presentation/
│   ├── ui/                 # Activity / Fragment
│   └── viewmodel/          # ViewModel
└── common/
    └── util/               # 工具类
```

### 3.2 分层原则与数据流
```
View（Activity/Fragment）
  ↕  观察 LiveData / 调用方法
ViewModel
  ↕  调用
Repository
  ↕  操作
Room Database（DAO）
```

**关键规则**:
- View 只持有 ViewModel 引用，不直接操作数据
- ViewModel 不持有 View（Context）引用，通过 LiveData 向上通知
- Repository 封装所有数据访问，ViewModel 不直接调用 DAO
- 禁止跨层直接调用

---

## 4. 编码规范 (Coding Standards)

### 4.1 Java 编码约定
- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 缩进: **4 个空格**，不使用 Tab
- 大括号: K&R 风格（左括号不换行）
- 每行最大长度: **100 字符**
- 编码格式: **UTF-8**

### 4.2 命名规范
| 类型 | 规则 | 示例 |
|------|------|------|
| 类名 | PascalCase | `MainActivity`, `QuizRepository` |
| 接口名 | PascalCase | `QuizRepository` |
| 方法名 | camelCase | `loadQuiz()`, `submitAnswer()` |
| 变量名 | camelCase | `quizList`, `isLoading` |
| 常量名 | UPPER_SNAKE_CASE | `MAX_SCORE`, `DB_NAME` |
| XML 布局 | snake_case | `activity_main.xml`, `item_quiz.xml` |
| Resource ID | snake_case | `btn_submit`, `tv_title` |
| 包名 | 全小写 | `com.aiethicsquest.data.local` |

### 4.3 注释规范
- **公共类和公共方法** 必须写 Javadoc
- **复杂逻辑** 必须行内注释说明意图
- **TODO / FIXME** 格式: `TODO(作者): 说明`

### 4.4 日志规范
- 使用 `android.util.Log`，Tag 统一用类名
- 敏感信息严禁打印到日志

---

## 5. Git 提交规范 (Git Commit Convention)

### 5.1 Commit Message 格式
```
<type>(<scope>): <subject>
```

### 5.2 Type 类型
| Type | 含义 |
|------|------|
| feat | 新功能 |
| fix | Bug 修复 |
| style | 代码格式调整（不影响功能） |
| refactor | 重构 |
| test | 测试相关 |
| chore | 构建/工具链/依赖变更 |

### 5.3 示例
```
feat(challenge): 新增题目挑战答题流程
fix(quiz): 修复选项点击无响应问题
chore(build): 升级 Room 版本至 2.6.1
```

---

## 6. UI/UX 设计规范 (Design Guidelines)

### 6.1 设计风格
- 现代、简洁、教育向
- 主色调: 待定
- 字体: 系统默认字体（Roboto / Noto Sans CJK）

### 6.2 适配要求
- 支持手机竖屏为主
- 适配不同屏幕密度（mdpi ~ xxxhdpi）
- 关键操作按钮最小触控区域 **48dp × 48dp**

### 6.3 资源管理
- 字符串资源必须放在 `strings.xml` 中，**硬编码中文/英文禁止**
- 颜色值统一定义在 `colors.xml`
- 尺寸值统一定义在 `dimens.xml`

---

## 7. 开发流程 (Development Workflow)

- 逐步添加新功能，无特殊要求不改原功能
- 更改代码时如有更好的方案，先询问确认，禁止自行决定
- 若给定的提示词条件不足，需再次确认后再动手
- 改完后自行运行测试，有 bug 及时修复
- 以开发规范的应用为目标，若提示词的要求不严谨或有问题，及时确认更正
- 新加的功能实现要向我解释其作用和原理

---

## 8. 常用命令速查 (Common Commands)

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 清理构建
./gradlew clean

# 运行单元测试
./gradlew test

# Lint 检查
./gradlew lint
```

---

## 附录：文件模板参考

### Activity 模板
```java
package com.aiethicsquest.presentation.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.aiethicsquest.databinding.ActivityExampleBinding;
import com.aiethicsquest.presentation.viewmodel.ExampleViewModel;

public class ExampleActivity extends AppCompatActivity {

    private ActivityExampleBinding binding;
    private ExampleViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExampleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(ExampleViewModel.class);
        initViews();
        observeData();
    }

    private void initViews() {
        // 初始化视图、绑定点击事件
    }

    private void observeData() {
        // 观察 LiveData
        viewModel.getSomeData().observe(this, data -> {
            // 更新 UI
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
```

### ViewModel 模板
```java
package com.aiethicsquest.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ExampleViewModel extends ViewModel {

    private final MutableLiveData<String> someData = new MutableLiveData<>();

    public LiveData<String> getSomeData() {
        return someData;
    }

    public void loadData() {
        // 调用 Repository 获取数据，结果通过 LiveData 推送
        someData.setValue("result");
    }
}
```

### Repository 模板
```java
package com.aiethicsquest.data.repository;

import androidx.lifecycle.LiveData;
import com.aiethicsquest.data.local.ExampleDao;
import com.aiethicsquest.data.model.ExampleEntity;
import java.util.List;

public class ExampleRepository {

    private final ExampleDao dao;

    public ExampleRepository(ExampleDao dao) {
        this.dao = dao;
    }

    public LiveData<List<ExampleEntity>> getAll() {
        return dao.getAll();
    }
}
```

---

> 最后更新: 2026-05-12

