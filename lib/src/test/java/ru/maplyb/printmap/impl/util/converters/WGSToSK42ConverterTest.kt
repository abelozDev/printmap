package ru.maplyb.printmap.impl.util.converters

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.charset.Charset
import kotlin.math.roundToInt
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WGSToSK42ConverterTest {

    private lateinit var converter: WGSToSK42Converter

    @BeforeAll
    fun setUp() {
        converter = WGSToSK42Converter()
    }

    @Test
    fun testChar() {
        val utf8String = "Артём"
        val cp1251Bytes = utf8String.toByteArray(Charset.forName("windows-1251"))
        val decodedString = String(cp1251Bytes, Charset.forName("windows-1251"))
        print(decodedString)
    }
    @Test
    fun `test wgs to sk42 coordinate converter`() {
        val wgsLat = 55.790282708
        val wgsLon = 37.528073993
        val (sk42Lat, sk42Lon) = converter.wgs84ToSk42(wgsLat, wgsLon)
        println("СК-42 широта: $sk42Lat")
        println("СК-42 долгота: $sk42Lon")
        assertEquals(6186302, sk42Lat.roundToInt())
        assertEquals(7407668, sk42Lon.roundToInt())

    }
}