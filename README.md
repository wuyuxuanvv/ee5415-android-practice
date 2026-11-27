# EE5415 Android 练习

一个仓库包含两个独立的 Gradle 工程。解压 GitHub 的 ZIP 或克隆仓库后，用 Android Studio 的 **File → Open** 分别打开 [BmiPractice](BmiPractice/) 或 [CalculatorPractice](CalculatorPractice/)。选择包含 `settings.gradle` 的那一层目录。

## 实验室配置

| 项目 | 配置 |
| --- | --- |
| Android Studio | Otter 2025.2.1；实验室截图为 2025.2.1 Patch 1 |
| 构建 | AGP 8.13.0、Gradle Wrapper 8.13、Java 17 源码级别 |
| 本地 Gradle JDK | 可使用 Otter 自带的 JBR |
| SDK Manager | Android SDK Platform 36、Android SDK Build-Tools 36.1.0 |
| Device Manager | 实验室的 Android 13 / API 33 模拟器 |
| 工程的 minSdk / targetSdk | 26 / 34 |

首次打开时等 Gradle Sync 完成，再选 API 33 模拟器并点 **Run**。需要联网下载 Gradle 和未缓存的依赖时，预留同步时间。API 36 是编译使用的 SDK，API 33 是安装应用的 Android 系统版本。实验室新建项目示例使用 API 24 作为最低版本。练习工程最低版本是 API 26，符合 API 33 模拟器的安装条件。

BMI 默认可以先选 `mvpDebug`。在 **View → Tool Windows → Build Variants** 选 `mvvmDebug` 或 `mviDebug` 可切换架构。三种版本的应用 ID 不同，各自有独立的本地记录。

也可以在任一工程目录的 Windows PowerShell 里构建。

```powershell
.\gradlew.bat :app:assembleDebug
```

APK 位于该工程的 `app/build/outputs/apk/`，BMI 会按架构分目录。

## BMI 代码怎么改

运行顺序是 **输入页 → `submit()` → 架构控制器 → `BmiModel.calculate()` → SQLite → 报告页**。

| 要改的功能 | 入口 | 需要一起检查的地方 |
| --- | --- | --- |
| 身高、体重、年龄、性别等输入 | [MainActivity.java](BmiPractice/app/src/main/java/com/practice/bmi/MainActivity.java) 的 `showEntry()`、`readForm()`、`submit()` | 增删输入项时，同步调整 `BmiModel.Input`、输入校验和 Bundle 恢复。保留儿童分级时仍需提供年龄及性别。 |
| BMI 公式、分级、严重提示 | [BmiModel.java](BmiPractice/app/src/main/java/com/practice/bmi/BmiModel.java) 的 `assess()` 与 `BOYS`／`GIRLS` | `showReport()` 控制颜色和标签。儿童表按 6 至 18 岁逐行存放，每行是前四个分类的上限。 |
| 文案和中英文 | [英文 strings.xml](BmiPractice/app/src/main/res/values/strings.xml) 和 [中文 strings.xml](BmiPractice/app/src/main/res/values-zh/strings.xml) | 两份文件使用相同资源键。语言菜单及持久化位于 `onCreateOptionsMenu()`、`onOptionsItemSelected()`、`setLanguage()`。 |
| 右上角菜单、报告长按菜单 | `onCreateOptionsMenu()` 与 `onCreateContextMenu()` | 删除某一项时，也删除对应的回调分支和未使用的菜单 ID。完整移除长按菜单时，还要删掉 `showReport()` 的 `registerForContextMenu()` 调用。 |
| 横竖屏布局 | `place()` | 竖屏把卡片上下叠放，横屏左右并排。保留 `onSaveInstanceState()` 和 `restore()` 可继续保存旋转前的状态。 |
| 自动回填 | [BmiStorage.java](BmiPractice/app/src/main/java/com/practice/bmi/BmiStorage.java) 的 `draft()`、`saveDraft()` | 如需去掉回填，移除 `onCreate()` 中读取 draft 的代码，以及 `onPause()`、`submit()` 的保存调用。Bundle 保存可以独立保留。 |
| 历史和趋势图 | `showHistory()`、`BmiStorage.saveResult()`、[TrendChart.java](BmiPractice/app/src/main/java/com/practice/bmi/TrendChart.java) | 只隐藏折线图时移除 `showHistory()` 中创建和添加 `TrendChart` 的代码，日期列表仍可用。完整移除历史时还需去掉所有趋势入口、`HISTORY` 页面及导航，以及 `BmiModel.calculate()` 的保存调用。 |

架构入口位于各自源码目录里的 `ControllerFactory.java`。 [MVP](BmiPractice/app/src/mvp/java/com/practice/bmi/ControllerFactory.java) 通过 Presenter 回调界面，[MVVM](BmiPractice/app/src/mvvm/java/com/practice/bmi/ControllerFactory.java) 通过 ViewModel 和 LiveData 传给界面，[MVI](BmiPractice/app/src/mvi/java/com/practice/bmi/ControllerFactory.java) 把提交转成 Intent，再由 Store 生成报告 State。三者都共用 `src/main` 的界面和 BMI 规则。要只保留一个构建变体时，可在 [app/build.gradle](BmiPractice/app/build.gradle) 删除其他架构的 flavor 条目。若移除 MVVM，也删除 `mvvmImplementation` 依赖声明。

## 计算器代码怎么改

运行顺序是 **点按键 → `press()` → 编辑算式／运行计算 → `render()`**。

| 要改的功能 | 入口 | 需要一起检查的地方 |
| --- | --- | --- |
| 按键顺序、每行布局 | [MainActivity.java](CalculatorPractice/app/src/main/java/com/practice/calculator/MainActivity.java) 的 `createLayout()`、`addRow()` | 按钮文字同时传给 `press()`。改运算符显示时，检查 `appendOperator()` 的符号映射和 `readable()`。 |
| 去掉退格或正负号 | `createLayout()` 的 `addRow()` | 从键盘移除对应文字，再删除 `press()` 的 `DEL` 或 `±` 分支及 `addRow()` 中相应的无障碍描述。 |
| 去掉小数 | `createLayout()` 中的 `.` 键，以及 `appendNumber()` | 去掉输入入口和重复小数点检查即可限制输入。除法仍可能产生小数结果。 |
| 运算优先级、除法精度 | [Expression.java](CalculatorPractice/app/src/main/java/com/practice/calculator/Expression.java) 的 `sum()`、`product()`、`number()` | `sum()` 处理加减，`product()` 处理乘除。精度由 `MathContext.DECIMAL64` 控制。除零提示在 `MainActivity.calculate()`。 |
| 横竖屏显示和结果样式 | `createLayout()` 与 `render()` | `createLayout()` 控制布局，`render()` 选择当前算式、结果或错误。 `onSaveInstanceState()` 保存旋转前的显示状态。 |

每次改完，跑一次原来的流程和改过的流程。比如计算器可用 `2 + 3 × 4 = 14`、`7 ÷ 0` 和旋转后的结果做检查。BMI 可用身高 170 cm、体重 65 kg、年龄 25 岁得到 BMI 22.5，再重新计算一次验证当天记录覆盖。
