package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Setter


public class Site {
    private String url;
    private String name;

    public String getUrl() {
        return (url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
    }

    public String getName() {
        return name;
    }
}
