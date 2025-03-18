package ru.maplyb.printmap.impl.domain.model

data class PageFormat(
    val name: String,
    val width: Int,
    val height: Int
) {
    companion object {
        val A4 = PageFormat(name = "A4",595, 842)
        val A3 = PageFormat(name = "A3",842, 1191)
        val entries = listOf(A3, A4)
    }
}
