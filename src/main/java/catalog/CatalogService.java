package catalog;

import static spark.Spark.*;
import com.google.gson.Gson;
import models.Book;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class CatalogService {
    private static Map<Integer, Book> catalog = new HashMap<>();
    private static final String CSV_PATH = "src/main/resources/catalog.csv";

    public static void main(String[] args) {
        port(4567);
        loadCatalog();

        get("/search/:topic", (req, res) -> {
            String topic = req.params(":topic");
            List<Map<String, Object>> books = searchByTopic(topic);
            res.type("application/json");
            return new Gson().toJson(books);
        });

        get("/info/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Map<String, Object> bookInfo = getBookInfo(id);
            res.type("application/json");
            return new Gson().toJson(bookInfo);
        });

        // Endpoint to update book stock quantity
        put("/update/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            int quantity = Integer.parseInt(req.queryParams("quantity"));
            updateBookStock(id, quantity);
            res.type("text/plain");
            return "Updated";
        });
    }

    private static void loadCatalog() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(CSV_PATH))) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");

                if (parts.length < 5) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }

                try {
                    int id = !parts[0].trim().isEmpty() ? Integer.parseInt(parts[0].trim()) : -1;
                    String title = parts[1].trim();
                    String topic = parts[2].trim();
                    int quantity = !parts[3].trim().isEmpty() ? Integer.parseInt(parts[3].trim()) : 0;
                    double price = !parts[4].trim().isEmpty() ? Double.parseDouble(parts[4].trim()) : 0.0;

                    if (id != -1 && quantity >= 0) {
                        catalog.put(id, new Book(id, title, topic, quantity, price));
                    } else {
                        System.err.println("Skipping line due to invalid id or quantity: " + line);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Skipping line due to number format issue: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Map<String, Object>> searchByTopic(String topic) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Book book : catalog.values()) {
            if (book.getTopic().equalsIgnoreCase(topic)) {
                Map<String, Object> bookData = new HashMap<>();
                bookData.put("id", book.getId());
                bookData.put("title", book.getTitle());
                results.add(bookData);
            }
        }
        return results;
    }

    private static Map<String, Object> getBookInfo(int id) {
        Book book = catalog.get(id);
        if (book != null) {
            Map<String, Object> bookInfo = new HashMap<>();

            bookInfo.put("title", book.getTitle());
            bookInfo.put("quantity", book.getQuantity());
            bookInfo.put("price", book.getPrice());

            return bookInfo;
        }
        return Collections.emptyMap();
    }

    private static void updateBookStock(int id, int quantity) {
        Book book = catalog.get(id);
        if (book != null) {
            book.setQuantity(quantity);
            saveCatalog();
        }
    }

    private static void saveCatalog() {
        try (PrintWriter writer = new PrintWriter(CSV_PATH)) {
            writer.println("id,title,topic,quantity,price");
            for (Book book : catalog.values()) {
                writer.println(book.toCSV());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
