package sk42grid

import kotlin.math.*

/**
 * SK-42 Gauss–Krüger (Krasovsky) projection utilities and grid generation.
 *
 * Extracted per logic found in the app: uses Krasovsky ellipsoid (a=6378245.0), 6° zones (central meridian = zone*6 - 3),
 * and standard Gauss–Krüger (Transverse Mercator) series.
 *
 * API:
 * - generateGrid(bbox, stepMeters): produces grid lines (polylines) in WGS84 for rendering, aligned to SK-42 GK meters.
 * - forwardSK42(latDeg, lonDeg, zone): geodetic (deg) -> GK meters (X=Easting, Y=Northing) in SK-42 zone
 * - inverseSK42(x, y, zone): GK meters -> geodetic (deg) on Krasovsky, SK-42 datum
 */

data class LatLon(val latDeg: Double, val lonDeg: Double)
data class XY(val x: Double, val y: Double) // x=Easting (meters), y=Northing (meters)
data class BBox(val minLatDeg: Double, val minLonDeg: Double, val maxLatDeg: Double, val maxLonDeg: Double)
data class Polyline(val points: List<LatLon>)
data class GridLine(val points: List<LatLon>, val isVertical: Boolean, val valueMeters: Double)

object SK42 {
    // Krasovsky (SK-42) ellipsoid parameters
    private const val A: Double = 6378245.0            // semi-major axis (meters)
    private const val INV_F: Double = 298.3            // inverse flattening
    private val F: Double = 1.0 / INV_F
    private val B: Double = A * (1.0 - F)              // semi-minor axis
    private val E2: Double = (A * A - B * B) / (A * A) // first eccentricity squared
    private val EP2: Double = (A * A - B * B) / (B * B)

    private const val DEG_TO_RAD = Math.PI / 180.0
    private const val RAD_TO_DEG = 180.0 / Math.PI

    /** Determine 6° zone per SK-42 convention using longitude in degrees. */
    fun zoneFromLon(lonDeg: Double): Int {
        // Normalize to [0, 360) then apply zone = floor(lon/6) + 1
        val lon360 = ((lonDeg % 360.0) + 360.0) % 360.0
        return floor(lon360 / 6.0).toInt() + 1 // 1..60; e.g., 35.7E -> 6
    }

    /** Central meridian (radians) for a given 6° zone. */
    private fun lambda0Rad(zone: Int): Double {
        val lambda0Deg = zone * 6.0 - 3.0 // degrees
        return lambda0Deg * DEG_TO_RAD
    }

    /**
     * Forward Gauss–Krüger (Transverse Mercator) on Krasovsky to SK-42 metric grid.
     * Input: geodetic lat/lon in degrees, zone (1..60)
     * Output: XY where x=Easting, y=Northing, meters. False Easting omitted (0 at meridian), false northing 0.
     */
    fun forwardSK42(latDeg: Double, lonDeg: Double, zone: Int): XY {
        val phi = latDeg * DEG_TO_RAD
        val lambda = lonDeg * DEG_TO_RAD
        val lambda0 = lambda0Rad(zone)

        val sinPhi = sin(phi)
        val cosPhi = cos(phi)
        val tanPhi = tan(phi)

        val N = A / sqrt(1.0 - E2 * sinPhi * sinPhi)
        val eta2 = EP2 * cosPhi * cosPhi

        // Meridian arc length (Krüger series)
        val a0 = 1.0 - E2 / 4.0 - 3.0 * E2 * E2 / 64.0 - 5.0 * E2 * E2 * E2 / 256.0
        val a2 = 3.0 / 8.0 * (E2 + E2 * E2 / 4.0 + 15.0 * E2 * E2 * E2 / 128.0)
        val a4 = 15.0 / 256.0 * (E2 * E2 + 3.0 * E2 * E2 * E2 / 4.0)
        val a6 = 35.0 / 3072.0 * (E2 * E2 * E2)
        val M = A * (a0 * phi - a2 * sin(2.0 * phi) + a4 * sin(4.0 * phi) - a6 * sin(6.0 * phi))

        val dl = (lambda - lambda0)
        val dl2 = dl * dl
        val dl3 = dl2 * dl
        val dl4 = dl2 * dl2
        val dl5 = dl4 * dl
        val dl6 = dl3 * dl3

        val T = tanPhi * tanPhi

        // Scale factor k0 in SK-42 GK often = 1.0; keep 1.0 to match in-app GK behavior
        val k0 = 1.0

        val x = k0 * (
            M + N * tanPhi * (
                dl2 / 2.0 +
                (5.0 - T + 9.0 * eta2 + 4.0 * eta2 * eta2) * dl4 / 24.0 +
                (61.0 - 58.0 * T + T * T + 270.0 * eta2 - 330.0 * T * eta2) * dl6 / 720.0
            )
        )

        val xE = k0 * N * (
            dl * cosPhi +
            (1.0 - T + eta2) * dl3 * cosPhi * cosPhi * cosPhi / 6.0 +
            (5.0 - 18.0 * T + T * T + 14.0 * eta2 - 58.0 * T * eta2) * dl5 * cosPhi.pow(5) / 120.0
        )

        // Easting usually adds false easting of zone*1e6 + 500000 in some implementations.
        // App code determines zone by millionth place of Easting; we mimic classic Russian GK: X = 1e6*zone + 500000 + xE
        val easting = zone * 1_000_000.0 + 500_000.0 + xE
        val northing = x
        return XY(easting, northing)
    }

    /** Inverse Gauss–Krüger (approximate), returns geodetic lat/lon (deg) on Krasovsky. */
    fun inverseSK42(x: Double, y: Double, zone: Int): LatLon {
        // Remove false easting per our forward
        val xE = x - (zone * 1_000_000.0 + 500_000.0)
        val M = y // northing
        val k0 = 1.0

        // Footpoint latitude
        val a0 = 1.0 - E2 / 4.0 - 3.0 * E2 * E2 / 64.0 - 5.0 * E2 * E2 * E2 / 256.0
        val a2 = 3.0 / 8.0 * (E2 + E2 * E2 / 4.0 + 15.0 * E2 * E2 * E2 / 128.0)
        val a4 = 15.0 / 256.0 * (E2 * E2 + 3.0 * E2 * E2 * E2 / 4.0)
        val a6 = 35.0 / 3072.0 * (E2 * E2 * E2)

        val mu = M / (A * a0)
        val e1 = (1.0 - sqrt(1.0 - E2)) / (1.0 + sqrt(1.0 - E2))

        val J1 = 3.0 * e1 / 2.0 - 27.0 * e1 * e1 * e1 / 32.0
        val J2 = 21.0 * e1 * e1 / 16.0 - 55.0 * e1.pow(4) / 32.0
        val J3 = 151.0 * e1.pow(3) / 96.0
        val J4 = 1097.0 * e1.pow(4) / 512.0

        val phi1 = mu + J1 * sin(2.0 * mu) + J2 * sin(4.0 * mu) + J3 * sin(6.0 * mu) + J4 * sin(8.0 * mu)

        val sinPhi1 = sin(phi1)
        val cosPhi1 = cos(phi1)
        val tanPhi1 = tan(phi1)

        val N1 = A / sqrt(1.0 - E2 * sinPhi1 * sinPhi1)
        val R1 = A * (1.0 - E2) / (1.0 - E2 * sinPhi1 * sinPhi1).pow(1.5)
        val eta12 = EP2 * cosPhi1 * cosPhi1

        val D = xE / (N1 * k0)

        val phi = phi1 - (N1 * tanPhi1 / R1) * (
            D * D / 2.0 -
            (5.0 + 3.0 * tanPhi1 * tanPhi1 + 10.0 * eta12 - 4.0 * eta12 * eta12 - 9.0 * EP2) * D.pow(4) / 24.0 +
            (61.0 + 90.0 * tanPhi1 * tanPhi1 + 298.0 * eta12 + 45.0 * tanPhi1.pow(4) - 252.0 * EP2 - 3.0 * eta12 * eta12) * D.pow(6) / 720.0
        )

        val lambda0 = lambda0Rad(zone)
        val lambda = lambda0 + (
            D - (1.0 + 2.0 * tanPhi1 * tanPhi1 + eta12) * D.pow(3) / 6.0 +
            (5.0 + 28.0 * tanPhi1 * tanPhi1 + 24.0 * tanPhi1.pow(4) + 6.0 * eta12 + 8.0 * eta12 * tanPhi1 * tanPhi1) * D.pow(5) / 120.0
        ) / cosPhi1

        return LatLon(phi * RAD_TO_DEG, lambda * RAD_TO_DEG)
    }

    /** Generate SK-42 grid lines for a WGS84 bbox and step (meters). Lines returned in WGS84 for rendering. */
    fun generateGrid(bboxWgs84: BBox, stepMeters: Double): List<GridLine> {
        require(stepMeters > 0.0)

        // Use zone of bbox center
        val centerLat = 0.5 * (bboxWgs84.minLatDeg + bboxWgs84.maxLatDeg)
        val centerLon = 0.5 * (bboxWgs84.minLonDeg + bboxWgs84.maxLonDeg)
        val zone = zoneFromLon(centerLon)

        // Project bbox corners to SK-42 GK
        val p1 = forwardSK42(bboxWgs84.minLatDeg, bboxWgs84.minLonDeg, zone)
        val p2 = forwardSK42(bboxWgs84.minLatDeg, bboxWgs84.maxLonDeg, zone)
        val p3 = forwardSK42(bboxWgs84.maxLatDeg, bboxWgs84.minLonDeg, zone)
        val p4 = forwardSK42(bboxWgs84.maxLatDeg, bboxWgs84.maxLonDeg, zone)

        val minX = listOf(p1.x, p2.x, p3.x, p4.x).minOrNull() ?: p1.x
        val maxX = listOf(p1.x, p2.x, p3.x, p4.x).maxOrNull() ?: p1.x
        val minY = listOf(p1.y, p2.y, p3.y, p4.y).minOrNull() ?: p1.y
        val maxY = listOf(p1.y, p2.y, p3.y, p4.y).maxOrNull() ?: p1.y

        // Snap to grid
        fun snapUp(v: Double, s: Double) = ceil(v / s) * s
        fun snapDown(v: Double, s: Double) = floor(v / s) * s

        // Expand to cover bbox edges fully: include lines just outside by snapping outward
        var startX = snapDown(minX, stepMeters) - 5 * stepMeters
        var endX = snapUp(maxX, stepMeters) + 5 * stepMeters
        var startY = snapDown(minY, stepMeters) - 12 * stepMeters
        var endY = snapUp(maxY, stepMeters) + 12 * stepMeters

        val lines = mutableListOf<GridLine>()

        // Vertical lines (constant X), sampled densely along Y to follow GK curvature and cover bbox
        var x = startX
        while (x <= endX + 1e-6) {
            val pts = ArrayList<LatLon>()
            val segmentsV = 400
            val totalY = endY - startY
            val yStep = if (segmentsV > 0) totalY / segmentsV else totalY
            var yy = startY
            var i = 0
            while (i <= segmentsV) {
                val p = inverseSK42(x, yy, zone)
                pts.add(p)
                yy += yStep
                i++
            }
            val clipped = pts.filter { it.latDeg in bboxWgs84.minLatDeg..bboxWgs84.maxLatDeg && it.lonDeg in bboxWgs84.minLonDeg..bboxWgs84.maxLonDeg }
            if (clipped.size >= 2) {
                lines += GridLine(points = clipped, isVertical = true, valueMeters = x)
            }
            x += stepMeters
        }

        // Horizontal lines (constant Y), sampled densely along X
        var y = startY
        while (y <= endY + 1e-6) {
            val pts = ArrayList<LatLon>()
            val segmentsH = 600
            val totalX = endX - startX
            val xStep = if (segmentsH > 0) totalX / segmentsH else totalX
            var xx = startX
            var i = 0
            while (i <= segmentsH) {
                val p = inverseSK42(xx, y, zone)
                pts.add(p)
                xx += xStep
                i++
            }
            val clipped = pts.filter { it.latDeg in bboxWgs84.minLatDeg..bboxWgs84.maxLatDeg && it.lonDeg in bboxWgs84.minLonDeg..bboxWgs84.maxLonDeg }
            if (clipped.size >= 2) {
                lines += GridLine(points = clipped, isVertical = false, valueMeters = y)
            }
            y += stepMeters
        }

        return lines
    }
}


