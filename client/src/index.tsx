import * as React from 'react'
import * as ReactDOM from 'react-dom'

import Application from './gui/application'
import {WorldHandler} from "./WorldHandler";
import Environment from "./Environment";

main()

function main() {
    Environment.onTick = WorldHandler.onTick
    Environment.init()
    WorldHandler.init()

    ReactDOM.render(<Application/>, document.getElementById('root'))

    Environment.gameLoop()
}