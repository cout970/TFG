import * as React from 'react'
import {Group, Mesh, MeshPhongMaterial} from "three";
import {Defs} from "../Definitions";


interface LayerProps {
    group: Group,
    layer: Defs.Layer
}

interface LayerState {
    show: boolean,
    wireframe: boolean
}

export default class Layer extends React.Component<LayerProps, LayerState> {

    constructor(props) {
        super(props)

        this.state = {show: true, wireframe: false}
    }

    toggleShow = () => this.setState({...this.state, show: !this.state.show})
    toggleWireframe = () => this.setState({...this.state, wireframe: !this.state.wireframe})

    static fixName(name: string): string {
        return name.replace(" ", "-")
    }

    render() {
        this.props.group.visible = this.state.show
        this.props.group.children.forEach(i => ((i as Mesh).material as MeshPhongMaterial).wireframe = this.state.wireframe)
        return (
            <tr>
                <td>
                    <input type="checkbox" id={Layer.fixName(this.props.layer.name)} name="checkbox"
                           checked={this.state.show} onChange={this.toggleShow}/>
                    <label htmlFor={Layer.fixName(this.props.layer.name)}>{this.props.layer.name}</label>

                    <input type="checkbox" id={Layer.fixName(this.props.layer.name) + "_wire"} name="checkbox2"
                           checked={this.state.wireframe} onChange={this.toggleWireframe}/>
                    <label htmlFor={Layer.fixName(this.props.layer.name) + "_wire"}>Wireframe</label>

                </td>
                <td>{this.props.layer.description}</td>
            </tr>
        )
    }
}