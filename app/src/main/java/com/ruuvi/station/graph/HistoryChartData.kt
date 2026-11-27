package com.ruuvi.station.graph

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.GapEntry
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.ruuvi.station.history.HistoryController
import com.ruuvi.station.history.HistorySample
import com.ruuvi.station.history.HistoryStatistics

/** A bounded graph model, separate from local sensor retention and raw export pages. */
data class HistoryChartData(
    val state: HistoryController.State,
    val metric: String,
    val groups: List<List<Entry>>,
    val onInteraction: (Boolean) -> Unit,
    val onViewport: (Double, Double) -> Unit,
) {
    fun statistics(): HistoryStatistics? = state.statistics(metric)
    fun statisticsDisplay() = state.statisticsDisplay(metric)
    fun bounds(): Pair<Double, Double>? {
        val values = listOfNotNull(state.overview?.statistics?.get(metric), state.detail?.statistics?.get(metric))
        // Retained archive context can outlive raw cache pages. Keep its visible extrema
        // inside the Y axis even when the statistics are explicitly partial.
        val drawn = state.overview?.samples?.get(metric).orEmpty() + state.detail?.samples?.get(metric).orEmpty()
        val minimum = (values.map { it.minimum } + drawn.map { it.value }).minOrNull() ?: return null
        val maximum = (values.map { it.maximum } + drawn.map { it.value }).maxOrNull() ?: return null
        return minimum to maximum
    }
}

/** Keep the shipped fork's exact drawing, fills, alert intersections and gap stems/dots.
 * Only replace its distance-based gap classification with raw-reading segment identity.
 */
class HistoryLineChartRenderer(chart: LineChart) : LineChartRenderer(chart, chart.animator, chart.viewPortHandler) {
    override fun buildLineDataSetWithIntersections(data: ILineDataSet, top: Float?, bottom: Float?, maximumGap: Float): ILineDataSet {
        val original = (0 until data.entryCount).map { data.getEntryForIndex(it) }
        if (original.isEmpty() || original.any { it.data !is HistorySample })
            return super.buildLineDataSetWithIntersections(data, top, bottom, maximumGap)
        val built = super.buildLineDataSetWithIntersections(data, top, bottom, Float.MAX_VALUE)
        fun rawGap(index: Int): Boolean = index > 0 && index < original.size &&
            (original[index - 1].data as HistorySample).segment != (original[index].data as HistorySample).segment
        for (index in 0 until built.entryCount) {
            val entry = built.getEntryForIndex(index) as? GapEntry ?: continue
            var low = 0; var high = original.size
            while (low < high) { val middle = (low + high) / 2; if (original[middle].x < entry.x) low = middle + 1 else high = middle }
            val actual = low < original.size && original[low].x == entry.x
            entry.isGap = index == 0 || rawGap(low)
            entry.isSinglePoint = actual && (low == 0 || rawGap(low)) && (low == original.lastIndex || rawGap(low + 1))
        }
        return built
    }
}
