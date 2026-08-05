package com.pucetec.teacolito.exceptions

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

    @ExceptionHandler(InvalidAmountException::class)
    fun handleInvalidAmount(ex: InvalidAmountException) =
        buildResponse(ex, HttpStatus.BAD_REQUEST)

    @ExceptionHandler(ShareMismatchException::class)
    fun handleShareMismatch(ex: ShareMismatchException) =
        buildResponse(ex, HttpStatus.BAD_REQUEST)

    @ExceptionHandler(NotGroupMemberException::class)
    fun handleNotGroupMember(ex: NotGroupMemberException) =
        buildResponse(ex, HttpStatus.FORBIDDEN)

    @ExceptionHandler(NotGroupCreatorException::class)
    fun handleNotGroupCreator(ex: NotGroupCreatorException) =
        buildResponse(ex, HttpStatus.FORBIDDEN)

    @ExceptionHandler(NotExpenseOwnerException::class)
    fun handleNotExpenseOwner(ex: NotExpenseOwnerException) =
        buildResponse(ex, HttpStatus.FORBIDDEN)

    @ExceptionHandler(NotSettlementOwnerException::class)
    fun handleNotSettlementOwner(ex: NotSettlementOwnerException) =
        buildResponse(ex, HttpStatus.FORBIDDEN)

    @ExceptionHandler(ExpenseGroupNotFoundException::class)
    fun handleExpenseGroupNotFound(ex: ExpenseGroupNotFoundException) =
        buildResponse(ex, HttpStatus.NOT_FOUND)

    @ExceptionHandler(ExpenseNotFoundException::class)
    fun handleExpenseNotFound(ex: ExpenseNotFoundException) =
        buildResponse(ex, HttpStatus.NOT_FOUND)

    @ExceptionHandler(SettlementNotFoundException::class)
    fun handleSettlementNotFound(ex: SettlementNotFoundException) =
        buildResponse(ex, HttpStatus.NOT_FOUND)

    @ExceptionHandler(InvitationNotFoundException::class)
    fun handleInvitationNotFound(ex: InvitationNotFoundException) =
        buildResponse(ex, HttpStatus.NOT_FOUND)

    @ExceptionHandler(GroupClosedException::class)
    fun handleGroupClosed(ex: GroupClosedException) =
        buildResponse(ex, HttpStatus.CONFLICT)

    @ExceptionHandler(MemberAlreadyExistsException::class)
    fun handleMemberAlreadyExists(ex: MemberAlreadyExistsException) =
        buildResponse(ex, HttpStatus.CONFLICT)

    @ExceptionHandler(DuplicateGroupNameException::class)
    fun handleDuplicateGroupName(ex: DuplicateGroupNameException) =
        buildResponse(ex, HttpStatus.CONFLICT)

    @ExceptionHandler(OutstandingBalanceException::class)
    fun handleOutstandingBalance(ex: OutstandingBalanceException) =
        buildResponse(ex, HttpStatus.CONFLICT)

    private fun buildResponse(ex: Exception, status: HttpStatus): ResponseEntity<ExceptionResponse> {
        val source = ex.stackTrace.firstOrNull()?.className ?: ex::class.simpleName.orEmpty()
        return ResponseEntity.status(status).body(ExceptionResponse(ex.message.orEmpty(), source))
    }
}
