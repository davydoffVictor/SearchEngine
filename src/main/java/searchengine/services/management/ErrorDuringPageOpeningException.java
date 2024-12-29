package searchengine.services.management;

public class ErrorDuringPageOpeningException extends RuntimeException{
    public ErrorDuringPageOpeningException(String message) {
        super(message);
    }
}
