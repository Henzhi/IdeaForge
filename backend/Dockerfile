# ===== Docker 多阶段构建 (对应 Docker部署设计.md) =====
# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
# 先拷贝 pom 利用缓存加速依赖下载
COPY pom.xml .
COPY ideaforge-common/pom.xml ideaforge-common/
COPY ideaforge-user/pom.xml ideaforge-user/
COPY ideaforge-idea/pom.xml ideaforge-idea/
COPY ideaforge-story/pom.xml ideaforge-story/
COPY ideaforge-generation/pom.xml ideaforge-generation/
COPY ideaforge-notification/pom.xml ideaforge-notification/
COPY ideaforge-app/pom.xml ideaforge-app/
RUN mvn -B dependency:go-offline
# 拷贝源码并打包
COPY ideaforge-common/src ideaforge-common/src
COPY ideaforge-user/src ideaforge-user/src
COPY ideaforge-idea/src ideaforge-idea/src
COPY ideaforge-story/src ideaforge-story/src
COPY ideaforge-generation/src ideaforge-generation/src
COPY ideaforge-notification/src ideaforge-notification/src
COPY ideaforge-app/src ideaforge-app/src
RUN mvn -B clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 非 root 用户
RUN groupadd -r app && useradd -r -g app app

COPY --from=builder /build/ideaforge-app/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dfile.encoding=UTF-8"
EXPOSE 8080

USER app
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
