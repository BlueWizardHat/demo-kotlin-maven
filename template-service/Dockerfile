# Build production image for Template-Service

FROM eclipse-temurin:25-jre-alpine

EXPOSE 8080
COPY template-service/target/template-service.jar .
CMD java -Duser.timezone=UTC -jar template-service.jar
