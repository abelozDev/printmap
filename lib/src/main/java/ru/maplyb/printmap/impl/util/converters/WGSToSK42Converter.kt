package ru.maplyb.printmap.impl.util.converters

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class WGSToSK42Converter {

    private val a = 6378245.0          // Большая полуось
    private val e2 = 0.006693421622966  // Эксцентриситет
    private val e = sqrt(e2)
    private val ep2 = e2 / (1 - e2)    // Второй эксцентриситет в квадрате

    fun wgs84ToSk42(latDeg: Double, lonDeg: Double): Pair<Double, Double> {
        val lat = Math.toRadians(latDeg)
        val lon = Math.toRadians(lonDeg)

        val zone = (lonDeg / 6).toInt() + 1
        val lon0Deg = zone * 6 - 3
        val lon0 = Math.toRadians(lon0Deg.toDouble())

        val l = lon - lon0
        val sinLat = sin(lat)
        val cosLat = cos(lat)
        val tanLat = tan(lat)

        val N = a / sqrt(1 - e2 * sinLat * sinLat)
        val T = tanLat * tanLat
        val C = e2 / (1 - e2) * cosLat * cosLat
        val A = cosLat * l

        val M = a * (
                (1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256) * lat
                        - (3 * e2 / 8 + 3 * e2 * e2 / 32 + 45 * e2 * e2 * e2 / 1024) * sin(2 * lat)
                        + (15 * e2 * e2 / 256 + 45 * e2 * e2 * e2 / 1024) * sin(4 * lat)
                        - (35 * e2 * e2 * e2 / 3072) * sin(6 * lat)
                )

        val x = M + N * tanLat * (
                A * A / 2 +
                        (5 - T + 9 * C + 4 * C * C) * A * A * A * A / 24 +
                        (61 - 58 * T + T * T) * A * A * A * A * A * A / 720
                )

        val y = N * (
                A +
                        (1 - T + C) * A * A * A / 6 +
                        (5 - 18 * T + T * T + 72 * C - 58 * e2) * A * A * A * A * A / 120
                ) + zone * 1_000_000 + 500_000  // добавляем смещение зоны

        return Pair(x, y)
    }

    fun sk42ToWgs84(x: Double, y: Double, zone: Int): Pair<Double, Double> {
        // Убираем смещение зоны
        val y0 = y - (zone * 1_000_000 + 500_000)
        
        // Центральный меридиан зоны
        val lon0 = (zone * 6 - 3).toDouble()
        
        // Первое приближение широты
        var lat = x / 6367558.4968
        
        var M: Double
        var mu: Double
        
        // Итеративно уточняем широту
        for (i in 0..5) {
            mu = lat
            
            M = a * (
                    (1 - e2/4 - 3*e2*e2/64 - 5*e2*e2*e2/256) * mu -
                    (3*e2/8 + 3*e2*e2/32 + 45*e2*e2*e2/1024) * sin(2*mu) +
                    (15*e2*e2/256 + 45*e2*e2*e2/1024) * sin(4*mu) -
                    (35*e2*e2*e2/3072) * sin(6*mu)
            )
            
            lat = mu + (x - M) / a
        }
        
        val sinLat = sin(lat)
        val cosLat = cos(lat)
        val tanLat = tan(lat)
        
        val N = a / sqrt(1 - e2 * sinLat * sinLat)
        val T = tanLat * tanLat
        val C = ep2 * cosLat * cosLat
        
        // Вычисляем долготу
        val lon = lon0 + Math.toDegrees(
            (y0 / (N * cosLat)) -
            (1 - T + C) * Math.pow(y0 / (N * cosLat), 3.0) / 6 -
            (5 - 18*T + T*T + 72*C - 58*ep2) * Math.pow(y0 / (N * cosLat), 5.0) / 120
        )
        
        // Вычисляем широту
        val phi = Math.toDegrees(lat) -
                (N * tanLat / a) * (
                    (y0*y0)/(2*N*N) +
                    (5 + 3*T + 10*C - 4*C*C - 9*ep2) * Math.pow(y0/N, 4.0) / 24 +
                    (61 + 90*T + 298*C + 45*T*T - 252*ep2 - 3*C*C) * Math.pow(y0/N, 6.0) / 720
                )
        
        return Pair(phi, lon)
    }

    // Вспомогательный метод для преобразования координат из СК-42 в WGS84 с автоматическим определением зоны
    fun sk42ToWgs84(lat: Double, lon: Double): Pair<Double, Double> {
        // Определяем зону по долготе
        val zone = (lon / 6).toInt() + 1
        return sk42ToWgs84(lat, lon, zone)
    }

}