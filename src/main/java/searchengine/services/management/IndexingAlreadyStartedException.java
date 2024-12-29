package searchengine.services.management;

public class IndexingAlreadyStartedException extends RuntimeException{
    public IndexingAlreadyStartedException(String message) {
        super(message);
    }
}
