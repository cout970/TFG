import * as React from 'react'

import '../style/index.less'
import '../style/font-awesome.min.css'
import '../style/fonts/fontawesome-webfont.woff2'
import '../style/main.css'

import Layer from "./layer";
import Environment from "../Environment";
import {Scene} from "three";

interface ApplicationState {
    scene: Scene,
    hidden: boolean
}

const hidePanel = document.getElementById("hidePanel")

export default class Application extends React.Component<{}, ApplicationState> {

    constructor(props) {
        super(props)

        this.state = {scene: Environment.externalScene, hidden: false}
        Environment.guiCallback = () => {
            this.setState(i => ({...i, scene: Environment.externalScene}))
        }
        hidePanel.onclick = () => {
            this.setState(i => ({...i, hidden: !this.state.hidden}))
        }
    }

    render() {
        if (this.state.hidden) {
            hidePanel.style.left = '0';
            return (<div id="panel-disable"/>);
        }

        hidePanel.style.left = '300px';

        return (
            <div id="panel-enable">
                <div className="table-wrapper">
                    <table className="alt">
                        <thead>
                        <tr>
                            <th><h4>Layers</h4></th>
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