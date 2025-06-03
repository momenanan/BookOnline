# Use an OpenJDK base image
FROM openjdk:11-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the compiled .jar file into the container
COPY target/CatalogService.jar CatalogService.jar

#  Copy the catalog.csv file into the container
COPY src/main/resources/catalog.csv /app/catalog.csv

# Expose the port the service listens on
EXPOSE 4567

# Run the service
CMD ["java", "-jar", "CatalogService.jar"]
