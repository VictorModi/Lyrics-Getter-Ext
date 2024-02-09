# Lyrics Getter 擴充

<div align="center">
<!-- <img src="https://github.com/VictorModi/LyricsGetterExt/edit/main/icon.svg" alt="icon" width="500"> -->
<img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/icon.svg" alt="icon" width="150">
<p>LyricsGetter · 擴充</p>
</div>

( [English](https://github.com/VictorModi/LyricsGetterExt/blob/main/README.md) / [简体中文](https://github.com/VictorModi/LyricsGetterExt/blob/main/docs/README_zh-CN.md) / 繁體中文 )

# 這是什麼？
這是一個用於獲取網絡歌詞的軟體。

它通過 [MediaController](https://developer.android.google.cn/reference/android/media/session/MediaController) 獲取當前正在播放的媒體信息，然後自動搜索歌詞並推送給 Lyrics Getter。

基於 [KaguraRinko/StatusBarLyricExt](https://github.com/KaguraRinko/StatusBarLyricExt)。但我們已經移除了系統檢測，加入了 MusixMatch 歌詞源並通過 API 適配了 Lyrics Getter！
（還有...是的...我們還為它添加了一個圖標...）
# 注意

如果你使用 Lyrics Getter 擴充 是爲了使用狀態欄歌詞，請確認 [StatusBarLyric](https://github.com/Block-Network/StatusBarLyric) 在你的裝置上是否正確設定。是否在測試模式下成功 Hook 上狀態欄，能夠正常顯示內容。

[相關教學](https://blog.xiaowine.cc/posts/8e64/)

# 如何使用？
1. 確保 Lyrics Getter 正常運行後，前往你的 Xposed 管理器，勾選 Lyrics Getter 擴展。

<div style="display: flex; justify-content: center;">
<img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/img/how2use.jpg" alt="h2u">
</div>

2. 打開 LyricsGetter 擴展，授予通知權限給 LyricsGetter 擴展，確保 "Lyrics Getter 連接狀態" 顯示為 "true"，然後開啟開關。

<div style="display: flex; justify-content: center;">
<img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/img/statusTrue.jpg" alt="st">
</div>

3. 點擊 "開啟" 後，你會進入 "通知使用權" 配置頁面，請在該頁面授予 LyricsGetter 擴展通知使用權，授予完成後服務將會自動開啟。

<div style="display: flex; justify-content: center;">
<img src="https://raw.githubusercontent.com/VictorModi/LyricsGetterExt/main/img/notificationAccess.jpg" alt="na">
</div>

4. 最後，如果你有多個音樂軟體，且其中有 Lyrics Getter 已適配的音樂軟體，使用 LyricsGetter 擴展可能會重複輸出歌詞。為了避免這種情況，請點擊 "匯入忽略應用程式規則"，它將從 Lyrics Getter 獲取規則文件並自動將文件中的所有應用添加到忽略列表中，這樣就可以避免重複輸出歌詞的問題。
