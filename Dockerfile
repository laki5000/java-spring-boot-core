FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY api/ api/
COPY backend/.mvn/ backend/.mvn/
COPY backend/mvnw backend/pom.xml backend/
COPY backend/src/ backend/src/
WORKDIR /build/backend
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -Dmaven.test.skip=true
RUN cp target/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
RUN groupadd --gid 10001 spring && useradd --uid 10001 --gid spring --no-create-home spring
USER 10001:10001
WORKDIR /application
COPY --from=build /build/backend/extracted/dependencies/ ./
COPY --from=build /build/backend/extracted/spring-boot-loader/ ./
COPY --from=build /build/backend/extracted/snapshot-dependencies/ ./
COPY --from=build /build/backend/extracted/application/ ./
EXPOSE 8080 9091
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
