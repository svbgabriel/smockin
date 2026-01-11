FROM bellsoft/liberica-runtime-container:jdk-11-musl AS builder

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src src

RUN ./mvnw clean package -DskipTests

FROM bellsoft/liberica-runtime-container:jre-11-slim-musl AS optimizer

WORKDIR /app

COPY --from=builder /app/target/smockin.jar smockin.jar

RUN java -Djarmode=layertools -jar smockin.jar extract

FROM bellsoft/liberica-runtime-container:jre-11-slim-musl

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

EXPOSE 8000-8003

COPY --from=optimizer /app/dependencies/ ./
COPY --from=optimizer /app/snapshot-dependencies/ ./
COPY --from=optimizer /app/spring-boot-loader/ ./
COPY --from=optimizer /app/application/ ./

CMD ["java", "org.springframework.boot.loader.JarLauncher"]
