package space.harbour.cloud.payments;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

/**
 * Turns validation failures into clean {@code 400 Bad Request} responses
 * describing which fields or headers were wrong.
 */
@RestControllerAdvice(assignableTypes = PaymentController.class)
public class PaymentExceptionHandler {

	/** Invalid JSON body fields (e.g. missing price, bad currency). */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail onBodyValidation(MethodArgumentNotValidException ex) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return problem(detail.isEmpty() ? "Invalid request body" : detail);
	}

	/** Invalid header values (e.g. blank Store-Id or Idempotency-Key). */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ProblemDetail onHeaderValidation(HandlerMethodValidationException ex) {
		String detail = ex.getAllErrors().stream()
				.map(error -> error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return problem(detail.isEmpty() ? "Invalid request" : detail);
	}

	private ProblemDetail problem(String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle("Payment registration failed");
		return problem;
	}
}
