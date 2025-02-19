package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits

// TODO: Remove this

/**
 * A helper object for coordinate related math
 */
object LocationMath {


    private fun convertUnitPerSecondsToUnitPerHours(unitPerSecond: Float): Float {
        return unitPerSecond * 60 * 60
    }

    fun convertToBaseUnit(meters: Float, units: UserPreferences.DistanceUnits): Float {
        return if (units == UserPreferences.DistanceUnits.Feet) {
            Distance(meters, DistanceUnits.Meters).convertTo(DistanceUnits.Feet).distance
        } else {
            meters
        }
    }

    fun convertToBaseSpeed(metersPerSecond: Float, units: UserPreferences.DistanceUnits): Float {
        return if (units == UserPreferences.DistanceUnits.Feet) {
            convertUnitPerSecondsToUnitPerHours(
                Distance(
                    metersPerSecond,
                    DistanceUnits.Meters
                ).convertTo(DistanceUnits.Miles).distance
            )
        } else {
            convertUnitPerSecondsToUnitPerHours(
                Distance(
                    metersPerSecond,
                    DistanceUnits.Meters
                ).convertTo(DistanceUnits.Kilometers).distance
            )
        }
    }

    fun convertToMeters(distance: Float, units: UserPreferences.DistanceUnits): Float {
        return if (units == UserPreferences.DistanceUnits.Meters) {
            distance
        } else {
            Distance(distance, DistanceUnits.Feet).meters().distance
        }
    }
}