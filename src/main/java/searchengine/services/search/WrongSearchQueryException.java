package searchengine.services.search;

public class WrongSearchQueryException extends RuntimeException{
    public WrongSearchQueryException(String message) {
        super(message);
    }
}
