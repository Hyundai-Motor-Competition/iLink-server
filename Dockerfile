# 빌드는 Jenkins가 한다: ./gradlew test bootJar → docker build. 이미지에는 build/libs/app.jar만 들어간다
FROM eclipse-temurin:21.0.12.1_1-jre-noble AS extract
WORKDIR /build
COPY build/libs/app.jar app.jar
# 의존성 · 애플리케이션 레이어를 나눠, 코드만 바뀐 빌드는 작은 레이어만 새로 푸시한다
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21.0.12.1_1-jre-noble
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=extract /build/extracted/dependencies/ ./
COPY --from=extract /build/extracted/spring-boot-loader/ ./
COPY --from=extract /build/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/extracted/application/ ./
USER 10001
ENV TZ=Asia/Seoul
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
