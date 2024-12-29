package searchengine.services.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.AppError;
import searchengine.services.management.ErrorDuringPageOpeningException;
import searchengine.services.management.IndexingAlreadyStartedException;
import searchengine.services.management.IndexingNotStartedException;
import searchengine.services.management.PageOutOfSitesRangeException;
import searchengine.services.search.WrongSearchQueryException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<AppError> catchIndexingAlreadyStartedException(IndexingAlreadyStartedException e) {
        log.error(e.getMessage(), e);
        return new ResponseEntity<>(new AppError(false, e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<AppError> catchIndexingANotStartedException(IndexingNotStartedException e) {
        log.error(e.getMessage(), e);
        return new ResponseEntity<>(new AppError(false, e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<AppError> catchPageOutOfSitesRangeException(PageOutOfSitesRangeException e) {
        log.error(e.getMessage(), e);
        return new ResponseEntity<>(new AppError(false, e.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler
    public ResponseEntity<AppError> catchErrorDuringPageOpeningException(ErrorDuringPageOpeningException e) {
        log.error(e.getMessage(), e);
        return new ResponseEntity<>(new AppError(false, e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<AppError> catchWrongSearchQueryException(WrongSearchQueryException e) {
        log.error(e.getMessage(), e);
        return new ResponseEntity<>(new AppError(false, e.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
