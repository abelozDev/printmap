package ru.maplyb.printmap.api.model


sealed interface OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>
    data class Error(val message: String) : OperationResult<Nothing>
    fun successOrNull(): OperationResult<T>? = if (this is Success) this else null
    fun successDataOrNull(): T? = if (this is Success) this.data else null
}


sealed interface Errors {
    data class NetworkError(val message: String) : Errors
    data class LogicalError(val message: String) : Errors
    data object SizeError: Errors
}