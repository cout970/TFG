import * as React from 'react'

import '../style/index.less'
import '../style/font-awesome.min.css'
import '../style/main.css'

import Layer from "./layer";
import Environment from "../Environment";

interface ApplicationState {
    showGround: boolean,
}

export default class Application extends React.Component<{}, ApplicationState> {

    constructor(props) {
        super(props)

        Environment.guiCallback = () => this.setState(i => i)
        this.state = {showGround: true}
    }

    toggleGround = () => this.setState({...this.state, showGround: !this.state.showGround})

    render() {
        Environment.ground.visible = this.state.showGround
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
                        <tr>
                            <td>
                                <input type="checkbox" id="terrain-checkbox" name="checkbox"
                                       checked={this.state.showGround} onChange={this.toggleGround}/>
                                <label htmlFor="terrain-checkbox">Terreno</label>
                            </td>
                            <td>Representacion de la elevacion del terreno</td>
                        </tr>
                        {Environment.layers.children.map(layer =>
                            (<Layer group={layer} layer={layer.userData}/>)
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        )
    }
}