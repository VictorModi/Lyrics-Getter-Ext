# Lyrics Getter 扩展

<div align="center">
    <img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/icon.svg" alt="icon" width="150">
    <p>LyricsGetter · 扩展</p>
</div>

( [English](https://github.com/VictorModi/LyricsGetterExt/blob/main/README.md) / 简体中文 / [繁體中文](https://github.com/VictorModi/LyricsGetterExt/blob/main/docs/README_zh-TW.md) )

# 这是什么 ?
这是一个用于获取网络歌词的软件。

它通过 [MediaController](https://developer.android.google.cn/reference/android/media/session/MediaController) 获取当前正在播放的媒体信息然后自动搜索歌词并推送给 [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter)。

基于 [KaguraRinko/StatusBarLyricExt](https://github.com/KaguraRinko/StatusBarLyricExt)。 但是我们移除了系统检测加入了 MusixMatch 歌词源且通过API适配了 [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter) ! 
(...以及...是的...我们还为它添加了一个图标...)

# 注意
如果你使用 Lyrics Getter 扩展 是为了使用状态栏歌词，请确保在你的设备上正确配置 StatusBarLyric。请在测试模式下成功 Hook 上状态栏，确保能够正常显示内容。

[相关教程](https://blog.xiaowine.cc/posts/8e64/)

# 如何使用 ?
1. 确保 [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter) 已经在正常工作以后前往你的 Xposed 管理器内对 [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter) 勾选 [LyricsGetter Ext](https://github.com/VictorModi/LyricsGetterExt)。

<div style="display: flex; justify-content: center;">
    <img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/img/how2use.jpg" alt="h2u">
</div>

2. 打开 [LyricsGetter Ext](https://github.com/VictorModi/LyricsGetterExt) ，授予 [LyricsGetter Ext](https://github.com/VictorModi/LyricsGetterExt) 通知权限，确保 `Lyrics Getter 连接状态` 显示为 `true` ， 然后将开关 `开启` 打开。

<div style="display: flex; justify-content: center;">
    <img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/img/statusTrue.jpg" alt="st">
</div>

3. 当你点击 `开启` 后，将会进入 `通知使用权` 配置界面，请在该页面授予 [LyricsGetter Ext](https://github.com/VictorModi/LyricsGetterExt) 通知使用权，授予完成后服务将会自动开启。

<div style="display: flex; justify-content: center;">
    <img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/img/notificationAccess.jpg" alt="na">
</div>

4. 最后，如果你不止使用一个音乐软件，且其中有已经被 [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter) 适配的音乐软件时，使用 [LyricsGetter Ext](https://github.com/VictorModi/LyricsGetterExt) 将有可能重复输出歌词，为了避免这种情况请点击 `导入忽略应用规则` , 它将会从[Lyrics Getter](https://github.com/xiaowine/Lyric-Getter)获取规则文件然后自动将规则文件内的所有应用添加至忽略列表，这样就可以避免重复输出歌词的问题。
