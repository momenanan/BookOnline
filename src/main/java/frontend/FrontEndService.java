package frontend;

import static spark.Spark.*;
import java.net.http.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FrontEndService {
    private static HttpClient client = HttpClient.newHttpClient();
    private static Map<String, String> cache = new HashMap<>();
    private static AtomicInteger catalogCounter = new AtomicInteger(0);
    private static AtomicInteger orderCounter = new AtomicInteger(0);
    private static final String[] catalogReplicas = {"http://catalog1:4567", "http://catalog2:4570"};
    private static final String[] orderReplicas = {"http://order1:4568", "http://order2:4571"};

    public static void main(String[] args) {
        port(4569);

        get("/search/:topic", (req, res) -> {
            String topic = req.params(":topic");
            String encodedTopic = URLEncoder.encode(topic, StandardCharsets.UTF_8);
            String fixedEncodedTopic = encodedTopic.replace("+", "%20");
            String cacheKey = "search:" + topic;

            try {
                if (cache.containsKey(cacheKey)) {
                    return cache.get(cacheKey);
                }

                HttpResponse<String> response = sendRoundRobinRequest(catalogReplicas, "/search/" + fixedEncodedTopic, catalogCounter);

                if (!response.body().equals("[]")) {
                    cache.put(cacheKey, response.body());
                }

                return response.body();
            } catch (Exception e) {
                res.status(500);
                return "Internal Server Error: " + e.getMessage();
            }
        });

        get("/info/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            String cacheKey = "info:" + id;

            try {
                if (cache.containsKey(cacheKey)) {
                    return cache.get(cacheKey);
                }

                HttpResponse<String> response = sendRoundRobinRequest(catalogReplicas, "/info/" + id, catalogCounter);

                if (!response.body().isEmpty()) {
                    cache.put(cacheKey, response.body());
                }

                return response.body();
            } catch (Exception e) {
                res.status(500);
                return "Internal Server Error: " + e.getMessage();
            }
        });

        post("/purchase/:id", (req, res) -> {
            System.out.println(">>> Received /purchase/:id request");
            req.body();
            int id = Integer.parseInt(req.params(":id"));
            try {
                HttpResponse<String> response = sendRoundRobinPostRequest(orderReplicas, "/purchase/" + id, orderCounter);
                cache.remove("info:" + id);
                return response.body();
            } catch (Exception e) {
                res.status(500);
                return "Internal Server Error: " + e.getMessage();
            }
        });

        post("/cache/invalidate", (req, res) -> {
            String key = req.queryParams("key");
            cache.remove(key);
            return "Cache invalidated for key: " + key;
        });

        System.out.println(">>> All routes registered: /search/:topic, /info/:id, /purchase/:id, /cache/invalidate");
    }

    private static HttpResponse<String> sendRoundRobinRequest(String[] replicas, String path, AtomicInteger counter) throws Exception {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < replicas.length) {
            String replica = replicas[counter.getAndIncrement() % replicas.length];
            String url = replica + path;

            try {
                System.out.println("Trying GET: " + url);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.err.println("Failed to reach GET: " + url + " -> " + e.getMessage());
                lastException = e;
                attempts++;
            }
        }

        throw lastException != null ? lastException : new Exception("All replicas failed.");
    }

    private static HttpResponse<String> sendRoundRobinPostRequest(String[] replicas, String path, AtomicInteger counter) throws Exception {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < replicas.length) {
            String replica = replicas[counter.getAndIncrement() % replicas.length];
            String url = replica + path;

            try {
                System.out.println("Trying POST: " + url);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody()) // POST بدون body
                    .build();
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.err.println("Failed to reach POST: " + url + " -> " + e.getMessage());
                lastException = e;
                attempts++;
            }
        }

        throw lastException != null ? lastException : new Exception("All replicas failed.");
    }
}
