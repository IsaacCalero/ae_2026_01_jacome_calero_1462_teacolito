package com.pucetec.users.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ExceptionResponse(
    val message: String,
    val source: String
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BlankFieldException::class)
    fun handleBlankField(ex: BlankFieldException) =
        buildResponse(ex, HttpStatus.BAD_REQUEST)

    @ExceptionHandler(DuplicateDisplayNameException::class)
    fun handleDuplicateDisplayName(ex: DuplicateDisplayNameException) =
        buildResponse(ex, HttpStatus.CONFLICT)

    @ExceptionHandler(UserProfileAlreadyExistsException::class)
    fun handleUserProfileAlreadyExists(ex: UserProfileAlreadyExistsException) =
        buildResponse(ex, HttpStatus.CONFLICT)

    @ExceptionHandler(UserProfileNotFoundException::class)
    fun handleUserProfileNotFound(ex: UserProfileNotFoundException) =
        buildResponse(ex, HttpStatus.NOT_FOUND)

    private fun buildResponse(ex: Exception, status: HttpStatus): ResponseEntity<ExceptionResponse> {
        val source = ex.stackTrace.firstOrNull()?.className ?: ex::class.simpleName.orEmpty()
        return ResponseEntity.status(status).body(ExceptionResponse(ex.message.orEmpty(), source))
    }
}
