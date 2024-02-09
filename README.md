# LyricsGetterExt

<div align="center">
    <img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/icon.svg" alt="icon" width="150">
    <p>Lyrics Getter · Ext</p>
</div>

( English / [简体中文](https://github.com/VictorModi/LyricsGetterExt/blob/main/docs/README_zh-CN.md) / [繁體中文](https://github.com/VictorModi/LyricsGetterExt/blob/main/docs/README_zh-TW.md) )

# What's this ?
It's a program to get internet lyrics.

It gets information about the currently playing media via [MediaController](https://developer.android.google.cn/reference/android/media/session/MediaController) and then gets the lyrics via the Internet, the Finally it pushes the lyrics to [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter).

It's base on [KaguraRinko/StatusBarLyricExt](https://github.com/KaguraRinko/StatusBarLyricExt). But we removed its system detection and added MusixMatch's lyrics source and adapted it to [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter) ! 
(...and...Yes...We also added an icon to it)

# How to use ?
1. You need to make sure that [LyricsGetter Ext](https://github.com/VictorModi/LyricsGetterExt) is checked in your Xposed Manager for [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter) while Lyrics Getter is running properly.

<div style="display: flex; justify-content: center;">
    <img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/img/how2use.jpg" alt="h2u">
</div>

2. Turn on [LyricsGetter Ext](https://github.com/VictorModi/LyricsGetterExt), turn it on and then grant [LyricsGetter Ext](https://github.com/VictorModi/LyricsGetterExt) notification permission, next you need to make sure that `Lyrics Getter Connection Status` is true and then turn on the switch `Enabled`.

<div style="display: flex; justify-content: center;">
    <img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/img/statusTrue.jpg" alt="st">
</div>

3. After clicking `Enable`, you will be taken to the Notification Access privilege management page, next, please grant [LyricsGetter Ext](https://github.com/VictorModi/LyricsGetterExt) Notification Next, please grant [LyricsGetter Ext](https://github.com/VictorModi/LyricsGetterExt) Notification Access privileges, and the service will start automatically after the authorization is completed.

<div style="display: flex; justify-content: center;">
    <img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/img/notificationAccess.jpg" alt="na">
</div>

4. Finally, if you are using more than one music app and one of the apps you are using has been adapted by [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter), the lyrics will be duplicated when you use that music app, to avoid this, please click `Input Ignored Apps Rules`, it will get the rules file from [Lyrics Getter](https://github.com/xiaowine/Lyric-Getter) and add all the apps in the rules file to the ignore list automatically, then you can avoid the problem of repeated lyrics output.

## Star History

<a href="https://star-history.com/#YouCanAi/LyricsGetterExt&Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=YouCanAi/LyricsGetterExt&type=Date&theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=YouCanAi/LyricsGetterExt&type=Date" />
    <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=YouCanAi/LyricsGetterExt&type=Date" />
  </picture>
</a>
