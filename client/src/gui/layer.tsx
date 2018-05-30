import * as React from 'react'
import {Mesh, MeshStandardMaterial} from "three";
import {Object3D} from "three/three-core";


interface LayerProps {
    group: Object3D
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

    recursiveSetWireframe(obj: any) {
        if (obj.hasOwnProperty("children")) {
            obj.children.forEach(i => this.recursiveSetWireframe(i))
        }

        if (obj instanceof Mesh) {
            if (obj.material instanceof MeshStandardMaterial) {
                obj.material.wireframe = this.state.wireframe
            }
        }
    }

    render() {
        this.props.group.visible = this.state.show
        this.recursiveSetWireframe(this.props.group)

        let name = this.props.group.userData.name
        let description = this.props.group.userData.description

        return (
            <tr>
                <td>
                    <input type="checkbox" id={Layer.fixName(name)} name="checkbox"
                           checked={this.state.show} onChange={this.toggleShow}/>
                    <label htmlFor={Layer.fixName(name)}>{name}</label>

                    <input type="checkbox" id={Layer.fixName(name) + "_wire"} name="checkbox2"
                           checked={this.state.wireframe} onChange={this.toggleWireframe}/>
                    <label htmlFor={Layer.fixName(name) + "_wire"}>Wireframe</label>

                </td>
                <td>{description}</td>
            </tr>
        )
    }
}