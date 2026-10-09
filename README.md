# Calculator Basic + Scientific

源码仓库：[hugestudyZhu/EE317calculator](https://github.com/hugestudyZhu/EE317calculator)。使用 OpenAI Codex，模型为 GPT-6.1 Sol · High。

原生 Android 计算器。Basic 采用浅色界面；Scientific 自动使用深色界面。两者共用字体层级、圆角控件和按键回弹动效，支持石墨橙、海盐蓝、抹茶绿三套配色。

## 操作

- 在输入 / 结果区域左右滑动，切换 Basic 与 Scientific；也可点击顶部 BASIC / SCI 小指示。
- 在同一区域上下滑动，打开共用历史记录。Scientific 的 History 键也可打开历史。
- 点击历史条目，载入科学输入编辑器重新编辑；关闭浮层即可返回原页面。
- 两个模式分别保留输入草稿；运算历史、变量和设置会在关闭应用后保留。

## 功能范围

Basic 保留加减乘除、小数、正负号、百分比、清除和连续等号。按输入顺序计算，例如 `2 + 3 × 4 = 20`。

Scientific 保留参考界面中除 MENU 外的所有入口：

| 区域 | 功能 |
|---|---|
| 顶部工具 | SETUP、History、Undo、Text |
| 输入控制 | SHIFT 第二功能、ALPHA 变量、左右光标、上下参数槽 |
| 科学函数 | sin / cos / tan、反三角、ln / exp、log / 自定义底数、平方 / 立方、根式 / n 次根、幂、分数 / 带分数、π / e、阶乘 / 组合、百分比 / mod、括号、倒数、绝对值 |
| 结果与变量 | Ans、S⇔D 精确 / 小数、ENG、Vars 赋值、M+ / M− |
| Catalog | 双曲函数、取整、整数与随机数、复数、微分 / 积分 / 求和 / 连乘、自定义 f(x) / g(x) |
| SETUP | DEG / RAD / GRAD、Norm / Fix / Sci / ENG、位数、带分数、复数显示、近似分数、清空变量和函数 |

科学模式遵循数学运算优先级。分数、根式和幂采用可编辑参数槽；点击光标位置或使用方向键移动。Text 支持直接输入表达式。

不提供 MENU 及其矩阵、方程、图形等工具页面。复杂的科学表达式在后台计算，保持界面响应。

## 前端结构

- `MainActivity`：单页组成、事件路由和模式过渡。
- `Palette`：浅色 / 深色配色。
- `DisplayArea`：只负责输入区域的滑动手势。
- `CalculatorSheets`：SETUP、历史、Text、变量和 Catalog 共用的浮层。
- `ScienceKeys`：按键标签、输入内容和 SHIFT 对应关系。
- `BasicCalculator` / `ScientificCalculator`：两种计算状态。
- `MathEditor` / `MathDisplayView`：复用并调整的数学输入与显示组件。
- `math/`：复用的表达式计算核心，仅保留当前界面需要的数学支持。

顶部不显示大标题。输入区域高度固定、不会换行；计算完成后算式缩小，输出结果放大，键盘位置保持不变。

右上角调色盘入口提供石墨橙、海盐蓝、抹茶绿三种外观，预览卡展示实际按键配色；选择后立即应用并记住设置。文字操作键使用小字号，数字与数学符号分层显示，科学计数法键使用上标排版。颜色分别用于运算、清除、变量与输入辅助。

设计参考：[Apple 按钮层级](https://developer.apple.com/design/human-interface-guidelines/buttons)、[Material 字体层级](https://m3.material.io/styles/typography/applying-type)、[Material 颜色角色](https://m3.material.io/styles/color/the-color-system)。保留原生 Android 字体和组件。

无第三方 UI 库。保留原生涟漪、按键回弹、数字过渡和模式切换颜色过渡；系统关闭动画时遵循该设置。小屏使用紧凑科学键盘，横屏和大字体支持页面滚动。

## 构建

在 Android Studio 中打开本目录，运行 `app`。最低 Android 7.0（API 24）。终端需完整 JDK，本机可使用 Android Studio 自带的 JBR：

```sh
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug testDebugUnitTest lintDebug
```

安装包：`app/build/outputs/apk/debug/app-debug.apk`。

[功能实现报告](docs/Calculator_Basic_功能实现报告.pdf)包含界面示意、实现路径及核心代码。完整用户指示见 [PROMPTS.md](PROMPTS.md)。本地预览位于 `previews/`，不提交至仓库。

## 上一版本验证

Debug 构建、62 项单元测试和 Android Lint 已通过。模拟器已实际检查基础运算、四向滑动、分数输入、SHIFT 反三角、Text 表达式、变量赋值、角度设置、Catalog、历史回放和重启恢复。计算前后输入区域及等号键位置保持一致。

本次外观调整按要求仅交付源码，未编译、运行或测试；现有 APK 和预览文件属于上一版本。
