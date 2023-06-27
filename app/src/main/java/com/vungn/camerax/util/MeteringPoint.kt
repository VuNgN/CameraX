package com.vungn.camerax.util

data class MeteringPoint(val x: Float, val y: Float, val size: Float)

fun MeteringPoint.offsetX(): Int {
    return (this.x - this.size * 7).toInt()
}

fun MeteringPoint.offsetY(): Int {
    return (this.y - this.size).toInt()
}