# =================================================================
# == 客製化 Elasticsearch Dockerfile (v2.0 - 最終批次安裝版)
# =================================================================

# 仍然使用官方的 Elasticsearch 鏡像作為基礎
FROM elasticsearch:8.13.4

# 【關鍵修正】在安裝指令中加入 --batch 參數，
# 這會讓它在非互動式環境中自動同意所有權限請求。
RUN ./bin/elasticsearch-plugin install --batch https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.13.4.zip
