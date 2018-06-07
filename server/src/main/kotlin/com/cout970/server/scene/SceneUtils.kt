package com.cout970.server.scene

import com.cout970.server.glTF.Vector3
import java.awt.Color

fun DColor.toVector() = Vector3(r, g, b)

fun DPolygon.flip(): DPolygon {
    return DPolygon(points.reversed())
}

fun colorOf(rgb: String): DColor {
    val c = if (rgb.startsWith("#")) rgb.substring(1) else rgb
    val str = if (c.length == 3) "${c[0]}${c[0]}${c[1]}${c[1]}${c[2]}${c[2]}" else c

    val color = Color(str.toInt(16))
    return DColor(color.red / 255f, color.green / 255f, color.blue / 255f)
}