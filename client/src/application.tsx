import * as React from 'react'

import './index.less'
import Environment from "./Environment";

interface ApplicationState {
    showGround: boolean,
    showBuildings: boolean,
    showStreets: boolean,
}

export default class Application extends React.Component<{}, ApplicationState> {

    constructor(props) {
        super(props)

        this.state = {
            showGround: true,
            showBuildings: true,
            showStreets: true,
        }
    }

    toggleGround = () => this.setState({...this.state, showGround: !this.state.showGround})
    toggleBuildings = () => this.setState({...this.state, showBuildings: !this.state.showBuildings})
    toggleStreets = () => this.setState({...this.state, showStreets: !this.state.showStreets})

    componentDidUpdate() {
        const scene = Environment.scene

        if (this.state.showGround && scene.getObjectByName("Ground") == undefined) {
            scene.add(Environment.ground)
        } else if (!this.state.showGround && scene.getObjectByName("Ground") != undefined) {
            scene.remove(Environment.ground)
        }

        if (this.state.showBuildings && scene.getObjectByName("Buildings") == undefined) {
            scene.add(Environment.buildings)
        } else if (!this.state.showBuildings && scene.getObjectByName("Buildings") != undefined) {
            scene.remove(Environment.buildings)
        }

        if (this.state.showStreets && scene.getObjectByName("Streets") == undefined) {
            scene.add(Environment.streets)
        } else if (!this.state.showStreets && scene.getObjectByName("Streets") != undefined) {
            scene.remove(Environment.streets)
        }
    }

    render() {
        return (
            <div className="right-panel">
                <h3 className="title">Controls</h3>

                <fieldset>
                    <label><input type="checkbox" checked={this.state.showGround} onChange={this.toggleGround}/>
                        <span>Show ground</span>
                    </label>
                    <label><input type="checkbox" checked={this.state.showBuildings} onChange={this.toggleBuildings}/>
                        <span>Show buildings</span>
                    </label>
                    <label><input type="checkbox" checked={this.state.showStreets} onChange={this.toggleStreets}/>
                        <span>Show streets</span>
                    </label>
                </fieldset>
            </div>
        )
    }
}