import * as React from 'react'
import {Group} from "three";
import {Defs} from "../Definitions";


interface LayerProps {
    group: Group,
    layer: Defs.Layer
}

interface LayerState {
    show: boolean,
}

export default class Layer extends React.Component<LayerProps, LayerState> {

    constructor(props) {
        super(props)

        this.state = {show: true}
    }

    toggleShow = () => this.setState({...this.state, show: !this.state.show})

    static fixName(name: string): string {
        return name.replace(" ", "-")
    }

    render() {
        this.props.group.visible = this.state.show
        return (
            <tr>
                <td>
                    <input type="checkbox" id={Layer.fixName(this.props.layer.name)} name="checkbox"
                           checked={this.state.show} onChange={this.toggleShow}/>

                    <label htmlFor={Layer.fixName(this.props.layer.name)}>{this.props.layer.name}</label>
                </td>
                <td>{this.props.layer.description}</td>
            </tr>
        )
    }
}