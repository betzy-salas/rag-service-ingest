package com.rag.ingestion.controller.advice;

import com.openai.errors.OpenAIException;
import com.rag.ingestion.exception.ErrorMessages;
import com.rag.ingestion.exception.SpaceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates exceptions into RFC 9457 {@link ProblemDetail} responses.
 *
 * <p>Spring MVC errors, such as a missing {@code spaceKey} parameter, are handled by {@link
 * ResponseEntityExceptionHandler} with the same format.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(SpaceNotFoundException.class)
  ResponseEntity<ProblemDetail> handleSpaceNotFound(SpaceNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler({HttpClientErrorException.Unauthorized.class, HttpClientErrorException.Forbidden.class})
  ResponseEntity<ProblemDetail> handleConfluenceCredentials(HttpClientErrorException exception) {
    log.warn("Confluence rejected the credentials: {}", exception.getStatusCode());
    return problem(HttpStatus.BAD_GATEWAY, ErrorMessages.CONFLUENCE_CREDENTIALS_REJECTED);
  }

  @ExceptionHandler(RestClientException.class)
  ResponseEntity<ProblemDetail> handleConfluence(RestClientException exception) {
    log.warn("Error calling Confluence: {}", exception.getMessage());
    return problem(HttpStatus.BAD_GATEWAY, ErrorMessages.CONFLUENCE_UNAVAILABLE);
  }

  @ExceptionHandler(OpenAIException.class)
  ResponseEntity<ProblemDetail> handleOpenAi(OpenAIException exception) {
    log.warn("Error generating embeddings with OpenAI: {}", exception.getMessage());
    return problem(HttpStatus.BAD_GATEWAY, ErrorMessages.EMBEDDINGS_FAILED);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
    log.error("Unexpected error", exception);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessages.INTERNAL_ERROR);
  }

  private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
    return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
  }
}
