package com.kylecorry.trail_sense.shared.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.core.sensors.IThermometer
import com.kylecorry.andromeda.location.GPS
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.andromeda.permissions.PermissionService
import com.kylecorry.andromeda.sense.SensorChecker
import com.kylecorry.andromeda.sense.accelerometer.GravitySensor
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.barometer.Barometer
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.andromeda.sense.compass.GravityCompensatedCompass
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.compass.LegacyCompass
import com.kylecorry.andromeda.sense.hygrometer.Hygrometer
import com.kylecorry.andromeda.sense.hygrometer.IHygrometer
import com.kylecorry.andromeda.sense.inclinometer.IInclinometer
import com.kylecorry.andromeda.sense.inclinometer.Inclinometer
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.LowPassMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.orientation.*
import com.kylecorry.andromeda.sense.temperature.AmbientThermometer
import com.kylecorry.andromeda.signal.CellSignalSensor
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.trail_sense.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.hygrometer.NullHygrometer
import com.kylecorry.trail_sense.shared.sensors.odometer.Odometer
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideAltimeter
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.sensors.speedometer.BacktrackSpeedometer
import com.kylecorry.trailsensecore.infrastructure.sensors.altimeter.FusedAltimeter

class SensorService(ctx: Context) {

    private var context = ctx.applicationContext
    private val userPrefs by lazy { UserPreferences(context) }
    private val sensorChecker by lazy { SensorChecker(context) }
    private val sensorManager by lazy { context.getSystemService<SensorManager>() }

    fun getGPS(background: Boolean = false): IGPS {

        val hasForegroundPermission = hasLocationPermission(false)

        if (!userPrefs.useAutoLocation || !hasForegroundPermission) {
            return OverrideGPS(context)
        }

        if (hasLocationPermission(background) && GPS.isAvailable(context)) {
            return CustomGPS(context)
        }

        return CachedGPS(context)
    }

    private fun hasLocationPermission(background: Boolean): Boolean {
        val permissions = PermissionService(context)
        return if (background) {
            permissions.isBackgroundLocationEnabled()
        } else {
            permissions.canGetFineLocation()
        }
    }

    fun getSpeedometer(realTime: Boolean? = null): ISpeedometer {
        val useRealTime = realTime
            ?: (userPrefs.navigation.speedometerMode == NavigationPreferences.SpeedometerMode.Instantaneous)
        return if (useRealTime) {
            getGPS(false)
        } else {
            BacktrackSpeedometer(context)
        }
    }

    fun getGPSAltimeter(background: Boolean = false): IAltimeter {
        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else {
            if (!GPS.isAvailable(context)) {
                return CachedAltimeter(context)
            }

            return getGPS(background)
        }
    }

    fun getAltimeter(background: Boolean = false): IAltimeter {

        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else if (mode == UserPreferences.AltimeterMode.Barometer && sensorChecker.hasBarometer()) {
            // TODO: Verify this still works correctly
            return Barometer(context, seaLevelPressure = userPrefs.seaLevelPressureOverride)
        } else {
            if (!GPS.isAvailable(context)) {
                return CachedAltimeter(context)
            }

            val gps = getGPS(background)

            return if (mode == UserPreferences.AltimeterMode.GPSBarometer && sensorChecker.hasBarometer()) {
                FusedAltimeter(gps, Barometer(context))
            } else {
                gps
            }
        }
    }

    fun getOdometer(): Odometer {
        return Odometer(context)
    }

    fun getCompass(): ICompass {
        val smoothing = userPrefs.navigation.compassSmoothing
        val useTrueNorth = userPrefs.navigation.useTrueNorth
        return if (userPrefs.navigation.useLegacyCompass) LegacyCompass(
            context,
            smoothing,
            useTrueNorth
        ) else GravityCompensatedCompass(
            context, smoothing, useTrueNorth
        )
    }

    fun getDeviceOrientationSensor(): DeviceOrientation {
        return DeviceOrientation(context)
    }

    fun getBarometer(): IBarometer {
        return if (userPrefs.weather.hasBarometer) Barometer(context) else NullBarometer()
    }

    fun getInclinometer(): IInclinometer {
        return Inclinometer(context)
    }

    @Suppress("DEPRECATION")
    fun getThermometer(): IThermometer {
        if (sensorChecker.hasSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)) {
            return AmbientThermometer(context, Sensor.TYPE_AMBIENT_TEMPERATURE)
        }

        if (sensorChecker.hasSensor(Sensor.TYPE_TEMPERATURE)) {
            return AmbientThermometer(context, Sensor.TYPE_TEMPERATURE)
        }

        val builtInSensors = sensorManager?.getSensorList(Sensor.TYPE_ALL) ?: listOf()

        val first = builtInSensors.filter {
            it.name.contains("temperature", true) ||
                    it.name.contains("thermometer", true)
        }.minByOrNull { it.resolution }

        if (first != null) {
            return AmbientThermometer(context, first.type)
        }

        return Battery(context)
    }

    fun getHygrometer(): IHygrometer {
        if (sensorChecker.hasHygrometer()) {
            return Hygrometer(context)
        }

        return NullHygrometer()
    }

    fun getCellSignal(background: Boolean = false): ICellSignalSensor {
        if (!hasLocationPermission(background)) {
            return NullCellSignalSensor()
        }
        return CellSignalSensor(context, userPrefs.cellSignal.populateCache)
    }

    fun getGravity(): IAccelerometer {
        return if (sensorChecker.hasSensor(Sensor.TYPE_GRAVITY)) {
            GravitySensor(context)
        } else {
            LowPassAccelerometer(context)
        }
    }

    fun getMagnetometer(): IMagnetometer {
        return Magnetometer(context)
    }

    fun getGyroscope(): IGyroscope {
        if (!sensorChecker.hasGyroscope()){
            return NullGyroscope()
        }
        return Gyroscope(context)
    }

    fun getOrientationSensor(
        useGyro: Boolean = true,
        useMag: Boolean = true,
        useAcc: Boolean = true
    ): IOrientationSensor {
        return MadgwickAHRS(context,
            accelerometer = if (useAcc) LowPassAccelerometer(context) else NullAccelerometer(),
            gyro = if (useGyro && sensorChecker.hasGyroscope()) Gyroscope(context) else NullGyroscope(),
            magnetometer = if (useMag) LowPassMagnetometer(context) else NullMagnetometer()
        )
    }

}