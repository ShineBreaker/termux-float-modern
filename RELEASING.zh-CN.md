# Termux:Float 发版流程（本 fork）

[English](RELEASING.md) | **简体中文**

记录本 fork 的发版流程，方便下次直接复用。
##



### CI 产物

每次 push/PR（`github_action_build.yml`）和每次发布 GitHub release
（`github_release_build.yml`）都会把同一个 release 版本**构建两遍**——代码相同、签名不同：

| 产物 | tag 命名 | 签名 | 可装环境 |
| --- | --- | --- | --- |
| `termux-float-app_<ver>+github.release.apk` | `vX.Y.Z(+sha).github.release` | 仓库内置 `app/testkey_untrusted.jks` | 同样 testkey 签名的 Termux（上游 GitHub 构建、本地 debug 构建） |
| `termux-float-app_<ver>+emacs.release.apk` | `vX.Y.Z(+sha).emacs.release` | GNU Emacs 构建密钥 | 重签名版 Termux（`android:sharedUserId="com.termux"` 共享 tmux 会话的前提） |

未配置 keystore 时 emacs 变体**会跳过并在 workflow 里打 warning**——在仓库设置里
配置下面任一项即可让它始终产出：

- Secret `EMACS_KEYSTORE_BASE64` —— keystore 文件的 base64
  （`base64 -w0 emacs.keystore`）。
- 或仓库变量 `EMACS_KEYSTORE_URL` —— runner 能用 `curl` 下载 keystore 的地址。

alias/密码不用配：`app/build.gradle` 已默认取公开值（`emacs keystore` / `emacs1`）。

关于"F-Droid 签名"：能装在 F-Droid 版 Termux 旁边的包只能由 F-Droid 自己产出——
需要他们的私有签名密钥，而且 `sharedUserId` 下签名不匹配会直接安装失败。
`+github` 产物就是给其他环境用的分发包。
##



### 发版步骤

1. **升版本号**（`app/build.gradle`）：
   - `versionName` —— 语义化版本（`0.21.0`），CI 会校验格式。
   - `versionCode` —— 加一（`1005` → `1006`）。
   - 同步 `README.md` 与 `README.zh-CN.md` 里的"最新版本"行。
2. **提交**：沿用仓库的 conventional commit 风格
   （`feat(ui):`、`fix(lint):`、`build(signing):`……）推到 `master`。
   普通 push 本身就会构建两个变体，等于发版前先验证一遍。
3. **打 tag 发 release**：建 `v<versionName>` tag 并发布 GitHub release。
   `github_release_build.yml` 监听 `release: published`，构建两个 APK 并连同
   `checksums-sha256.txt` 上传到 release。workflow 失败时会自动删掉该 release
   和 tag，可以放心重试。
4. **验收**：下载 release 里的 APK，核对 `checksums-sha256.txt`，把 emacs 签名
   包装到重签名 Termux 旁边，确认会话共享正常。
##



### 本地构建

`local.properties` 指向 keystore 时 release 构建自动用 `emacs` 签名：

```properties
sdk.dir=/path/to/android/sdk
emacs.keystore.path=/path/to/emacs.keystore
```

然后 `./gradlew assembleRelease`，产物在 `app/build/outputs/apk/release/`。
删掉 `emacs.keystore.path`（或整个 `local.properties`）得到 testkey 包。

本机踩过的坑，备查：

- wrapper 指定的 Gradle 8.9 可能缺 AGP 缓存；用
  `~/.guix-home/profile/bin/gradle` 加
  `GRADLE_USER_HOME=/home/brokenshine/.local/share/gradle`。
- `dl.google.com` 不通时用 init 脚本注入镜像
  （`-I /tmp/aliyun-mirror.init.gradle`，指 `maven.aliyun.com`）。
- AGP 8.7 要 Build Tools 34；只装了 35+ 就在同一 init 脚本里设
  `android.buildToolsVersion = '35.0.1'`。
