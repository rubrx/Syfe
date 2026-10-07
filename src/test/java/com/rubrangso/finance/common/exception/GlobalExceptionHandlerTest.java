package com.rubrangso.finance.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;

/**
 * Unit tests for {@link GlobalExceptionHandler}. Each test verifies that the correct HTTP status
 * and a properly structured {@link ErrorResponse} are returned for each exception type.
 * No Spring context is loaded — handler methods are called directly with a mocked request.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("ResourceNotFoundException returns 404 with message and no fieldErrors")
    void handleAppException_resourceNotFound_returns404() {
        var ex = new ResourceNotFoundException("Category not found");

        var response = handler.handleAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Category not found");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().fieldErrors()).isNull();
    }

    @Test
    @DisplayName("DuplicateResourceException returns 409")
    void handleAppException_duplicateResource_returns409() {
        var ex = new DuplicateResourceException("Category name already exists");

        var response = handler.handleAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Category name already exists");
    }

    @Test
    @DisplayName("ForbiddenOperationException returns 403")
    void handleAppException_forbidden_returns403() {
        var ex = new ForbiddenOperationException("Cannot delete a default category");

        var response = handler.handleAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().status()).isEqualTo(403);
    }

    @Test
    @DisplayName("BusinessValidationException returns 400")
    void handleAppException_businessValidation_returns400() {
        var ex = new BusinessValidationException("Category is in use by transactions");

        var response = handler.handleAppException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().fieldErrors()).isNull();
    }

    @Test
    @DisplayName("MethodArgumentNotValidException returns 400 with populated fieldErrors")
    void handleValidation_returns400WithFieldErrors() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "createTransactionRequest");
        bindingResult.addError(new FieldError("createTransactionRequest", "amount", "must be greater than 0"));
        bindingResult.addError(new FieldError("createTransactionRequest", "category", "must not be blank"));

        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().fieldErrors()).hasSize(2);
        assertThat(response.getBody().fieldErrors())
                .extracting(ErrorResponse.FieldErrorDetail::field)
                .containsExactlyInAnyOrder("amount", "category");
        assertThat(response.getBody().fieldErrors())
                .extracting(ErrorResponse.FieldErrorDetail::message)
                .containsExactlyInAnyOrder("must be greater than 0", "must not be blank");
    }

    @Test
    @DisplayName("ConstraintViolationException returns 400 with violations as fieldErrors")
    void handleConstraintViolation_returns400WithFieldErrors() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("goalName");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        var ex = new ConstraintViolationException(Set.of(violation));

        var response = handler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().fieldErrors()).hasSize(1);
        assertThat(response.getBody().fieldErrors().get(0).field()).isEqualTo("goalName");
        assertThat(response.getBody().fieldErrors().get(0).message()).isEqualTo("must not be blank");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException returns 400")
    void handleUnreadableMessage_returns400() {
        var ex = mock(HttpMessageNotReadableException.class);

        var response = handler.handleUnreadableMessage(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().fieldErrors()).isNull();
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException returns 400 with descriptive message")
    void handleTypeMismatch_returns400() {
        var ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getValue()).thenReturn("abc");
        when(ex.getName()).thenReturn("id");

        var response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("abc").contains("id");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException returns 400 with param name")
    void handleMissingParam_returns400() {
        var ex = new MissingServletRequestParameterException("startDate", "String");

        var response = handler.handleMissingParam(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("startDate");
    }

    @Test
    @DisplayName("NoResourceFoundException returns 404")
    void handleNoResource_returns404() {
        var ex = new NoResourceFoundException(HttpMethod.GET, "/api/unknown");

        var response = handler.handleNoResource(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException returns 405")
    void handleMethodNotAllowed_returns405() {
        var ex = new HttpRequestMethodNotSupportedException("PATCH");

        var response = handler.handleMethodNotAllowed(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().message()).contains("PATCH");
    }

    @Test
    @DisplayName("DataIntegrityViolationException returns 409")
    void handleDataIntegrity_returns409() {
        var ex = new DataIntegrityViolationException("Unique constraint violated");

        var response = handler.handleDataIntegrity(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
    }

    @Test
    @DisplayName("Unhandled Exception returns 500 with generic message")
    void handleGeneric_returns500() {
        var ex = new RuntimeException("Something went very wrong");

        var response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().fieldErrors()).isNull();
    }

    @Test
    @DisplayName("ErrorResponse timestamp is populated and path matches the request URI")
    void errorResponse_hasTimestampAndCorrectPath() {
        var ex = new ResourceNotFoundException("Goal not found");

        var response = handler.handleAppException(ex, request);

        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().error()).isEqualTo("Not Found");
    }
}
