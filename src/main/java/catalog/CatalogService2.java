package catalog;

import static spark.Spark.*;
import com.google.gson.Gson;
import models.Book;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class CatalogService2 {
    private static Map<Integer, Book> catalog = new HashMap<>();
    private static final String CSV_PATH = "/app/catalog.csv"; 

    public static void main(String[] args) {
        port(4570); 
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
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0].trim());
                String title = parts[1].trim();
                String topic = parts[2].trim();
                int quantity = Integer.parseInt(parts[3].trim());
                double price = Double.parseDouble(parts[4].trim());
                catalog.put(id, new Book(id, title, topic, quantity, price));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Map<String, Object>> searchByTopic(String topic) {
        List<Map<String, Object>> results = new ArrayList<>();
        String cleanTopic = topic.trim().toLowerCase();

        for (Book book : catalog.values()) {
            if (book.getTopic().trim().toLowerCase().equals(cleanTopic)) {
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
