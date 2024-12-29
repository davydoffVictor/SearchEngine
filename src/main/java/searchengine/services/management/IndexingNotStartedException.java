package searchengine.services.management;

public class IndexingNotStartedException extends RuntimeException{
    public IndexingNotStartedException(String message) {
        super(message);
    }
}
