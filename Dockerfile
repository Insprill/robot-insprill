####################################################################################################
## Builder
####################################################################################################
FROM gradle:8.10-jdk21-alpine AS builder

WORKDIR /robot-insprill

COPY . .

RUN gradle build --no-daemon

####################################################################################################
## Final image
####################################################################################################
# JavaCPP presets for Tesseract doesn't support Alpine :/
FROM eclipse-temurin:21-jre

# Copy our build
COPY --from=builder /robot-insprill/build/libs/robot-insprill*.jar /robot-insprill.jar

CMD ["java", "-jar", "robot-insprill.jar"]
