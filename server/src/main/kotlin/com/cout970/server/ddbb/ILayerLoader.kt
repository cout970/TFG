package com.cout970.server.ddbb

import com.cout970.server.rest.DLayer

typealias Area = String

interface ILayerLoader {

    fun load(area: Area): DLayer
}