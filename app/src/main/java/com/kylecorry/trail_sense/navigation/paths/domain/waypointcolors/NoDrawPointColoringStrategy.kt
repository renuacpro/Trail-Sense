package com.kylecorry.trail_sense.navigation.paths.domain.waypointcolors

import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint

class NoDrawPointColoringStrategy : IPointColoringStrategy {
    override fun getColor(point: PathPoint): Int? {
        return null
    }
}