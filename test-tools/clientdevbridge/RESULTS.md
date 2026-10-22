# 1.21.1 beta Bridge 結果

2026-09-30（Asia/Taipei）。CG 2.0.0-beta3，原始碼基底 3cc6546 加上本次無效規則預覽修正。

| Profile | 遊戲檢查 | 圖像比對 | 結果 |
| --- | ---: | ---: | --- |
| neo-jei | 46 | 3 | 通過 |
| neo-emi | 46 | 3 | 通過 |
| 合計 | 92 | 6 | 通過 |

固定 Minecraft 1.21.1、NeoForge 21.1.249、JEI 19.42.0.379／EMI 1.1.24、Java 21、Bridge 1.0.0 build 171。兩個 profile 使用相同核心案例，數字包含跨 viewer 重複驗證。

最新完整證據：`evidence/suite-2026-09-30T06-34-31-588Z`；彙整：`evidence/final-verification.json`。完整本機證據受 Git 忽略。

- 各 40 項核心操作、混合成員、搜尋、修飾鍵／拖曳、規則、歸屬與冷啟動檢查。
- 各 6 項新版批次選取、停用／啟用、分類移動、側欄與冷啟動分類保存檢查。
- 各 3 項已檢視區域的圖像比對：混合群組管理器、無效規則預覽、分類移動後的管理器。
- Common JUnit：1,122 項，零失敗／錯誤；runner：13 項通過。
- NeoForge build、修正後的 common:test／NeoForge jar、Fabric／Forge compileJava 通過。
- 正常退出、程序結束、設定按原位元組還原、端口關閉及產物 SHA 核對通過。
- NeoForge 產品 jar 不含 Bridge／probe 類別。

未修正 beta 曾在 invalid-rule-retains-last-valid-preview 重現物品預覽遺失；修正後在兩個 viewer 通過。失敗、修正與產物雜湊詳見 [實作紀錄](../../docs/bridge-beta-regression-20260930.md)。

本次範圍不含 Fabric／Forge 的 beta 遊戲實跑，也不含 KubeJS／generic／中文矩陣及效能測量。1.5.0 的先前結果不能代替 beta 驗證。重跑方式見 [README.md](README.md)。

## Historical scope

The results above apply to the dated beta3 artifacts and pinned dependencies only. They do not certify the current checkout, beta5, or the updated JEI pins. Use the README to generate new evidence for the exact deployed artifacts.
