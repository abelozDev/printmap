package ru.maplyb.printmap.impl.domain.use_case

import okhttp3.internal.cacheGet
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.domain.model.TileParams
import ru.maplyb.printmap.impl.util.GeoCalculator

class GetTilesCountAndFileSizeUseCase {
    suspend operator fun invoke(boundingBox: BoundingBox, zoom: Int): OperationResult<Pair<List<TileParams>, Long>> {
        val count = GeoCalculator().calculateTotalTilesCount(
            boundingBox, zoom
        )
        return when(count) {
            is OperationResult.Error -> count
            is OperationResult.Success -> {
                val tilesSize = calculateApproximateFileSize(count.data.count())
                if (tilesSize > 584_640_000) {
                    OperationResult.Error(
                        message = "Слишком большой размер карты. Попробуйте уменьшить размер или зум"
                    )
                } else {
                    OperationResult.Success(count.data to tilesSize)
                }
            }
        }
    }
    private fun calculateApproximateFileSize(tilesCount: Int): Long {
        return tilesCount.toLong() * 255 * 255 * 4
    }
}