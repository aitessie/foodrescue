package com.example.foodrescue.offerservice.adapter.`in`

import com.example.foodrescue.offerservice.adapter.`in`.dtos.ApiExceptionResponse
import com.example.foodrescue.offerservice.application.exceptions.*
import com.example.foodrescue.offerservice.application.exceptions.AccessDeniedException as ApplicationAccessDeniedException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import java.time.Clock
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException as SpringAccessDeniedException
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler(private val clock: Clock) {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.BAD_REQUEST,
            code = "VALIDATION_ERROR",
            message = validationMessage(exception.bindingResult),
            request = request,
        )

    @ExceptionHandler(BindException::class)
    fun handleBindException(
        exception: BindException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.BAD_REQUEST,
            code = "VALIDATION_ERROR",
            message = validationMessage(exception.bindingResult),
            request = request,
        )

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(
        exception: HandlerMethodValidationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.BAD_REQUEST,
            code = "VALIDATION_ERROR",
            message =
                exception.allErrors
                    .mapNotNull { error -> error.defaultMessage }
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted()
                    .takeIf(List<String>::isNotEmpty)
                    ?.joinToString("; ") ?: "Request validation failed",
            request = request,
        )

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        exception: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> {
        val message =
            exception.constraintViolations
                .map { violation -> violation.message }
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
                .takeIf(List<String>::isNotEmpty)
                ?.joinToString("; ") ?: "Request validation failed"

        return response(
            status = HttpStatus.BAD_REQUEST,
            code = "VALIDATION_ERROR",
            message = message,
            request = request,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.BAD_REQUEST,
            code = "MALFORMED_REQUEST_BODY",
            message = "Request body is malformed or contains an invalid value",
            request = request,
        )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(
        exception: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> {
        val invalidUuid = exception.requiredType == UUID::class.java

        return response(
            status = HttpStatus.BAD_REQUEST,
            code =
                if (invalidUuid) {
                    "INVALID_UUID"
                } else {
                    "INVALID_PARAMETER"
                },
            message =
                if (invalidUuid) {
                    "${exception.name} must be a valid UUID"
                } else {
                    "${exception.name} contains an invalid value"
                },
            request = request,
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingRequestParameter(
        exception: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.BAD_REQUEST,
            code = "MISSING_PARAMETER",
            message = "${exception.parameterName} is required",
            request = request,
        )

    @ExceptionHandler(ApplicationAccessDeniedException::class)
    fun handleApplicationAccessDenied(
        exception: ApplicationAccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.FORBIDDEN,
            code = "ACCESS_DENIED",
            message = exception.message?.takeIf(String::isNotBlank) ?: "Access is denied",
            request = request,
        )

    @ExceptionHandler(SpringAccessDeniedException::class)
    fun handleSpringAccessDenied(
        exception: SpringAccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.FORBIDDEN,
            code = "ACCESS_DENIED",
            message = "Access is denied",
            request = request,
        )

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(
        exception: NotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.NOT_FOUND,
            code = "NOT_FOUND",
            message = exception.message?.takeIf(String::isNotBlank) ?: "Resource was not found",
            request = request,
        )

    @ExceptionHandler(EntityVersionConflictException::class)
    fun handleEntityVersionConflict(
        exception: EntityVersionConflictException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.CONFLICT,
            code = "ENTITY_VERSION_CONFLICT",
            message = exception.message?.takeIf(String::isNotBlank) ?: "Entity version conflict",
            request = request,
        )

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailure(
        exception: OptimisticLockingFailureException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.CONFLICT,
            code = "ENTITY_VERSION_CONFLICT",
            message = "Entity was modified by another request",
            request = request,
        )

    @ExceptionHandler(InvalidStateException::class)
    fun handleInvalidState(
        exception: InvalidStateException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.CONFLICT,
            code = "INVALID_STATE",
            message =
                exception.message?.takeIf(String::isNotBlank)
                    ?: "Operation is not allowed in the current state",
            request = request,
        )

    @ExceptionHandler(PartnerServiceUnavailableException::class)
    fun handlePartnerServiceUnavailable(
        exception: PartnerServiceUnavailableException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            code = "PARTNER_SERVICE_UNAVAILABLE",
            message = "Partner Service is temporarily unavailable",
            request = request,
        )

    @ExceptionHandler(PartnerServiceAuthenticationException::class)
    fun handlePartnerServiceAuthentication(
        exception: PartnerServiceAuthenticationException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            code = "PARTNER_SERVICE_AUTHENTICATION_FAILED",
            message = "Partner Service authentication is unavailable",
            request = request,
        )

    @ExceptionHandler(PartnerServiceContractException::class)
    fun handlePartnerServiceContract(
        exception: PartnerServiceContractException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        response(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            code = "PARTNER_SERVICE_CONTRACT_ERROR",
            message = "Partner Service returned an invalid response",
            request = request,
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> {
        logger.error(
            "Unexpected Offer Service request failure",
            exception,
        )

        return response(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            code = "INTERNAL_ERROR",
            message = "An unexpected error occurred",
            request = request,
        )
    }

    private fun validationMessage(bindingResult: BindingResult): String {
        val fieldMessages =
            bindingResult.fieldErrors.map { error ->
                "${error.field}: ${error.defaultMessage ?: "invalid value"}"
            }
        val globalMessages =
            bindingResult.globalErrors.map { error ->
                error.defaultMessage ?: "invalid request"
            }

        return (fieldMessages + globalMessages)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
            .takeIf(List<String>::isNotEmpty)
            ?.joinToString("; ") ?: "Request validation failed"
    }

    private fun response(
        status: HttpStatus,
        code: String,
        message: String,
        request: HttpServletRequest,
    ): ResponseEntity<ApiExceptionResponse> =
        ResponseEntity.status(status)
            .body(
                ApiExceptionResponse(
                    timestamp = clock.instant(),
                    status = status.value(),
                    code = code,
                    message = message,
                    path = request.requestURI,
                )
            )

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
