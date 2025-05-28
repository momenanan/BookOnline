package frontend;

import static spark.Spark.*;
import java.net.http.*;
import java.net.URI;
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
            String cacheKey = "search:" + topic;
            if (cache.containsKey(cacheKey)) {
                return cache.get(cacheKey);
            }
            HttpResponse<String> response = sendRoundRobinRequest(catalogReplicas, "/search/" + topic, catalogCounter);
            cache.put(cacheKey, response.body());
            return response.body();
        });

        get("/info/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            String cacheKey = "info:" + id;
            if (cache.containsKey(cacheKey)) {
                return cache.get(cacheKey);
            }
            HttpResponse<String> response = sendRoundRobinRequest(catalogReplicas, "/info/" + id, catalogCounter);
            cache.put(cacheKey, response.body());
            return response.body();
        });

        post("/purchase/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            HttpResponse<String> response = sendRoundRobinRequest(orderReplicas, "/purchase/" + id, orderCounter);
            cache.remove("info:" + id);
            return response.body();
        });

        post("/cache/invalidate", (req, res) -> {
            String key = req.queryParams("key");
            cache.remove(key);
            return "Cache invalidated for key: " + key;
        });
    }

    private static HttpResponse<String> sendRoundRobinRequest(String[] replicas, String path, AtomicInteger counter) throws Exception {
        String url = replicas[counter.getAndIncrement() % replicas.length] + path;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

