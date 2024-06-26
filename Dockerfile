FROM eclipse-temurin:21-jre-alpine

COPY target/ops-svc*.jar /app/ops-svc.jar
WORKDIR /app
#Expose server port
EXPOSE 8080
#Expose gRPC port
EXPOSE 80
CMD ["java", "-jar", "ops-svc.jar"]