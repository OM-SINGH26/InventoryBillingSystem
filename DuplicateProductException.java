package exceptions;

public class DuplicateProductException extends Exception {
    public DuplicateProductException(String productId) {
        super("Product with ID '" + productId + "' already exists.");
    }
}
