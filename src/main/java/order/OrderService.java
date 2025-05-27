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
    private static final String ORDERS_CSV = "src/main/resources/orders.csv";
    private static List<Order> orders = new ArrayList<>();
    private static HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) {
        port(4568);
        loadOrders();

        // Handle purchase request
        post("/purchase/:id", (req, res) -> {
            int bookId = Integer.parseInt(req.params(":id"));
            boolean success = placeOrder(bookId);
            res.type("text/plain");
            return success ? "Order placed" : "Out of stock";
        });
    }

    private static boolean placeOrder(int bookId) {
        try {
            // Step 1: Check stock from CatalogService
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:4567/info/" + bookId))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse JSON response to get quantity
                Book book = new Gson().fromJson(response.body(), Book.class);
                if (book.getQuantity() > 0) {
                    // Step 2: Update stock in CatalogService
                    HttpRequest updateRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:4567/update/" + bookId + "?quantity=" + (book.getQuantity() - 1)))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();
                    client.send(updateRequest, HttpResponse.BodyHandlers.ofString());

                    // Step 3: Record the order if successful
                    Order order = new Order(orders.size() + 1, bookId, 1, LocalDate.now().toString());
                    orders.add(order);
                    saveOrders();
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;  // Out of stock or error
    }

    private static void loadOrders() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(ORDERS_CSV))) {
            String line;
            reader.readLine();  // Skip header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;  // Skip empty lines
                String[] parts = line.split(",");
                if (parts.length < 4) continue;  // Skip incomplete lines

                int orderId = Integer.parseInt(parts[0]);
                int bookId = Integer.parseInt(parts[1]);
                int quantity = Integer.parseInt(parts[2]);
                String date = parts[3];
                orders.add(new Order(orderId, bookId, quantity, date));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Error parsing integer in orders file: " + e.getMessage());
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