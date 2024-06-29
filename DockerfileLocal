FROM eclipse-temurin:21-jre-alpine

COPY target/ops-svc*.jar /app/ops-svc.jar
WORKDIR /app
#Expose server port
EXPOSE 50020
#Expose gRPC port
EXPOSE 50025
CMD ["java", "-jar", "ops-svc.jar"]