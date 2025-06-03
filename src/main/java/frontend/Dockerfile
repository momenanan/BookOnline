# Use an OpenJDK base image
FROM openjdk:11-jre-slim

# Install curl
RUN apt-get update && apt-get install -y curl

# Set the working directory inside the container
WORKDIR /app

# Copy the compiled JAR file into the container
COPY target/FrontEndService.jar FrontEndService.jar

# Copy any necessary resource files (لو عندك، مثل ملفات config أو csv)
# COPY src/main/resources/... /app/...  ← مش مطلوب غالبًا للـ frontend

# Expose the port the service listens on
EXPOSE 4569

# Run the service
CMD ["java", "-jar", "FrontEndService.jar"]
