# macOS Native 链接缺 objc_msgSend$* 导致失败

GraalVM 21 在本机编 macOS Native 时，链接阶段找不到 `objc_msgSend$…` 符号的原因与处理。

- 日期：2026-10-16
- 涉及：后台 / 部署

## 现象

`native-image` 分析/编译已通过，最后 Linking 失败：

```
Undefined symbols for architecture arm64:
  "_objc_msgSend$UTF8String", referenced from:
      _systemVersionPlatform in libdarwin.a(foundation.o)
ld: symbol(s) not found for architecture arm64
```

同时可能看到 `object file … was built for newer macOS version (14.0) than being linked (12.1)`。

## 根因

1. GraalVM 21 自带的 `libdarwin.a` 按较新的 macOS 构建，依赖 Objective-C 消息发送桩。
2. 本机 `xcode-select` 仍指向很旧的 Xcode 13 / SDK 12.1，链接部署目标被压成 12.1，且未链入 `-lobjc`。

## 解决办法

仅在本机 macOS 构建注入（`cat2bug-build` → `_compile_macos_native`，勿写进共用 `native` profile，以免 Linux Mandrel 链接失败）：

1. `scripts/platform/macos-native-cc.sh` 包装 `cc`/`clang`，强制 `-isysroot` 指向 Command Line Tools 的较新 SDK；经 `.work/macos-cc-bin` 置入 `PATH` 最前，供 GraalVM 自动探测。
2. Maven `-Dquarkus.native.additional-build-args=…` 在原有参数后追加 `-H:NativeLinkerOption=-lobjc` 与对应 `-isysroot`。
3. 环境：`DEVELOPER_DIR` / `SDKROOT` / `MACOSX_DEPLOYMENT_TARGET=13.0`。

长期建议：升级 Xcode，或 `sudo xcode-select -s /Library/Developer/CommandLineTools`。

## 以后怎么避免

- 本机编 macOS Native 前确认 `xcrun --show-sdk-version` 不低于 13。
- 升级 GraalVM 大版本后若再出现链接缺符，先查是否又回到了旧 Xcode SDK。
