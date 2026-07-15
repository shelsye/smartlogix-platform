FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY Componentes_Backend_Microservicios ./Componentes_Backend_Microservicios
RUN mvn -B -U -DskipTests -pl Componentes_Backend_Microservicios/inventory-service -am clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/Componentes_Backend_Microservicios/inventory-service/target/inventory-service-1.0.0.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
