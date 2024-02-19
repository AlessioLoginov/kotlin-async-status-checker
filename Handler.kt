fun interface Handler {
    fun performOperation(id: String): ApplicationStatusResponse
}
