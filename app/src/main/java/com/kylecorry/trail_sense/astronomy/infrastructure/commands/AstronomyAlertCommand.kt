package com.kylecorry.trail_sense.astronomy.infrastructure.commands

import android.content.Context
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.commands.LocationCommand
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration

class AstronomyAlertCommand(private val context: Context) : CoroutineCommand {
    override suspend fun execute() {
        val gps = SensorService(context).getGPS(true)

        try {
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(Duration.ofSeconds(10).toMillis()) {
                    if (!gps.hasValidReading) {
                        gps.read()
                    }
                }
            }
        } finally {
            gps.stop(null)
        }

        val location = gps.location

        if (location == Coordinate.zero) {
            return
        }

        val commands: List<LocationCommand> = listOf(
            LunarEclipseAlertCommand(context),
            MeteorShowerAlertCommand(context)
        )

        commands.forEach { it.execute(location) }
    }
}