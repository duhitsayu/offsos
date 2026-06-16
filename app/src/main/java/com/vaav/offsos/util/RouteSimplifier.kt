package com.vaav.offsos.util

import android.location.Location
import com.vaav.offsos.data.RoutePoint

object RouteSimplifier {
    
    // Simplifies points using Ramer-Douglas-Peucker algorithm
    fun simplify(points: List<RoutePoint>, epsilon: Double): List<RoutePoint> {
        if (points.size < 3) return points

        var maxDistance = 0.0
        var index = 0
        val end = points.size - 1

        for (i in 1 until end) {
            // Do not simplify across gaps
            if (points[i].isGap) {
                // If there's a gap, split and process both sides
                val left = simplify(points.subList(0, i + 1), epsilon)
                val right = simplify(points.subList(i, end + 1), epsilon)
                val result = mutableListOf<RoutePoint>()
                result.addAll(left.subList(0, left.size - 1))
                result.addAll(right)
                return result
            }

            val distance = perpendicularDistance(points[i], points[0], points[end])
            if (distance > maxDistance) {
                index = i
                maxDistance = distance
            }
        }

        return if (maxDistance > epsilon) {
            val left = simplify(points.subList(0, index + 1), epsilon)
            val right = simplify(points.subList(index, end + 1), epsilon)
            
            val result = mutableListOf<RoutePoint>()
            result.addAll(left.subList(0, left.size - 1))
            result.addAll(right)
            result
        } else {
            listOf(points[0], points[end])
        }
    }

    private fun perpendicularDistance(pt: RoutePoint, lineStart: RoutePoint, lineEnd: RoutePoint): Double {
        val startLoc = Location("").apply {
            latitude = lineStart.latitude
            longitude = lineStart.longitude
        }
        val endLoc = Location("").apply {
            latitude = lineEnd.latitude
            longitude = lineEnd.longitude
        }
        val ptLoc = Location("").apply {
            latitude = pt.latitude
            longitude = pt.longitude
        }

        val totalDist = startLoc.distanceTo(endLoc)
        if (totalDist == 0f) return startLoc.distanceTo(ptLoc).toDouble()
        
        val a = ptLoc.distanceTo(startLoc).toDouble()
        val b = ptLoc.distanceTo(endLoc).toDouble()
        val c = totalDist.toDouble()
        
        // Heron's formula for area
        val s = (a + b + c) / 2.0
        val areaSq = s * (s - a) * (s - b) * (s - c)
        if (areaSq <= 0.0) return 0.0
        
        val area = Math.sqrt(areaSq)
        
        // Area = 0.5 * base * height -> height = 2 * Area / base
        return 2.0 * area / c
    }
}
