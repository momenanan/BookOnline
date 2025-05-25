package models;

public class Book {
    private int id;
    private String title;
    private String topic;
    private int quantity;
    private double price;

    public Book(int id, String title, String topic, int quantity, double price) {
        this.id = id;
        this.title = title;
        this.topic = topic;
        this.quantity = quantity;
        this.price = price;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    // Convert to CSV format for saving
    public String toCSV() {
        return id + "," + title + "," + topic + "," + quantity + "," + price;
    }
}
