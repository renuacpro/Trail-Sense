package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.widget.addTextChangedListener
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatServiceV2
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.andromeda.core.units.Coordinate
import com.kylecorry.andromeda.location.IGPS
import com.kylecorry.trailsensecore.infrastructure.system.UiUtils
import com.kylecorry.andromeda.core.time.Timer
import java.time.Duration

class CoordinateInputView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val formatService by lazy { FormatServiceV2(getContext()) }
    private val sensorService by lazy { SensorService(getContext()) }
    lateinit var gps: IGPS

    private val errorHandler = Timer {
        locationEdit.error = getContext().getString(R.string.coordinate_input_invalid_location)
    }

    var coordinate: Coordinate?
        get() = _coordinate
        set(value) {
            _coordinate = value
            if (value == null) {
                locationEdit.setText("")
            } else {
                val formatted = formatService.formatLocation(value)
                locationEdit.setText(formatted)
            }
        }
    private var _coordinate: Coordinate? = null

    private var changeListener: ((coordinate: Coordinate?) -> Unit)? = null
    private var autofillListener: (() -> Unit)? = null

    private lateinit var locationEdit: EditText
    private lateinit var gpsBtn: ImageButton
    private lateinit var beaconBtn: ImageButton
    private lateinit var helpBtn: ImageButton
    private lateinit var gpsLoadingIndicator: ProgressBar

    init {
        context?.let {
            inflate(it, R.layout.view_coordinate_input, this)
            gps = sensorService.getGPS()
            locationEdit = findViewById(R.id.utm)
            gpsLoadingIndicator = findViewById(R.id.gps_loading)
            helpBtn = findViewById(R.id.coordinate_input_help_btn)
            gpsBtn = findViewById(R.id.gps_btn)
            beaconBtn = findViewById(R.id.beacon_btn)

            gpsBtn.visibility = View.VISIBLE
            gpsLoadingIndicator.visibility = View.GONE

            locationEdit.addTextChangedListener {
                onChange()
            }

            helpBtn.setOnClickListener {
                UiUtils.alert(
                    getContext(),
                    getContext().getString(R.string.location_input_help_title),
                    getContext().getString(R.string.location_input_help),
                    getContext().getString(R.string.dialog_ok)
                )
            }

            beaconBtn.setOnClickListener {
                CustomUiUtils.pickBeacon(context, null, gps.location){
                    if (it != null){
                        coordinate = it.coordinate
                    }
                }
            }

            gpsBtn.setOnClickListener {
                autofillListener?.invoke()
                gpsBtn.visibility = View.GONE
                gpsLoadingIndicator.visibility = View.VISIBLE
                beaconBtn.isEnabled = false
                locationEdit.isEnabled = false
                gps.start(this::onGPSUpdate)
            }
        }
    }

    private fun onGPSUpdate(): Boolean {
        coordinate = gps.location
        gpsBtn.visibility = View.VISIBLE
        gpsLoadingIndicator.visibility = View.GONE
        beaconBtn.isEnabled = true
        locationEdit.isEnabled = true
        return false
    }

    private fun onChange() {
        val locationText = locationEdit.text.toString()
        _coordinate = Coordinate.parse(locationText)
        errorHandler.stop()
        if (_coordinate == null && locationText.isNotEmpty()) {
            errorHandler.once(Duration.ofSeconds(2))
        } else {
            locationEdit.error = null
        }
        changeListener?.invoke(_coordinate)
    }

    fun pause() {
        gps.stop(this::onGPSUpdate)
        gpsBtn.visibility = View.VISIBLE
        gpsLoadingIndicator.visibility = View.GONE
        locationEdit.isEnabled = true
        errorHandler.stop()
    }

    fun setOnAutoLocationClickListener(listener: (() -> Unit)?) {
        autofillListener = listener
    }

    fun setOnCoordinateChangeListener(listener: ((coordinate: Coordinate?) -> Unit)?) {
        changeListener = listener
    }

}