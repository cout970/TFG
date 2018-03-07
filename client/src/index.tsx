import * as React from 'react'
import * as ReactDOM from 'react-dom'

import Application from './application'
import {WorldHandler} from "./WorldHandler";
import Environment from "./Environment";

main()

function main() {

    ReactDOM.render(<Application/>, document.getElementById('root'))

    WorldHandler.init()
    Environment.onTick = WorldHandler.onTick
    Environment.init()
    Environment.gameLoop()
}