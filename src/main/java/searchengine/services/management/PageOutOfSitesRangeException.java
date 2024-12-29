package searchengine.services.management;

public class PageOutOfSitesRangeException extends RuntimeException{
    public PageOutOfSitesRangeException(String message) {
        super(message);
    }
}
