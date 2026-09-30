# 1.21.1 beta Bridge 回歸

日期：2026-09-30（Asia/Taipei）。產品基準為 `3cc6546`、CG `2.0.0-beta3`。此次範圍為 NeoForge + JEI 與 NeoForge + EMI；不沿用 CG 1.5.0 的通過數字作為 beta 驗證結果。

## 實作

- 將獨立 Bridge runner／probe 導入 beta 分支，產品建置及發布依賴不加入測試模組。
- 從產品版本設定產生 jar 名稱，測試實例使用獨立名稱、端口、session 與檔案雜湊。
- 管理器按鈕位置改讀取 `ManagerHeaderLayout` 與 `ManagerContentLayout`；編輯器選取狀態改讀取已發布的 contents projection。
- 新增兩個群組的批次停用／啟用、分類移動、側欄切換及冷啟動分類保存檢查。分類 fixture 隨 CG 設定一併備份還原。
- 圖像基準重新擷取、檢視後固定；1.5.0 的舊版面不作為 beta 的視覺基準。

## 發現與修正

未修正 beta 在 `suite-2026-09-30T06-27-08-714Z/neo-jei` 通過前 26 項後，於 `invalid-rule-retains-last-valid-preview` 失敗。把 `minecraft:stone` 改成 `minecraft:stone!` 會阻擋儲存，但石頭預覽消失，泥土與水仍在。

`EditorRightPanel` 現在於目前篩選與實際預覽篩選不一致時，從最後有效篩選解析物品；有效草稿維持索引路徑。`EditorInvalidPreviewTest` 檢查物品、流體、generic 採用同一有效篩選，並確認修復規則後恢復索引路徑。

第一次工具啟動曾因 probe 的產品依賴仍鎖定 1.5.0 而在模組載入時失敗；已修正為 beta3。該程序依 session 與實例名稱核對後結束，備份還原確認為零個原始設定檔。這次啟動錯誤不計入產品測試通過數字。

## 驗證

最新完整實跑為 `test-tools/clientdevbridge/evidence/suite-2026-09-30T06-34-31-588Z`。NeoForge + JEI 與 NeoForge + EMI 各 46 項遊戲檢查、3 項圖像比對通過，合計 92 + 6。每項保留三個不同 frame／tick；兩次遊戲程序均正常退出，CG 設定按原位元組還原，端口已關閉。

`audit-evidence.mjs neo-jei neo-emi` 核對最新非負控制結果、目前部署產物、退出與還原證據通過。共用 JUnit 1,122 項零失敗／錯誤；runner 13 項通過。NeoForge build 通過，修正後重新執行 common:test 與 NeoForge jar，Fabric／Forge compileJava 亦通過。發布 jar 不含 Bridge／probe 類別。

| 產物 | SHA-256 |
| --- | --- |
| NeoForge CG beta3 | 310934dbd9255e6ec398d0b327bdf7fdfb92b625056db8d78fcb8cc2c344ffbe |
| NeoForge probe | 7bf8640de32dc3273dae9ba3627d8dd0a02aa285c1eb89911804ed40a3b85d0a |

圖像基準來自修正後、完整語意檢查通過的 `suite-2026-09-30T06-29-58-515Z/neo-jei`，檢視後在最終實跑對兩個 viewer 重新驗證。排除動畫物品與世界背景，並非所有解析度的全畫面驗收。

本次未把 Fabric／Forge 遊戲實跑、KubeJS／generic／中文矩陣或效能測量宣稱為 beta 新增驗證；舊版 285 項紀錄不併入上述數字。這些案例仍可另行適配擴充。
