package ru.mapolib.printmap.gui.utils.scale

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScaleTests {

    // Моковая функция pixelsPerCm, так как она используется в основном коде
    private fun pixelsPerCm(dpi: Float): Float {
        return dpi / 2.54f
    }

    @Test
    fun `test scale rounding and segment length calculation`() {
        // Входные данные
        val bitmapWidth = 1000 // Ширина изображения в пикселях
        val scale = 50000 // Масштаб, количество метров в 1 см

        // Ожидаемые результаты
        val expectedRoundedScale = 500 // Ожидаемое округление масштаба до 500
        val expectedSegmentLengthWithNewScale = 2647.18f // Ожидаемая длина отрезка после округления

        // Вызываем функцию
        val (roundedScale, segmentLengthWithNewScale) = roundScale(bitmapWidth, scale)

        // Проверяем результат
        assertEquals(expectedRoundedScale, roundedScale)
        assertEquals(expectedSegmentLengthWithNewScale, segmentLengthWithNewScale, 0.01f)
    }

    @Test
    fun `test different bitmap width and scale values`() {
        // Входные данные
        val bitmapWidth = 2000 // Ширина изображения в пикселях
        val scale = 100000 // Масштаб, количество метров в 1 см

        // Ожидаемые результаты
        val expectedRoundedScale = 1000 // Ожидаемое округление масштаба до 1000
        val expectedSegmentLengthWithNewScale = 5294.36f // Ожидаемая длина отрезка после округления

        // Вызываем функцию
        val (roundedScale, segmentLengthWithNewScale) = roundScale(bitmapWidth, scale)

        // Проверяем результат
        assertEquals(expectedRoundedScale, roundedScale)
        assertEquals(expectedSegmentLengthWithNewScale, segmentLengthWithNewScale, 0.01f)
    }

    @Test
    fun `test scale with small bitmap width`() {
        // Входные данные
        val bitmapWidth = 500 // Ширина изображения в пикселях
        val scale = 20000 // Масштаб, количество метров в 1 см

        // Ожидаемые результаты
        val expectedRoundedScale = 500 // Ожидаемое округление масштаба до 500
        val expectedSegmentLengthWithNewScale = 1323.59f // Ожидаемая длина отрезка после округления

        // Вызываем функцию
        val (roundedScale, segmentLengthWithNewScale) = roundScale(bitmapWidth, scale)

        // Проверяем результат
        assertEquals(expectedRoundedScale, roundedScale)
        assertEquals(expectedSegmentLengthWithNewScale, segmentLengthWithNewScale, 0.01f)
    }

    @Test
    fun `test scale with extreme scale value`() {
        // Входные данные
        val bitmapWidth = 1000 // Ширина изображения в пикселях
        val scale = 1000000 // Масштаб, количество метров в 1 см

        // Ожидаемые результаты
        val expectedRoundedScale = 1000 // Ожидаемое округление масштаба до 1000
        val expectedSegmentLengthWithNewScale = 529436.36f // Ожидаемая длина отрезка после округления

        // Вызываем функцию
        val (roundedScale, segmentLengthWithNewScale) = roundScale(bitmapWidth, scale)

        // Проверяем результат
        assertEquals(expectedRoundedScale, roundedScale)
        assertEquals(expectedSegmentLengthWithNewScale, segmentLengthWithNewScale, 0.01f)
    }
}
