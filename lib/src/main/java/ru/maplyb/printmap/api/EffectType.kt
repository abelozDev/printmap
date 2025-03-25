package ru.maplyb.printmap.api

import android.graphics.DashPathEffect
import android.graphics.Path
import android.graphics.PathDashPathEffect
import android.graphics.PathEffect

enum class PathEffectTypes(
    val effect1: PathEffect,
    val effect2: PathEffect? = null
) {
    RYBEZH_ALLY(
        effect1 = DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply { addRect(0f, 0f, 8f, 20f, Path.Direction.CW) },
            15f,
            10f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    RYBEZH_ENEMY(
        effect1 = DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply { addRect(0f, 0f, 8f, 20f, Path.Direction.CW) },
            15f,
            10f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    WIREFENCE_ALLY(
        effect1 = DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                addRect(-3f, -15f, 3f, 15f, Path.Direction.CW)
            },
            25f,
            10f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    WIREFENCE_ENEMY(
        effect1 =
            DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                addRect(-3f, -15f, 3f, 15f, Path.Direction.CW)
            },
            25f,
            10f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    LOWVISIBLEFENCE_ALLY(
        effect1 =
            DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                addOval(
                    android.graphics.RectF(-5f, -12f, 5f, 12f),
                    Path.Direction.CW
                ) // Овал без заливки
            },
            13f, // Расстояние между овалами
            4f,  // Смещение первого элемента
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    LOWVISIBLEFENCE_ENEMY(
        effect1 =
            DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                addOval(
                    android.graphics.RectF(-5f, -12f, 5f, 12f),
                    Path.Direction.CW
                ) // Овал без заливки
            },
            13f, // Расстояние между овалами
            4f,  // Смещение первого элемента
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    SOLID(PathEffect()),
    TRENCH_INSIDE(
        effect1 = DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply { addRect(0f, 0f, 10f, 20f, Path.Direction.CW) },
            25f,
            10f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    TRENCH_OUTSIDE(
        effect1 = DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply { addRect(0f, 0f, -10f, -20f, Path.Direction.CW) },
            25f,
            10f,
            PathDashPathEffect.Style.ROTATE
        ),

        ),
    ATGM_PTUR_INSIDE(
        effect1 =
            DashPathEffect(floatArrayOf(50f, 50f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                moveTo(20f, 22.5f)
                lineTo(40f, -17.5f)
                lineTo(30f, -22.5f)
                lineTo(20f, -5f)
                lineTo(10f, -22.5f)
                lineTo(0f, -17.5f)
                close()
            },
            100f,
            50f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    ATGM_PTUR_OUTSIDE(
        effect1 =
            DashPathEffect(floatArrayOf(50f, 50f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                moveTo(20f, -22.5f)
                lineTo(40f, 17.5f)
                lineTo(30f, 22.5f)
                lineTo(20f, 5f)
                lineTo(10f, 22.5f)
                lineTo(0f, 17.5f)
                close()
            },
            100f,
            50f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    TANK_LINE(
        effect1 = DashPathEffect(floatArrayOf(50f, 50f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                lineTo(0f, -20f)
                lineTo(10f, -20f)
                lineTo(10f, 20f)
                lineTo(0f, 20f)
                close()
            },
            100f,
            25f,
            PathDashPathEffect.Style.ROTATE
        ),

        ),
    IFV_BTR_LINE(
        effect1 = DashPathEffect(floatArrayOf(50f, 50f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                moveTo(10f, -20f)
                lineTo(20f, -10f)
                lineTo(10f, 0f)
                lineTo(20f, 10f)
                lineTo(10f, 20f)
                lineTo(0f, 10f)
                lineTo(-10f, 20f)
                lineTo(-20f, 10f)
                lineTo(-10f, 0f)
                lineTo(-20f, -10f)
                lineTo(-10f, -20f)
                lineTo(0f, -10f)
                close()
            },
            100f,
            25f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    ARMS_FRONTIER_INSIDE(
        effect1 = DashPathEffect(floatArrayOf(50f, 50f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                moveTo(-5f, 25f)
                lineTo(5f, 25f)
                lineTo(5f, -15f)
                lineTo(20f, -15f)
                lineTo(20f, -25f)
                lineTo(-20f, -25f)
                lineTo(-20f, -15f)
                lineTo(-5f, -15f)
                close()
            },
            100f,
            25f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    ARMS_FRONTIER_OUTSIDE(
        effect1 =
            DashPathEffect(floatArrayOf(50f, 50f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                moveTo(-5f, -25f)
                lineTo(5f, -25f)
                lineTo(5f, 15f)
                lineTo(20f, 15f)
                lineTo(20f, 25f)
                lineTo(-20f, 25f)
                lineTo(-20f, 15f)
                lineTo(-5f, 15f)
                close()
            },
            100f,
            25f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    MINING(
        effect1 = DashPathEffect(floatArrayOf(50f, 50f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                addCircle(0f, 0f, 10f, Path.Direction.CW)
            },
            100f,
            25f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    WIRE_FENCE1(
        effect1 =
            DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                addRect(-5f, -20f, 5f, 20f, Path.Direction.CW)
            },
            100f,
            25f,
            PathDashPathEffect.Style.ROTATE

        ),
    ),
    WIRE_FENCE2(
        effect1 = DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                addRect(-10f, -20f, -5f, 20f, Path.Direction.CW)
                addRect(5f, -20f, 10f, 20f, Path.Direction.CW)
            },
            100f,
            25f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    WIRE_FENCE3(
        effect1 =
            DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                addRect(-10f, -20f, -5f, 20f, Path.Direction.CW)
                addRect(0f, -20f, 5f, 20f, Path.Direction.CW)
                addRect(10f, -20f, 15f, 20f, Path.Direction.CW)
            },
            100f,
            25f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    WIRE_FENCE4(
        effect1 =
            DashPathEffect(floatArrayOf(50f, 0f), 0f),
        effect2 = PathDashPathEffect(
            Path().apply {
                addRect(-20f, -20f, -15f, 20f, Path.Direction.CW)
                addRect(-10f, -20f, -5f, 20f, Path.Direction.CW)
                addRect(0f, -20f, 5f, 20f, Path.Direction.CW)
                addRect(10f, -20f, 15f, 20f, Path.Direction.CW)
            },
            100f,
            25f,
            PathDashPathEffect.Style.ROTATE
        ),
    ),
    LARGE_GAP(effect1 = DashPathEffect(floatArrayOf(40f, 20f), 0f)),
    MEDIUM_GAP(effect1 = DashPathEffect(floatArrayOf(20f, 15f), 0f)),
    SMALL_GAP(effect1 = DashPathEffect(floatArrayOf(10f, 10f), 0f)),
    DOTTED(effect1 = DashPathEffect(floatArrayOf(5f, 10f), 0f)),
    DASH_DOT(effect1 = DashPathEffect(floatArrayOf(40f, 15f, 10f, 15f), 0f)),
    LONG_DASH_SHORT_DASH(effect1 = DashPathEffect(floatArrayOf(30f, 10f, 10f, 10f), 0f)),
    LONG_DASH_DOT(effect1 = DashPathEffect(floatArrayOf(30f, 5f, 10f, 5f), 0f)),
    SHORT_DASHES(effect1 = DashPathEffect(floatArrayOf(15f, 5f, 5f, 5f), 0f));
}