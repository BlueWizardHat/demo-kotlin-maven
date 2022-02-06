# Build production image for Template-Service

FROM eclipse-temurin:17-jre-alpine

EXPOSE 8080
COPY template-service/target/template-service.jar .
CMD java -Duser.timezone=UTC -jar template-service.jar
