import * as React from 'react'

import '../style/index.less'
import '../style/font-awesome.min.css'
import '../style/main.css'

import Layer from "./layer";
import Environment from "../Environment";
import {Scene} from "three";

interface ApplicationState {
    scene: Scene
}

export default class Application extends React.Component<{}, ApplicationState> {

    constructor(props) {
        super(props)

        this.state = {scene: Environment.externalScene}
        Environment.guiCallback = () => {
            this.setState(i => ({scene: Environment.externalScene}))
        }
    }

    render() {
        return (
            <div className="right-panel">
                <h4>Layers</h4>
                <div className="table-wrapper">
                    <table className="alt">
                        <thead>
                        <tr>
                            <th>Nombre</th>
                            <th>Descripción</th>
                        </tr>
                        </thead>
                        <tbody>
                        {this.state.scene.children.map(layer =>
                            (<Layer group={layer}/>)
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        )
    }
}