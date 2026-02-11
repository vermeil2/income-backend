# Nexus에서 다운받은 JAR로 이미지 빌드 (Jenkins가 JAR을 빌드 컨텍스트에 넣어줌)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget \
    && addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# 빌드 컨텍스트에 있는 단일 JAR (Jenkins에서 Nexus에서 받은 JAR을 넣음)
COPY *.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
