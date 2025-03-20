package ru.maplyb.printmap.impl.domain.model

data class PageFormat(
    val name: String,
    val width: Int,
    val height: Int
) {
    companion object {
        val A4 = PageFormat(name = "A4",210, 297)
        val A3 = PageFormat(name = "A3",297, 420)
        val entries = listOf(A3, A4)
    }
}
