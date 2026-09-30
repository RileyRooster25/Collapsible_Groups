# ClientDevBridge beta 回歸

獨立遊戲測試工具，使用 Prism 啟動實際發布 jar。ClientDevBridge 提供通訊、世界操作與截圖；probe 提供原生輸入與繪製後的唯讀觀察。產品的 Gradle 與發布 jar 不包含 Bridge 或 probe。

本分支受測範圍是 Minecraft 1.21.1、CG 2.0.0-beta3、NeoForge 21.1.249 的純 JEI／純 EMI。實作與失敗修正見 [beta 回歸紀錄](../../docs/bridge-beta-regression-20260930.md)，完成結果見 [RESULTS.md](RESULTS.md)。其他保留的 profile／案例尚未完成 beta 適配驗證。

## 環境與準備

需要 Windows、Node.js 22+、JDK 21，以及已登入並可啟動 Minecraft 的 Prism Launcher。畫面固定 1280 × 800、GUI scale 2、60 FPS、en_us；拖曳時遊戲需保持焦點。Bridge 1.0.0 build 171、JEI 19.42.0.379、EMI 1.1.24 的完整版本與 SHA 位於 dependencies.json。

1. 複製 local.example.json 為 local.json，填入 Prism、Java 21 與 NeoForge 21.1.249 的 mmc-pack.json 路徑。只讀取 pack 版本，不複製個人世界、mods 或設定。
2. 將鎖定的 NeoForge Bridge、JEI、EMI jar 放入 companion/libs。也可透過 local.json 的 dependencyFiles 指定來源；stage 會核對 SHA。
3. 在本資料夾執行以下準備。JAVA_HOME 指向 JDK 21；不需要 npm install。

```powershell
../../gradlew.bat :common:test :neoforge:jar
node runner/stage.mjs neo-jei neo-emi
../../gradlew.bat -p companion build
node runner/prepare.mjs neo-jei neo-emi
```

若 Gradle 遇到 Unable to establish loopback connection，可設定 `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=D:/cg-bridge-no-socket`，該目錄刻意不存在以使用 TCP fallback。

prepare 建立獨立的 `CG Bridge Beta <profile> 20260930` 實例，使用 25831／25832 端口與所有權標記；同名實例已存在時拒絕覆蓋。產品版本及檔名讀取根目錄 gradle.properties；probe 的產品相依版本亦需同步。

## 執行

```powershell
node --test runner/*.test.mjs
node runner/run.mjs neo-jei neo-emi
node runner/audit-evidence.mjs neo-jei neo-emi
```

更新產品或 probe 後，重建、stage、重建 probe，再對已停止的實例執行 `node runner/deploy.mjs neo-jei neo-emi`。任一案例失敗即停止；執行結果或設定還原失敗皆回傳非零結束碼。

每個 profile 包含 40 項核心／規則／生命週期檢查，以及 6 項 beta 批次操作、分類移動與冷啟動檢查。三張圖像基準檢查混合群組管理器、無效規則預覽、分類移動後的管理器。圖像只比較已檢視的文字與控制項區域，排除動畫圖示、游標與世界背景；不自動接受新的基準。

## 證據與隔離

- 身分核對包含 session、程序、gameDir、loader、Minecraft、模組版本、jar 路徑及 SHA。
- 輸入走 MouseHandler／KeyboardHandler，修飾鍵在操作結束後釋放。觀察不呼叫 CG setter、重建索引或搜尋。
- 通過判定需三個不同 frame 與 tick；畫面、搜尋或 generation 改變會重新等待。
- 保存另外核對磁碟 JSON，退出後重新啟動檢查保存結果。
- 測前備份 config/collapsiblegroups，分類 fixture 也在此範圍。正常退出並確認程序結束後按原位元組還原。
- 無法確認退出時保留證據與備份，不任意終止 Java。恢復前需核對測試實例的 session、程序和端口。
- evidence 保存快照、截圖、遊戲日誌、備份與退出結果。audit 採各 profile 最新的非負控制結果，不以舊成功掩蓋新失敗。

## 來源

通訊與世界控制沿用 [ClientDevBridge](https://github.com/CyclopsMC/ClientDevBridge)，研究基準 c8f60830079b7e80272c953985da4674fd3595ab。CG 的輸入、觀察、案例與還原由本工具實作。
