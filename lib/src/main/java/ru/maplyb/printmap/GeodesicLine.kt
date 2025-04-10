package ru.maplyb.printmap

import kotlin.math.*

private const val a = 6378137.0            // Большая полуось (экваториальный радиус) WGS-84
private const val f = 1 / 298.257223563    // Сжатие эллипсоида
private const val b = a * (1 - f)          // Малая полуось

data class LatLon(val lat: Double, val lon: Double)

fun getGeodesicLine(
    startLat: Double,
    startLon: Double,
    endLat: Double,
    endLon: Double,
    stepMeters: Double
): List<LatLon> {
    val results = mutableListOf<LatLon>()

    val distanceAndAzimuth = vincentyInverse(startLat, startLon, endLat, endLon)
    val totalDistance = distanceAndAzimuth.distance
    val azimuth = distanceAndAzimuth.azimuth

    val steps = (totalDistance / stepMeters).toInt()

    for (i in 0..steps) {
        val d = i * stepMeters
        results.add(vincentyDirect(startLat, startLon, azimuth, d))
    }

    // Включаем конечную точку точно
    results.add(LatLon(endLat, endLon))

    return results
}

private data class VincentyResult(val distance: Double, val azimuth: Double)

private fun vincentyInverse(lat1: Double, lon1: Double, lat2: Double, lon2: Double): VincentyResult {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val lambda1 = Math.toRadians(lon1)
    val lambda2 = Math.toRadians(lon2)

    val U1 = atan((1 - f) * tan(phi1))
    val U2 = atan((1 - f) * tan(phi2))
    val L = lambda2 - lambda1
    var lambda = L

    var sinSigma: Double
    var cosSigma: Double
    var sigma: Double
    var cos2SigmaM: Double
    var sinAlpha: Double
    var cosSqAlpha: Double

    var lambdaPrev: Double
    var iterLimit = 100
    do {
        val sinLambda = sin(lambda)
        val cosLambda = cos(lambda)
        sinSigma = sqrt(
            (cos(U2) * sinLambda).pow(2) +
                    (cos(U1) * sin(U2) - sin(U1) * cos(U2) * cosLambda).pow(2)
        )
        if (sinSigma == 0.0) return VincentyResult(0.0, 0.0) // co-incident points
        cosSigma = sin(U1) * sin(U2) + cos(U1) * cos(U2) * cosLambda
        sigma = atan2(sinSigma, cosSigma)
        sinAlpha = cos(U1) * cos(U2) * sinLambda / sinSigma
        cosSqAlpha = 1 - sinAlpha.pow(2)
        cos2SigmaM = if (cosSqAlpha == 0.0) 0.0 else cosSigma - 2 * sin(U1) * sin(U2) / cosSqAlpha
        val C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha))
        lambdaPrev = lambda
        lambda = L + (1 - C) * f * sinAlpha * (
                sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM.pow(2)))
                )
    } while (abs(lambda - lambdaPrev) > 1e-12 && --iterLimit > 0)

    val uSq = cosSqAlpha * (a * a - b * b) / (b * b)
    val A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)))
    val B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)))
    val deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (
            cosSigma * (-1 + 2 * cos2SigmaM.pow(2)) -
                    B / 6 * cos2SigmaM * (-3 + 4 * sinSigma.pow(2)) * (-3 + 4 * cos2SigmaM.pow(2))
            ))

    val distance = b * A * (sigma - deltaSigma)

    val azimuth = atan2(
        cos(U2) * sin(lambda),
        cos(U1) * sin(U2) - sin(U1) * cos(U2) * cos(lambda)
    )

    return VincentyResult(distance, Math.toDegrees(azimuth))
}

private fun vincentyDirect(lat1: Double, lon1: Double, azimuthDeg: Double, distance: Double): LatLon {
    val alpha1 = Math.toRadians(azimuthDeg)
    val phi1 = Math.toRadians(lat1)
    val lambda1 = Math.toRadians(lon1)

    val U1 = atan((1 - f) * tan(phi1))
    val sigma1 = atan2(tan(U1), cos(alpha1))
    val sinAlpha = cos(U1) * sin(alpha1)
    val cosSqAlpha = 1 - sinAlpha.pow(2)
    val uSq = cosSqAlpha * (a * a - b * b) / (b * b)
    val A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)))
    val B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)))

    var sigma = distance / (b * A)
    var sigmaP: Double
    var iterations = 100
    do {
        val cos2SigmaM = cos(2 * sigma1 + sigma)
        val sinSigma = sin(sigma)
        val cosSigma = cos(sigma)
        val deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (
                cosSigma * (-1 + 2 * cos2SigmaM.pow(2)) -
                        B / 6 * cos2SigmaM * (-3 + 4 * sinSigma.pow(2)) * (-3 + 4 * cos2SigmaM.pow(2))
                ))
        sigmaP = sigma
        sigma = distance / (b * A) + deltaSigma
    } while (abs(sigma - sigmaP) > 1e-12 && --iterations > 0)

    val tmp = sin(U1) * sin(sigma) - cos(U1) * cos(sigma) * cos(alpha1)
    val phi2 = atan2(
        sin(U1) * cos(sigma) + cos(U1) * sin(sigma) * cos(alpha1),
        (1 - f) * sqrt(sinAlpha.pow(2) + tmp.pow(2))
    )
    val lambda = atan2(
        sin(sigma) * sin(alpha1),
        cos(U1) * cos(sigma) - sin(U1) * sin(sigma) * cos(alpha1)
    )
    val C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha))
    val L = lambda - (1 - C) * f * sinAlpha * (
            sigma + C * sin(sigma) * (
                    cos(2 * sigma1 + sigma) + C * cos(sigma) * (-1 + 2 * cos(2 * sigma1 + sigma).pow(2))
                    )
            )

    val lon2 = (lambda1 + L + 3 * PI) % (2 * PI) - PI

    return LatLon(Math.toDegrees(phi2), Math.toDegrees(lon2))
}
