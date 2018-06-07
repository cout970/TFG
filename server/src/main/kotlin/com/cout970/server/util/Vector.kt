package com.cout970.server.util

import com.cout970.server.glTF.Vector2
import com.cout970.server.glTF.Vector3
import eu.printingin3d.javascad.coords.Coords3d

operator fun Vector3.plus(other: Vector3) = Vector3(this.x + other.x, this.y + other.y, this.z + other.z)
operator fun Vector3.minus(other: Vector3) = Vector3(this.x - other.x, this.y - other.y, this.z - other.z)
operator fun Vector3.times(other: Vector3) = Vector3(this.x * other.x, this.y * other.y, this.z * other.z)

operator fun Vector3.plus(other: Float) = Vector3(this.x + other, this.y + other, this.z + other)
operator fun Vector3.minus(other: Float) = Vector3(this.x - other, this.y - other, this.z - other)
operator fun Vector3.times(other: Float) = Vector3(this.x * other, this.y * other, this.z * other)

operator fun Vector2.plus(other: Vector2) = Vector2(this.x + other.x, this.y + other.y)
operator fun Vector2.minus(other: Vector2) = Vector2(this.x - other.x, this.y - other.y)
operator fun Vector2.times(other: Vector2) = Vector2(this.x * other.x, this.y * other.y)

operator fun Vector2.plus(other: Float) = Vector2(this.x + other, this.y + other)
operator fun Vector2.minus(other: Float) = Vector2(this.x - other, this.y - other)
operator fun Vector2.times(other: Float) = Vector2(this.x * other, this.y * other)

fun Vector3.toCoords(): Coords3d = Coords3d(x.toDouble(), y.toDouble(), z.toDouble())
fun Coords3d.toVector(): Vector3 = Vector3(x.toFloat(), y.toFloat(), z.toFloat())