package order;

import static spark.Spark.*;
import models.Order;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import com.google.gson.Gson;
import models.Book;

public class OrderService {
    private static final String ORDERS_CSV = "/app/orders.csv"; 
    private static final String[] catalogReplicas = {"http://catalog1:4567", "http://catalog2:4570"};
    private static final String[] orderReplicas = {"http://order1:4568", "http://order2:4571"};
    private static List<Order> orders = new ArrayList<>();
    private static HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        port(4568); // Adjust for each replica
        loadOrders();

        post("/purchase/:id", (req, res) -> {
            int bookId = Integer.parseInt(req.params(":id"));
            boolean success = placeOrder(bookId);
            if (success) invalidateCache("info:" + bookId);
            res.type("text/plain");
            return success ? "Order placed" : "Out of stock";
        });

        post("/recordOrder", (req, res) -> {
            Order order = new Gson().fromJson(req.body(), Order.class);
            orders.add(order);
            saveOrders();
            return "Order recorded";
        });
    }

    private static boolean placeOrder(int bookId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(catalogReplicas[0] + "/info/" + bookId)) // Query one replica
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Book book = new Gson().fromJson(response.body(), Book.class);
                if (book.getQuantity() > 0) {
                    // Update stock on all catalog replicas
                    for (String catalogReplica : catalogReplicas) {
                        HttpRequest updateRequest = HttpRequest.newBuilder()
                            .uri(URI.create(catalogReplica + "/update/" + bookId + "?quantity=" + (book.getQuantity() - 1)))
                            .PUT(HttpRequest.BodyPublishers.noBody())
                            .build();
                        client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
                    }

                    // Record the order locally
                    Order order = new Order(orders.size() + 1, bookId, 1, LocalDate.now().toString());
                    orders.add(order);
                    saveOrders();

                    // Propagate the order to all replicas
                    propagateOrderToReplicas(order);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void propagateOrderToReplicas(Order order) {
        for (String orderReplica : orderReplicas) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(orderReplica + "/recordOrder"))
                    .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(order)))
                    .header("Content-Type", "application/json")
                    .build();
                client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void invalidateCache(String cacheKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://frontend:4569/cache/invalidate?key=" + cacheKey))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadOrders() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(ORDERS_CSV))) {
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int orderId = Integer.parseInt(parts[0]);
                int bookId = Integer.parseInt(parts[1]);
                int quantity = Integer.parseInt(parts[2]);
                String date = parts[3];
                orders.add(new Order(orderId, bookId, quantity, date));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveOrders() {
        try (PrintWriter writer = new PrintWriter(ORDERS_CSV)) {
            writer.println("orderId,bookId,quantity,date");
            for (Order order : orders) {
                writer.println(order.toCSV());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
