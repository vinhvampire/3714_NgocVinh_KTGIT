# Stage 1: Build - Sử dụng Maven với Java 21 
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy mã nguồn vào container để build [cite: 1]
COPY . .

# Đóng gói file .jar (bỏ qua chạy thử nghiệm test để build nhanh hơn) [cite: 1]
RUN mvn clean package -DskipTests

# Stage 2: Run - Sử dụng JRE 21 nhẹ hơn để chạy [cite: 1]
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar đã build từ Stage 1 sang Stage 2 [cite: 1]
# Dùng *.jar để tự động nhận diện đúng tên file trong thư mục target [cite: 1]
COPY --from=build /app/target/*.jar app.jar

# Mở cổng 8080 cho ứng dụng [cite: 1]
EXPOSE 8080

# Lệnh khởi chạy ứng dụng [cite: 1]
ENTRYPOINT ["java", "-jar", "app.jar"]