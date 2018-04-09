package com.cout970.server.util

import com.cout970.server.rest.Vector3

operator fun Vector3.plus(other: Vector3) = Vector3(this.x + other.x, this.y + other.y, this.z + other.z)
operator fun Vector3.minus(other: Vector3) = Vector3(this.x - other.x, this.y - other.y, this.z - other.z)
operator fun Vector3.times(other: Vector3) = Vector3(this.x * other.x, this.y * other.y, this.z * other.z)

operator fun Vector3.plus(other: Float) = Vector3(this.x + other, this.y + other, this.z + other)
operator fun Vector3.minus(other: Float) = Vector3(this.x - other, this.y - other, this.z - other)
operator fun Vector3.times(other: Float) = Vector3(this.x * other, this.y * other, this.z * other)
