package frontend;

import static spark.Spark.*;
import java.net.http.*;
import java.net.URI;

public class FrontEndService {
    private static HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        port(4569);

        // Endpoint for searching books by topic
        get("/search/:topic", (req, res) -> {
            String topic = req.params(":topic");
            HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:4567/search/" + topic)).build(),
                HttpResponse.BodyHandlers.ofString());
            return response.body();
        });

        // Endpoint for retrieving book information by ID
        get("/info/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:4567/info/" + id)).build(),
                HttpResponse.BodyHandlers.ofString());
            return response.body();
        });

        // Endpoint for processing a book purchase by ID
        post("/purchase/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:4568/purchase/" + id))
                .POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
            return response.body();
        });
    }
}

