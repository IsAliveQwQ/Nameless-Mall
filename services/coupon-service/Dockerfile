# Dockerfile for coupon-service
# 與其他服務保持一致的單階段 Dockerfile

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# CI/CD 流程會將打包好的 .jar 檔案複製為 app.jar
COPY app.jar app.jar

# 聲明容器將會監聽的端口號 (coupon-service 使用 9008)
EXPOSE 9008

# 容器啟動時執行的指令
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-jar", "app.jar"]
