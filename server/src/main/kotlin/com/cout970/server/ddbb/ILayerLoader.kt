package com.cout970.server.ddbb

import com.cout970.server.rest.Defs

typealias Area = String

interface ILayerLoader {

    fun load(area: Area): Defs.Layer
}