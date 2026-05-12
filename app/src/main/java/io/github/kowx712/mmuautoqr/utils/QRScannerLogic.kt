package io.github.kowx712.mmuautoqr.utils

import kotlin.math.roundToInt

object QRScannerLogic {
    const val INVALID_SCAN_COOLDOWN_MILLIS = 2_000L
    const val AUTO_ZOOM_COOLDOWN_MILLIS = 1_000L
    const val MIN_BARCODE_COVERAGE_RATIO = 0.1f
    const val AUTO_ZOOM_STEP_MULTIPLIER = 1.4f

    data class IntRect(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val width: Int get() = (right - left).coerceAtLeast(0)
        val height: Int get() = (bottom - top).coerceAtLeast(0)
        val area: Int get() = width * height
    }

    data class InvalidScanState(
        val rawValue: String,
        val timestampMillis: Long
    )

    fun computeCenterCropRect(
        imageWidth: Int,
        imageHeight: Int,
        frameWidthRatio: Float,
        frameHeightRatio: Float
    ): IntRect {
        val cropWidth =
            (imageWidth * frameWidthRatio.coerceIn(0f, 1f)).roundToInt().coerceIn(1, imageWidth)
        val cropHeight =
            (imageHeight * frameHeightRatio.coerceIn(0f, 1f)).roundToInt()
                .coerceIn(1, imageHeight)
        val left = ((imageWidth - cropWidth) / 2f).roundToInt()
        val top = ((imageHeight - cropHeight) / 2f).roundToInt()

        return IntRect(
            left = left,
            top = top,
            right = left + cropWidth,
            bottom = top + cropHeight
        )
    }

    fun computeAutoZoomRatio(
        currentZoomRatio: Float,
        maxZoomRatio: Float,
        barcodeBounds: IntRect?,
        analysisRect: IntRect,
        minCoverageRatio: Float = MIN_BARCODE_COVERAGE_RATIO,
        zoomStepMultiplier: Float = AUTO_ZOOM_STEP_MULTIPLIER,
        lastAutoZoomAtMillis: Long,
        nowMillis: Long,
        cooldownMillis: Long = AUTO_ZOOM_COOLDOWN_MILLIS
    ): Float? {
        val bounds = barcodeBounds ?: return null
        if (nowMillis - lastAutoZoomAtMillis < cooldownMillis) return null
        if (analysisRect.area <= 0 || bounds.area <= 0) return null

        val coverageRatio = bounds.area.toFloat() / analysisRect.area.toFloat()
        if (coverageRatio >= minCoverageRatio) return null

        val nextZoom = (currentZoomRatio * zoomStepMultiplier).coerceAtMost(maxZoomRatio)
        return nextZoom.takeIf { it > currentZoomRatio }
    }

    fun shouldThrottleInvalidScan(
        rawValue: String,
        previousState: InvalidScanState?,
        nowMillis: Long,
        cooldownMillis: Long = INVALID_SCAN_COOLDOWN_MILLIS
    ): Boolean {
        if (previousState == null) return false
        return previousState.rawValue == rawValue &&
            nowMillis - previousState.timestampMillis < cooldownMillis
    }
}