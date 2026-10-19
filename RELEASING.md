# Releasing Termux:Float (this fork)

**English** | [简体中文](RELEASING.zh-CN.md)

This documents how this fork cuts a release so the next one only takes a few minutes.
##



### What CI produces

Every push/PR (`github_action_build.yml`) and every published GitHub release
(`github_release_build.yml`) builds the release APK **twice** — same code, two signatures:

| Artifact | Tag pattern | Signed with | Installable next to |
| --- | --- | --- | --- |
| `termux-float-app_<ver>+github.release.apk` | `vX.Y.Z(+sha).github.release` | the bundled `app/testkey_untrusted.jks` | Termux builds signed with the same test key (upstream GitHub builds, local debug builds) |
| `termux-float-app_<ver>+emacs.release.apk` | `vX.Y.Z(+sha).emacs.release` | the GNU Emacs build key | the re-signed Termux app (required by `android:sharedUserId="com.termux"` for tmux session sharing) |

The emacs variant is **skipped with a workflow warning** when no keystore is
configured — set one of these in the repo settings so it is always produced:

- Secret `EMACS_KEYSTORE_BASE64` — the keystore file base64-encoded
  (`base64 -w0 emacs.keystore`).
- Or repository variable `EMACS_KEYSTORE_URL` — a URL the runner can `curl` the
  keystore from.

The alias/password are not needed: `app/build.gradle` already defaults them to the
public values (`emacs keystore` / `emacs1`).

Note on "F-Droid signed": a package installable next to F-Droid's Termux can only be
produced by F-Droid itself — it requires their private signing key, and
`sharedUserId` makes a wrong signature fail install outright. The `+github` artifact
is the distribution build for anything else.
##



### Cutting a release

1. **Bump the version** in `app/build.gradle`:
   - `versionName` — semantic version (`0.21.0`); the CI also validates it.
   - `versionCode` — bump by one (`1005` → `1006`).
   - Update the "Latest version" line in `README.md` and `README.zh-CN.md`.
2. **Commit** with the repo's conventional-commit style
   (`feat(ui):`, `fix(lint):`, `build(signing):`, …) and push to `master`.
   A normal push already builds both variants, so the release build is verified
   before tagging.
3. **Tag and publish**: create the tag `v<versionName>` and a GitHub release.
   `github_release_build.yml` runs on `release: published`, builds both APKs and
   attaches them plus `checksums-sha256.txt` to the release. If the workflow fails
   it deletes the release and the tag again, so the tag is safe to retry.
4. **Verify**: download the release APKs, check `checksums-sha256.txt`, install the
   emacs-signed APK next to the re-signed Termux and confirm session sharing works.
##



### Local builds

The release build signs with `emacs` automatically when `local.properties` points at
the keystore:

```properties
sdk.dir=/path/to/android/sdk
emacs.keystore.path=/path/to/emacs.keystore
```

then `./gradlew assembleRelease` → `app/build/outputs/apk/release/`. Remove the
`emacs.keystore.path` line (or `local.properties` itself) to get the testkey build.

Gotchas seen on this machine, in case they come back:

- The Gradle wrapper (8.9) may not have the AGP cache; the system Gradle under
  `~/.guix-home/profile/bin/gradle` works with
  `GRADLE_USER_HOME=/home/brokenshine/.local/share/gradle`.
- If `dl.google.com` is unreachable, inject a mirror with an init script
  (e.g. `-I /tmp/aliyun-mirror.init.gradle` pointing at `maven.aliyun.com`).
- AGP 8.7 asks for Build Tools 34; with only 35+ installed set
  `android.buildToolsVersion = '35.0.1'` via the same init script.
