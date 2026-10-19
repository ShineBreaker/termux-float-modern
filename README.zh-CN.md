# Termux:Float

[English](README.md) | **简体中文**

[![构建状态](https://github.com/termux/termux-float/workflows/Build/badge.svg)](https://github.com/termux/termux-float/actions)
[![加入 https://gitter.im/termux/termux 聊天室](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)

一个 [Termux] 插件应用，在悬浮终端窗口中显示终端。
##



### 目录
- [安装](#安装)
- [本 Fork 的特性](#本-fork-的特性)
- [终端与应用设置](#终端与应用设置)
- [调试](#调试)
- [值得注意](#值得注意)
- [维护者与贡献者](#维护者与贡献者)
- [Fork](#fork)
##



### 安装

最新版本为 `v0.21.0`。

**继续阅读前请先查看 [`termux-app` 安装说明](https://github.com/termux/termux-app#Installation)。**

### F-Droid

`Termux:Float` 可以从 `F-Droid` 获取：[软件包页面](https://f-droid.org/en/packages/com.termux.window)。

安装 `Termux:Float` **不需要**先下载 `F-Droid` 应用本身（页面上那个 `Download F-Droid` 链接是 F-Droid 客户端）。直接点击每个版本区块底部的 `Download APK` 即可下载 APK。

`Github` 上发布新版本后，`F-Droid` 通常需要几天（甚至一周以上）才会更新——F-Droid 在[检测](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/com.termux.window.yml)到新 `Github` release 后自行构建发布，Termux 维护者对 F-Droid 的构建发布没有任何控制权，也没有 F-Droid 版的签名密钥，因此无法在 `Github` 上自行发布与 `F-Droid` 版兼容的 APK。

`F-Droid` 经常不会自动提醒更新，需要在 `Updates` 页手动下拉刷新。请确保对该应用关闭电池优化，详见 https://dontkillmyapp.com/ 。

### Github

`Termux:Float` 可以在 `Github` 获取：[`Github Releases`](https://github.com/termux/termux-float/releases)（`>= 0.15.0`）或 [`Github Actions`](https://github.com/termux/termux-float/actions/workflows/github_action_build.yml?query=branch%3Amaster+event%3Apush) 构建产物。

`Github Releases` 的 APK 在 release 的 `Assets` 折叠区中，随新版本自动上传。

`Github Actions` 的 APK 在工作流运行页面的 `Artifacts` 区，每次提交/推送都会生成，适合不想等 release、想立即体验新功能或测试 PR 的用户。注意必须[登录 `Github` 账号](https://github.com/login)才能下载 `Artifacts`；如果使用 `Github` 手机应用，请改用 Chrome、Firefox 等已登录账号的浏览器打开链接，应用内浏览器可能没有登录。

每个 release 和 action 构建都提供两个签名变体：`+github.release`（仓库内置 testkey，与上游 GitHub 版 Termux 兼容）和 `+emacs.release`（GNU Emacs 构建密钥，用于重签名版 Termux）。安装与你 Termux 签名一致的那个——两个变体之间、以及与其他来源之间都不能互换。

### Google Play Store **（已弃用）**

**由于 [Android 10 的问题](https://github.com/termux/termux-packages/wiki/Termux-and-Android-10)，Termux 及其插件已不再在 [Google Play Store](https://play.google.com/store/apps/details?id=com.termux.window) 更新并已弃用，强烈建议不要再从 Play Store 安装 Termux 系列应用。** 详见 https://github.com/termux/termux-app#google-play-store-deprecated 。
##



### 终端与应用设置

`Termux:Float` 支持在 `~/.termux/termux.float.properties` 文件中定义设置（`>= 0.15.0`），作用类似 `Termux` 的 `~/.termux/termux.properties`。目前支持的属性：`enforce-char-based-input`、`ctrl-space-workaround`、`bell-character`、`terminal-cursor-style`、`terminal-transcript-rows`、`back-key`、`default-working-directory`、`volume-keys`。详见 [Terminal Settings](https://wiki.termux.com/wiki/Terminal_Settings)。`~/` 是 Termux home 目录 `/data/data/com.termux/files/home/` 的缩写，也可写作 `$HOME`。

可以运行下面的命令用 `nano` 编辑器创建/编辑该文件，按 `Ctrl+o` 然后 `Enter` 保存，`Ctrl+x` 退出。也可以在创建后用 [SAF 文件浏览器](https://github.com/termux/termux-tasker#Creating-And-Modifying-Scripts)编辑。

```
mkdir -p ~/.termux
nano ~/.termux/termux.float.properties
```

##



### 本 Fork 的特性

本 fork（`termux-float-modern`）基于上游 `Termux:Float`，除下述上游功能外还提供：

- **Material Design 3 界面** — 悬浮窗边框、悬浮权限页和窗口背景采用 Material Design 3 深色基线方案：16dp 圆角窗口、surface container 标题栏、带圆形状态层的 40dp 图标按钮、MD3 字体层级与 MD3 配色的气泡描边。在 Android 12+ 上窗口边框、文字和强调色还会跟随系统壁纸调色板（动态取色）；更早版本保持基线深色方案。
- **界面本地化** — 所有用户可见字符串都是可翻译资源，已内置简体中文（`zh-rCN`）。新增字符串后请运行 `scripts/check-translations.py`，防止翻译失同步。
- **会话共享** — 窗口标题栏的会话选择器列出 Termux 内运行的 tmux 会话。悬浮窗中 attach 的会话可以在 Termux 应用中打开（反之亦然），两端画面保持同步，因为双方都是同一 tmux 会话的客户端。当已安装 tmux 时，悬浮窗的初始会话也以 `termux-float` 为名跑在 tmux 里，Termux 端可以直接 attach，不再是无法共享的裸 shell。需要 `pkg install tmux`；把会话移交到 Termux 还需在 `~/.termux/termux.properties` 中设置 `allow-external-apps = true`。普通 shell 会话无法共享——pty 属于创建它的进程。

  Termux 的标签页默认是裸 shell，因此永远不会出现在悬浮窗的会话列表里。想让每个新 Termux 标签页都落在 tmux 里，把下面任一行追加到 shell 的 rc 文件（`~/.bashrc`、`~/.zshrc` 等）：

  ```sh
  # 每个新标签页都 attach 到名为 "main" 的共享会话
  [ -z "$TMUX" ] && exec tmux new-session -A -s main

  # 或者每个标签页各开一个 tmux 会话，在悬浮窗列表里挑
  [ -z "$TMUX" ] && exec tmux
  ```

  对于已经打开的标签页，在里面直接执行 `tmux` 即可把它变成 tmux client。
- **同签名构建** — 应用声明了 `android:sharedUserId="com.termux"`，必须使用与已安装 Termux 相同的证书签名；签名不匹配会安装失败（`INSTALL_FAILED_SHARED_USER_INCOMPATIBLE`），即使装上也会拿到不同的 UID。在 `local.properties` 中指向构建 Termux 用的 keystore（`emacs.keystore.path=/path/to/emacs.keystore`；alias 与密码默认取 GNU Emacs 公开值 `emacs keystore` / `emacs1`），debug 与 release 构建都会用它签名。没有该配置时使用上游 debug 测试密钥。
##



### 调试

可以在 `Termux` 应用设置 -> `Termux:Float` -> `Debugging` -> `Log Level` 中调整 `logcat` 日志级别来帮助排查问题（需要 `Termux` 应用版本 `>= 0.118.0`）。默认 `Normal`，`Verbose` 会记录更多信息。排查完建议改回 `Normal`，否则正常使用时可能有隐私数据进入 `logcat`，且额外日志会增加耗时。

设置好日志级别后，可以在 `Termux` 或 `Termux:Float` 终端里运行 `logcat` 实时查看日志（`Ctrl+c` 停止），或用 `logcat -d > logcat.txt` 导出，也可以通过 `ADB` 在电脑上查看。更多信息见官方 [logcat 文档](https://developer.android.com/studio/command-line/logcat)。

##### 日志级别
- `Off` - 不记录任何日志
- `Normal` - 记录 error、warn、info 级别日志及堆栈
- `Debug` - 记录 debug 级别日志
- `Verbose` - 记录 verbose 级别日志
##



## 维护者与贡献者

详见 `termux/termux-app` `README` 的 [For Maintainers and Contributors](https://github.com/termux/termux-app#For-Maintainers-and-Contributors) 一节。

本 fork 的发版流程——版本号、打 tag、CI 双签名（testkey `+github` 与 emacs 密钥 `+emacs` 两个 APK）：[`RELEASING.zh-CN.md`](RELEASING.zh-CN.md)（[English](RELEASING.md)）。
##



## Fork

详见 `termux/termux-app` `README` 的 [Forking](https://github.com/termux/termux-app#Forking) 一节。
##



[Termux]: https://termux.dev
