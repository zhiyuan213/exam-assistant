# WeChat Cloud Run builds from the repository root.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /build/target/exam-assistant-1.0.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Shanghai
USER app
EXPOSE 8080
# Fetch missing DigiCert intermediates while retaining normal PKIX/hostname checks.
ENTRYPOINT ["java", "-Dcom.sun.security.enableAIAcaIssuers=true", "-Dcom.sun.security.allowedAIALocations=http://cacerts.digicert.cn/ http://cacerts.digicert.com/", "-jar", "app.jar"]
