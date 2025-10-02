package ru.mapolib.printmap.gui.presentation.downloaded

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.LayerObject
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.excel_generator.deleteExcelFile
import ru.maplyb.printmap.impl.excel_generator.exportToExcel
import ru.maplyb.printmap.impl.excel_generator.sendExcelFile
import ru.maplyb.printmap.impl.util.draw_on_bitmap.DrawOnBitmap
import ru.maplyb.printmap.impl.files.FileUtil
import ru.maplyb.printmap.impl.util.GeoCalculator.distanceBetween
import ru.maplyb.printmap.impl.util.defTextPaint
import ru.mapolib.printmap.gui.presentation.util.PrintMapViewModel
import ru.mapolib.printmap.gui.utils.scale.roundScale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.hypot
import kotlin.math.roundToInt

internal class MapDownloadedViewModel(
    private val path: String,
    boundingBox: BoundingBox,
    appName: String,
    author: String,
    layers: List<Layer>,
    reportPath: String?,
    private val fileUtil: FileUtil,
    private val context: Context
) : PrintMapViewModel<MapDownloadedEvent, MapDownloadedEffect>() {

    private fun createLayerObjectsColor(layers: List<Layer>): Map<String, Int?> {
        return layers
            .flatMap { it.objects }
            .distinctBy { it::class }
            .map { it::class.simpleName!! }
            .associateWith { null }
    }

    private val _state = MutableStateFlow(
        MapDownloadedUiState(
            image = path,
            bitmap = BitmapFactory.decodeFile(path),
            boundingBox = boundingBox,
            layers = layers,
            appName = appName,
            author = author,
            reportFilePath = reportPath,
            layerObjectsColor = createLayerObjectsColor(layers)
        )
    )
    internal val state = _state.asStateFlow()

    init {
        activeRequestCount
            .map {
                it > 0
            }
            .onEach { progress ->
                _state.update {
                    it.copy(
                        updateMapProgress = progress
                    )
                }
            }
            .launchIn(viewModelScope)
        sendEvent(MapDownloadedEvent.Startup)
    }

    private var drawJob: Job? = null
    private val drawMutex = Mutex() // защищает от наложений

    private fun drawLayers() {
        drawJob?.cancel()
        drawJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                drawMutex.withLock {
                    doWork {
                        if (!isActive) return@doWork

                        val currentState = _state.value
                        val currentBitmap = BitmapFactory.decodeFile(path)
                            ?: return@doWork

                        val bitmap = try {
                            currentBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return@doWork
                        }

                        if (!isActive) return@doWork


                        if (currentState.showCoordinateGrid) {
                            DrawOnBitmap().drawScaleLines(
                                stepDegrees = currentState.coordinateGrid,
                                context = context,
                                bitmap = bitmap,
                                boundingBox = currentState.boundingBox,
                                color = currentState.coordinateGridColor.color,
                                width = currentState.coordinatesGridSliderInfo.value,
                                coordinateSystem = currentState.coordinateSystem,
                                stepMeters = currentState.coordinateGrid
                            )
                        }

                        if (!isActive) return@doWork

                        val bitmapWithDraw = if (!currentState.showLayers) {
                            bitmap
                        } else {
                            DrawOnBitmap().drawLayers(
                                bitmap = bitmap,
                                boundingBox = currentState.boundingBox,
                                layers = currentState.layers.filter { it.selected },
                                context = context,
                                layerObjectsColor = currentState.layerObjectsColor
                            )
                            bitmap
                        }

                        if (!isActive) return@doWork

                        val rotatedBitmap =
                            if (currentState.orientation != ImageOrientation.PORTRAIT) {
                                rotateBitmap(bitmap = bitmapWithDraw)
                            } else bitmapWithDraw

                        val bitmapWithDefaults = setDefault(rotatedBitmap)

                        _state.update {
                            it.copy(bitmap = bitmapWithDefaults)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setDefault(
        bitmap: Bitmap
    ): Bitmap {
        val scale = calculateMapScale(
            widthBitmap = bitmap.width,
            heightBitmap = bitmap.height,
            boundingBox = _state.value.boundingBox,
            dpi = _state.value.dpi
        )
        val bitmapWithScale = drawScale(bitmap, scale)
        val bitmapWithName = drawName(bitmapWithScale, _state.value.name)
        val bitmapWithWatermark =
            addWatermark(
                bitmap = bitmapWithName,
                appName = _state.value.appName,
                author = _state.value.author
            )
        return bitmapWithWatermark
    }

    fun addWatermark(
        bitmap: Bitmap,
        appName: String,
        author: String
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val textSize = (mutableBitmap.width + mutableBitmap.height) / 2 * 0.025f
        val paintStroke = defTextPaint(
            context = context,
            color = Color.WHITE,
            textSize = textSize,
            strokeWidth = textSize / 10f,
            style = Paint.Style.STROKE
        )

        val paintFill = defTextPaint(
            context = context,
            color = Color.BLACK,
            textSize = textSize,
        )

        val textHeight = paintFill.descent() - paintFill.ascent()
        val x = mutableBitmap.width * 0.02f
        canvas.drawText(appName, x, textHeight, paintStroke)
        canvas.drawText(appName, x, textHeight, paintFill)

        if (author.isNotEmpty()) {
            canvas.drawText(author, x, textHeight * 2, paintStroke)
            canvas.drawText(author, x, textHeight * 2, paintFill)
        }
        return mutableBitmap
    }

    override fun consumeEvent(action: MapDownloadedEvent) {
        when (action) {
            MapDownloadedEvent.Startup -> {
                if (_state.value.exportType is ExportTypes.PDF) {
                    _state.update {
                        it.copy(
                            exportType = (_state.value.exportType as ExportTypes.PDF).copy(
                                pagesSize = fileUtil.calculatePagesSize(
                                    _state.value.bitmap,
                                    (_state.value.exportType as ExportTypes.PDF).format
                                )
                            )
                        )
                    }
                }
                drawLayers()
            }

            MapDownloadedEvent.DeleteImage -> {
                deleteExistedMap()
            }

            MapDownloadedEvent.Share -> shareImage()
            MapDownloadedEvent.ShowPolylineChanged -> {
                _state.update {
                    it.copy(
                        showLayers = !_state.value.showLayers
                    )
                }
                drawLayers()
            }

            MapDownloadedEvent.ChangeOrientation -> {
                _state.update {
                    it.copy(
                        orientation = _state.value.orientation.getOther(),
                    )
                }
                drawLayers()
            }

            is MapDownloadedEvent.UpdateLayer -> {
                val newLayers = _state.value.layers.map { layer ->
                    if (layer.name == action.layer.name) action.layer else layer
                }
                _state.update {
                    it.copy(
                        layers = newLayers,
                        showLayers = !newLayers.all { layer -> !layer.selected }
                    )
                }
                drawLayers()
            }

            is MapDownloadedEvent.UpdateMapObjectStyle -> {
                updateMapObjectsStyle(action.layerObject)
            }

            MapDownloadedEvent.UpdateLayers -> {
                drawLayers()
            }

            is MapDownloadedEvent.UpdateExportType -> {
                val type = if (action.type is ExportTypes.PDF) {
                    action.type.copy(
                        pagesSize = fileUtil.calculatePagesSize(
                            _state.value.bitmap,
                            action.type.format
                        )
                    )
                } else action.type
                _state.update {
                    it.copy(
                        exportType = type
                    )
                }
            }

            is MapDownloadedEvent.UpdateName -> {
                _state.update {
                    it.copy(
                        name = action.name
                    )
                }
                drawLayers()
            }

            is MapDownloadedEvent.SelectDpi -> {
                _state.update {
                    it.copy(
                        dpi = action.dpi
                    )
                }
            }

            is MapDownloadedEvent.UpdateState -> {
                _state.update {
                    it.copy(
                        state = action.state
                    )
                }
            }

            is MapDownloadedEvent.UpdateColorToObjects -> {
                val updatedMap = _state.value.layerObjectsColor.toMutableMap()
                updatedMap[action.layerObject::class.simpleName!!] = action.color?.toArgb()
                _state.update {
                    it.copy(
                        state = MapDownloadedState.Initial,
                        layerObjectsColor = updatedMap
                    )
                }
                drawLayers()
            }

            is MapDownloadedEvent.UpdateCoordinateGridColor -> {
                _state.update {
                    it.copy(
                        state = MapDownloadedState.Initial,
                        coordinateGridColor = action.color
                    )
                }
                drawLayers()
            }

            is MapDownloadedEvent.ChangeCheckCoordinateGrid -> {
                _state.update {
                    it.copy(
                        showCoordinateGrid = !it.showCoordinateGrid
                    )
                }
                drawLayers()
            }

            is MapDownloadedEvent.CoordinateGridSliderValueChangeFinished -> {
                _state.update {
                    it.copy(
                        coordinatesGridSliderInfo = it.coordinatesGridSliderInfo.copy(
                            value = action.value
                        )
                    )
                }
                drawLayers()
            }

            is MapDownloadedEvent.SelectCoordinateGrid -> {
                _state.update {
                    it.copy(
                        coordinateGrid = action.value
                    )
                }
                drawLayers()
            }

            is MapDownloadedEvent.SelectCoordinateSystem -> {
                _state.update {
                    it.copy(
                        coordinateSystem = action.system,
                        coordinateGrid = coordinateGridVariants[action.system]!!.first()
                    )
                }
                drawLayers()
            }

            MapDownloadedEvent.OnReportClick -> { onReportClicked() }

            MapDownloadedEvent.OnDeleteReportClick -> { onDeleteRepostClick() }
            MapDownloadedEvent.OnShareReportClick -> { onShareReportClick() }
        }
    }

    private fun onShareReportClick() {
        state.value.reportFilePath?.let {
            sendExcelFile(context, it)
        }
    }

    private fun onDeleteRepostClick() {
        _state.update { it.copy(state = MapDownloadedState.Progress) }
        val result = state.value.reportFilePath?.let {
            deleteExcelFile(it)
        } ?: false
        if (result) {
            onEffect(MapDownloadedEffect.DeleteReportPath)
            _state.update {
                it.copy(
                    reportFilePath = null,
                    state = MapDownloadedState.Initial,
                )
            }
        }
    }

    private fun onReportClicked() {
        _state.update { it.copy(state = MapDownloadedState.Progress) }
        viewModelScope.launch(Dispatchers.Default) {
            val objects = state.value
                .layers
                .filter { it.selected }
                .map { it.objects }
                .flatten()
                .filterIsInstance<LayerObject.Object>()
            val filePath = exportToExcel(context, objects)
            filePath?.let {
                onEffect(MapDownloadedEffect.SetReportPath(filePath))
            }
            _state.update {
                it.copy(
                    reportFilePath = filePath,
                    state = MapDownloadedState.Initial,
                )
            }
        }
    }

    private fun drawName(bitmap: Bitmap, name: String): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val textSize = mutableBitmap.width * 0.025f
        val paintStroke = defTextPaint(
            context = context,
            color = Color.WHITE,
            textSize = textSize,
            strokeWidth = textSize / 10f,
            style = Paint.Style.STROKE
        )
        val paintFill = defTextPaint(
            context = context,
            color = Color.BLACK,
            textSize = textSize,
        )
        val textLength = paintFill.measureText(name)
        val x = (mutableBitmap.width / 2f) - (textLength / 2)
        val textHeight = paintFill.descent() - paintFill.ascent()
        val lines = name.split('\n')
        val lineSpacing = paintFill.fontSpacing // или можно задать вручную

        lines.forEachIndexed { index, line ->
            val y = textHeight + index * lineSpacing
            canvas.drawText(line, x, y, paintStroke)
            canvas.drawText(line, x, y, paintFill)
        }
        return mutableBitmap
    }

    /*Масштаб м/см*/
    private fun calculateMapScale(
        widthBitmap: Int,
        heightBitmap: Int,
        boundingBox: BoundingBox,
        dpi: Dpi
    ): Int {
        val widthGeoMetr = distanceBetween(
            boundingBox.latNorth,
            boundingBox.lonWest,
            boundingBox.latNorth,
            boundingBox.lonEast
        )

        val heightGeoMetr = distanceBetween(
            boundingBox.latNorth,
            boundingBox.lonWest,
            boundingBox.latSouth,
            boundingBox.lonWest
        )

        val widthCm = (widthBitmap / dpi) * 2.54
        val heightCm = (heightBitmap / dpi) * 2.54

        val scaleWidth = widthGeoMetr / widthCm
        val scaleHeight = heightGeoMetr / heightCm

        val scale = (scaleWidth + scaleHeight) / 2

        return scale.roundToInt()
    }


    private fun drawScale(
        bitmap: Bitmap,
        scale: Int,
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val (scaleInSegment, segmentLength) = roundScale(
            bitmap.width,
            scale
        )
        val padding = mutableBitmap.width * 0.025f // отступ от текста до линии

        val textSize = (mutableBitmap.width + mutableBitmap.height) / 2 * 0.015f
        val paintStroke = defTextPaint(
            context = context,
            color = Color.WHITE,
            textSize = textSize,
            strokeWidth = textSize / 10f,
            style = Paint.Style.STROKE
        )

        val paintFill = defTextPaint(
            context = context,
            color = Color.BLACK,
            textSize = textSize,
        )

        val textHeight = paintFill.descent() - paintFill.ascent()
        val scaleY = mutableBitmap.height - textHeight / 2


        /*Рисуем 0*/
        canvas.drawText("0", padding, scaleY, paintStroke)
        canvas.drawText("0", padding, scaleY, paintFill)

        canvas.drawText(scaleInSegment.toString(), padding + segmentLength, scaleY, paintStroke)
        canvas.drawText(scaleInSegment.toString(), padding + segmentLength, scaleY, paintFill)

        canvas.drawText(
            "${(scaleInSegment * 2)} м",
            padding + segmentLength * 2,
            scaleY,
            paintStroke
        )
        canvas.drawText("${(scaleInSegment * 2)} м", padding + segmentLength * 2, scaleY, paintFill)

        drawScaleLines(
            padding = padding,
            bitmap = bitmap,
            textHeight = textHeight,
            segmentLength = segmentLength,
            scaleY = scaleY,
            canvas = canvas
        )
        return mutableBitmap
    }

    private fun drawScaleLines(
        padding: Float,
        bitmap: Bitmap,
        textHeight: Float,
        segmentLength: Float,
        scaleY: Float,
        canvas: Canvas
    ) {
        val thickness =
            bitmap.width.coerceAtMost(bitmap.height) * 0.005f // 0.5% от минимального размера
        val crossLength = thickness * 2  // Длина поперечных линий
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = thickness
            isAntiAlias = true
        }
        val outlineThickness = thickness * 2 // Обводка в 2 раза толще

        val paintOutline = Paint().apply {
            color = Color.WHITE
            strokeWidth = outlineThickness
            isAntiAlias = true
        }

        val lineYStart = scaleY - textHeight - crossLength
        val lineXEnd = padding + segmentLength * 2  // Это корректное значение для второго отрезка

// Рисуем белую обводку
        canvas.drawLine(padding, lineYStart, lineXEnd, lineYStart, paintOutline)
// Рисуем основную линию
        canvas.drawLine(padding, lineYStart, lineXEnd, lineYStart, paint)

// Вычисляем направление линии (вектор)
        val dx = lineXEnd - padding
        val dy = lineYStart - lineYStart
        val length = hypot(dx.toDouble(), dy.toDouble()).toFloat()

// Нормализуем вектор (направление линии)
        val nx = dx / length
        val ny = dy / length

// Вектор перпендикуляра
        val px = -ny * crossLength
        val py = nx * crossLength

// Поперечные линии на концах
        canvas.drawLine(
            padding - px,
            lineYStart - py - (thickness / 2),
            padding + px,
            lineYStart + py + (thickness / 2),
            paintOutline
        )
        canvas.drawLine(
            padding + segmentLength,
            lineYStart - py - (thickness / 2),
            padding + segmentLength,
            lineYStart + py + (thickness / 2),
            paintOutline
        )
        val endLineStartX = lineXEnd - px
        val endLineEndX = lineXEnd + px
        canvas.drawLine(
            endLineStartX,
            lineYStart - py - (thickness / 2),
            endLineEndX,
            lineYStart + py + (thickness / 2),
            paintOutline
        )

        canvas.drawLine(
            padding - px,
            lineYStart - py,
            padding + px,
            lineYStart + py,
            paint
        )
        canvas.drawLine(
            padding + segmentLength,
            lineYStart - py,
            padding + segmentLength,
            lineYStart + py,
            paint
        )
        canvas.drawLine(
            lineXEnd - px,
            lineYStart - py,
            lineXEnd + px,
            lineYStart + py,
            paint
        )
    }


    private fun rotateBitmap(
        orientation: ImageOrientation = _state.value.orientation,
        bitmap: Bitmap = _state.value.bitmap
    ): Bitmap {
        val matrix = Matrix()
        if (orientation == ImageOrientation.LANDSCAPE) {
            matrix.postRotate(90f)
        } else {
            matrix.postRotate(-90f)
        }
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        _state.update {
            it.copy(
                orientation = orientation
            )
        }
        return rotatedBitmap
    }

    private fun updateMapObjectsStyle(layerObject: LayerObject) {
        val type = layerObject.javaClass
        _state.update {
            it.copy(
                layers = it.layers.map { layer ->
                    layer.copy(
                        objects = layer.objects.map {
                            if (it.javaClass == type) {
                                it.updateStyleWidth(
                                    layerObject.style.width
                                )
                            } else it
                        }
                    )
                }
            )
        }
    }

    private fun shareImage() {
        viewModelScope.launch(Dispatchers.Default) {
            doWork(
                onProgress = { progress ->
                    if (progress) {
                        _state.update {
                            it.copy(
                                state = MapDownloadedState.Progress
                            )
                        }
                    }
                },
                doOnAsyncBlock = {
                    val fileName =
                        if (_state.value.name.isNotEmpty()) _state.value.name else "${System.currentTimeMillis()}"
                    when (_state.value.exportType) {
                        is ExportTypes.PDF -> {
                            when (val saveResult = fileUtil.saveBitmapToPdf(
                                bitmap = _state.value.bitmap,
                                fileName = fileName,
                                pageFormat = (_state.value.exportType as ExportTypes.PDF).format,
                                dpi = _state.value.dpi
                            )
                            ) {
                                is OperationResult.Error -> {
                                    _state.update {
                                        it.copy(
                                            state = MapDownloadedState.Failure(saveResult.message)
                                        )
                                    }
                                }

                                is OperationResult.Success<String> -> {
                                    fileUtil.sendImageAsFile(saveResult.data)
                                    _state.update {
                                        it.copy(
                                            state = MapDownloadedState.Initial
                                        )
                                    }
                                }
                            }
                        }

                        is ExportTypes.PNG -> {
                            fileUtil.saveBitmapToExternalStorage(
                                bitmap = _state.value.bitmap,
                                fileName = fileName,
                                dpi = _state.value.dpi
                            )?.let {
                                fileUtil.sendImageAsFile(it)
                                _state.update {
                                    it.copy(
                                        state = MapDownloadedState.Initial
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    private fun deleteExistedMap() {
        fileUtil.clearDownloadMapStorage()
        onEffect(MapDownloadedEffect.DeleteMap(_state.value.image ?: error("Image path is null")))
    }

    companion object {
        fun create(
            path: String,
            boundingBox: BoundingBox,
            layers: List<Layer>,
            appName: String,
            author: String,
            reportPath: String?,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MapDownloadedViewModel::class.java)) {
                    val saveBitmap = FileUtil(context)
                    @Suppress("UNCHECKED_CAST")
                    return MapDownloadedViewModel(
                        path = path,
                        boundingBox = boundingBox,
                        layers = layers,
                        fileUtil = saveBitmap,
                        context = context,
                        appName = appName,
                        author = author,
                        reportPath = reportPath,
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}