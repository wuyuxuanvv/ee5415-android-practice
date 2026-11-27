# 把 BMI 或计算器复制到空项目

以下步骤用于在允许完整复制工程的场景下练习或迁移。两个项目分别打开，各自构建。

## 1. 准备原项目和空项目

1. 在仓库首页点 **Code → Download ZIP**，用 Windows 解压。在解压出来的目录里找到 [BmiPractice](BmiPractice/) 和 [CalculatorPractice](CalculatorPractice/)。
2. 在 Android Studio 点 **New Project → Phone and Tablet → No Activity**。做 BMI 就把新工程命名为 `BmiCopy`，做计算器就命名为 `CalculatorCopy`。如果向导要求选择最低 SDK，选 **API 26**。记住 **Save location**，然后点 **Finish**。
3. 工程创建后点 **File → Close Project**。用资源管理器打开刚才的保存位置，找到包含 `app` 文件夹和 `settings.gradle.kts`（或 `settings.gradle`）的那一层。下面称它为「新工程根目录」。
4. 只在**刚创建的空项目**里移走以下模板内容，留出位置给原工程的文件。
   - 新工程根目录的 `build.gradle.kts`、`settings.gradle.kts`，如果存在。
   - 新工程 `app` 里的 `build.gradle.kts`，如果存在。
   - 新工程的 `app/src` 整个目录。
   - 如果下面复制时出现同名文件，选择 **替换目标中的文件**。

## 2. 照搬 BMI 时，复制这些

1. 打开原来的 **`BmiPractice`** 文件夹。选中 `build.gradle`、`settings.gradle`、`gradle.properties`、`gradlew`、`gradlew.bat`，复制到 **`BmiCopy` 根目录**。
2. 把原项目的整个 `gradle` 文件夹复制到 **`BmiCopy` 根目录**。确认能找到 `BmiCopy/gradle/wrapper/gradle-wrapper.jar` 和 `gradle-wrapper.properties`。
3. 打开 **`BmiPractice/app`**，把里面的 `build.gradle` 文件和整个 `src` 文件夹复制到 **`BmiCopy/app`**。
4. 打开 **`BmiCopy/app/src`** 检查。应该直接看到 `main`、`mvp`、`mvvm`、`mvi` 四个文件夹。继续打开 `main`，里面应有 `AndroidManifest.xml`、`java` 和 `res`。文件夹层级应为 `BmiCopy/app/src/main`。

## 3. 照搬计算器时，复制这些

1. 打开原来的 **`CalculatorPractice`** 文件夹。选中 `build.gradle`、`settings.gradle`、`gradle.properties`、`gradlew`、`gradlew.bat`，复制到 **`CalculatorCopy` 根目录**。
2. 把原项目的整个 `gradle` 文件夹复制到 **`CalculatorCopy` 根目录**。确认能找到 `CalculatorCopy/gradle/wrapper/gradle-wrapper.jar` 和 `gradle-wrapper.properties`。
3. 打开 **`CalculatorPractice/app`**，把里面的 `build.gradle` 文件和整个 `src` 文件夹复制到 **`CalculatorCopy/app`**。
4. 打开 **`CalculatorCopy/app/src/main`** 检查。这里应有 `AndroidManifest.xml`、`java` 和 `res`。两个 Java 文件 `MainActivity.java`、`Expression.java` 都在 `java/com/practice/calculator/` 下。

## 4. 同步并运行

1. 回到 Android Studio，点 **Open**，选你刚复制好的 **`BmiCopy`** 或 **`CalculatorCopy`** 根目录，再点 **OK**。
2. 等 Gradle 同步完成。如果 IDE 没有自动同步，点 **File → Sync Project with Gradle Files**。Android Studio 会为当前电脑生成或更新 `local.properties`。
3. 如果提示缺少 SDK，在 **Tools → SDK Manager** 安装 **Android SDK Platform 36** 和 **Android SDK Build-Tools 36.1.0**。安装后再同步。[实验室初始化 PDF](docs/EE5415-Lab-Setup.pdf)
4. **BMI**：打开 **View → Tool Windows → Build Variants**，把 `app` 的 **Active Build Variant** 选成 `mvpDebug`。如果题目指定 MVVM 或 MVI，分别选 `mvvmDebug` 或 `mviDebug`。**计算器**：使用默认的 `debug`。
5. 在顶部运行配置选 `app`，设备选 **API 33 模拟器**，再点绿色的 **Run ▶**。BMI 可以输入身高 170、体重 65、年龄 25，选一个性别，结果应为 **22.5**。计算器可以测试 **2 + 3 = 5**。

## 5. 手动生成 APK 并找到文件

1. 在 Android Studio 底部打开 **Terminal**，用 PowerShell 执行 `Get-Location`。确认路径是当前工程根目录，并且这里能找到 `gradlew.bat`。
2. 按你的工程和版本运行表里的命令。看到 **BUILD SUCCESSFUL** 后，在资源管理器打开同一工程目录，按表里的相对路径找到 APK。

| 要生成的 APK | PowerShell 命令 | 从工程根目录查找 APK |
| --- | --- | --- |
| BMI MVP | `.\gradlew.bat :app:assembleMvpDebug` | `app/build/outputs/apk/mvp/debug/app-mvp-debug.apk` |
| BMI MVVM | `.\gradlew.bat :app:assembleMvvmDebug` | `app/build/outputs/apk/mvvm/debug/app-mvvm-debug.apk` |
| BMI MVI | `.\gradlew.bat :app:assembleMviDebug` | `app/build/outputs/apk/mvi/debug/app-mvi-debug.apk` |
| 计算器 | `.\gradlew.bat :app:assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |

## 6. 打包提交

1. 在工程外新建一个文件夹，比如 `BMI_Submission`。在里面再新建一个 `BmiCopy` 文件夹，用来放**完整的可构建源代码工程**。
2. 按第 2 节的清单，把你的最终 `BmiCopy` 里的根目录文件、`gradle`、`app/build.gradle` 和 `app/src` 复制到提交用的 `BmiCopy`，保留原来的目录层级。若提交计算器，用第 3 节的清单建立 `CalculatorCopy` 副本。
3. 把第 5 节找到的最终 APK 复制到 `BMI_Submission`，和源代码工程文件夹并排放。题目规定提交多个版本时，放入对应的 APK。
4. 如果按原 BMI 作业要求提交，再放入两张截图。一张截图显示输入页，另一张显示 BMI 结果页。最终目录示例如下。

```text
BMI_Submission/
├── BmiCopy/                 完整的可构建源代码工程
├── app-mvp-debug.apk        可安装的 APK
├── bmi-input.png            输入页截图
└── bmi-report.png           结果页截图
```

5. 在资源管理器里右键 **`BMI_Submission` → 压缩为 ZIP 文件**（或 **发送到 → 压缩文件夹**）。打开生成的 ZIP 检查能看到 APK、源代码工程和题目要求的截图，再按提交页面要求上传。
