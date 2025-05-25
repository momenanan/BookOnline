package models;

public class Order {
    private int orderId;
    private int bookId;
    private int quantity;
    private String date;

    public Order(int orderId, int bookId, int quantity, String date) {
        this.orderId = orderId;
        this.bookId = bookId;
        this.quantity = quantity;
        this.date = date;
    }

    // Getters and Setters
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    // Convert to CSV format for saving
    public String toCSV() {
        return orderId + "," + bookId + "," + quantity + "," + date;
    }
}
