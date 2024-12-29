package searchengine.dto;

import lombok.Data;

@Data
public class AppError {
    private final Boolean result;
    private final String error;
}
