package com.kylecorry.trail_sense.weather.ui

import com.github.mikephil.charting.charts.LineChart
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.SimpleLineChart
import java.time.Duration


class TemperatureChart(chart: LineChart) {

    private val simpleChart = SimpleLineChart(chart, chart.context.getString(R.string.no_data))

    private var granularity = 1f

    private val color = Resources.getAndroidColorAttr(chart.context, R.attr.colorPrimary)

    init {
        simpleChart.configureYAxis(
            granularity = granularity,
            labelCount = 5,
            drawGridLines = true
        )

        simpleChart.configureXAxis(
            labelCount = 0,
            drawGridLines = false
        )
    }

    fun plot(data: List<Reading<Float>>) {
        val first = data.firstOrNull()?.time
        val values = data.map { Duration.between(first, it.time).seconds / (60f * 60f) to it.value }
        simpleChart.plot(values, color)
    }
}