package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class WeightInputView(context: Context, attributeSet: AttributeSet? = null) : BaseUnitInputView<Weight, WeightUnits>(context, attributeSet) {

    private val formatService by lazy { FormatService(context) }

    init {
        hint = context.getString(R.string.weight)
    }

    override fun createDisplayUnit(units: WeightUnits): UnitInputView.DisplayUnit<WeightUnits> {
        return UnitInputView.DisplayUnit(
            units,
            formatService.getWeightUnitName(units, true),
            formatService.getWeightUnitName(units)
        )
    }

    override fun map(amount: Number, unit: WeightUnits): Weight {
        return Weight(amount.toFloat(), unit)
    }

    override fun getAmount(value: Weight): Number {
        return value.weight
    }

    override fun getUnit(value: Weight): WeightUnits {
        return value.units
    }

}