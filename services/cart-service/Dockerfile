# 使用 eclipse-temurin 作為基礎鏡像，這是一個穩定且輕量的 Java 運行環境
FROM eclipse-temurin:17-jre-jammy

# 設定容器內的工作目錄
WORKDIR /app

# 複製由 CI/CD 流程預先準備好的 app.jar 檔案到容器中
# 您的 CI/CD 工作流會負責執行：
# cp ./services/cart-service/target/cart-service-1.0.0-SNAPSHOT.jar ./services/cart-service/app.jar
COPY app.jar app.jar

# 聲明容器將會監聽的端口號 (僅為文件作用)
EXPOSE 11000

# 容器啟動時執行的指令
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-jar", "app.jar"]
