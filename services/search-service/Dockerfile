# Dockerfile for Spring Boot services (e.g., gateway, user-service, auth-service, product-service etc.)
# 這是一個與 CI/CD 流程兼容的、簡單的單階段 Dockerfile。

# [關鍵修正] 使用一個穩定且明確的 JRE 17 slim 鏡像標籤
# eclipse-temurin 是高品質的 OpenJDK 發行版，廣泛用於生產環境
FROM eclipse-temurin:17-jre-jammy

# 設定工作目錄
WORKDIR /app

# [重要] 這個 Dockerfile 預期 CI/CD 流程已經執行了 `cp` 指令，
# 將打包好的 .jar 檔案複製為 app.jar，並將其放置在 Docker build 的 context 中。
COPY app.jar app.jar

# 聲明容器將會監聽的端口號 (僅為文件作用)
# 例如，在 product-service 中，您可以寫 EXPOSE 8082
EXPOSE 14000

# 容器啟動時執行的指令
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-jar", "app.jar"]
