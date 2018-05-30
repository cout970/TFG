import {
    BufferAttribute,
    BufferGeometry,
    Color,
    Mesh,
    MeshPhongMaterial,
    Object3D,
    TextGeometry,
    VertexColors
} from "three";
import {Defs} from "./Definitions";
import {WorldHandler} from "./WorldHandler";
import Environment from "./Environment";
import Material = Defs.Material;
import Model = Defs.Model;
import Pair = Defs.Pair;

function colorOf(c: Color): string {
    return '#' + new Color(c.r, c.g, c.b).getHexString()
}

export class MeshFactory {

    static bakeLabel(label: Defs.Label): Object3D {
        let geometry = new TextGeometry(label.txt, {
            font: Environment.font,
            size: label.scale,
            height: 1,
            curveSegments: 4,
            bevelEnabled: false,
            bevelThickness: 10,
            bevelSize: 8
        });

        let bufferGeometry = new BufferGeometry()
        bufferGeometry.fromGeometry(geometry)
        bufferGeometry.attributes

        let material = new MeshPhongMaterial({
            color: "#FFF",
        })

        let mesh = new Mesh(geometry, material)

        mesh.position.x = label.position.x
        mesh.position.y = label.position.y
        mesh.position.z = label.position.z

        return mesh
    }

    static toMeshModel(model: Defs.Model): Object3D {
        let geom = model.geometry
        let mat = model.material
        let geometry = new BufferGeometry()
        let material

        if (mat == undefined) {
            material = new MeshPhongMaterial()
            material.vertexColors = VertexColors
        } else {
            material = new MeshPhongMaterial({
                color: colorOf(mat.diffuseColor),
                emissive: colorOf(mat.emissiveColor),
                specular: colorOf(mat.specularColor),
                shininess: mat.shininess,
                transparent: mat.transparency > 0,
                opacity: 1 - mat.transparency
            })
        }

        // material.wireframe = true
        // material.vertexColors = VertexColors

        geom.attributes.forEach(attr => {
            geometry.addAttribute(attr.attributeName, new BufferAttribute(Float32Array.from(attr.data), attr.count))
        })

        geometry.computeVertexNormals()

        return new Mesh(geometry, material)
    }

    static fromTerrain(pair: Pair<string, string>): Promise<Object3D> {

        let p = WorldHandler.loadArray(pair.first)
        let c = WorldHandler.loadArray(pair.second)

        return Promise.all([p, c]).then(i => this.toMeshModel(this.toModel2(i[0], i[1])))
    }

    static toMeshModels(map: Map<string, Float32Array>, models: Array<Defs.Pair<Defs.Material, Array<string>>>): Object3D[] {
        let result = []

        models.forEach(i => {
            let material = i.first
            let arrays = i.second.map(j => map.get(j))

            arrays
            .map(a => this.toModel(material, a))
            .map(a => this.toMeshModel(a))
            .forEach(a => result.push(a))
        })

        return result
    }

    private static toModel(mat: Material, data: Float32Array): Model {
        return {
            material: mat,
            geometry: {
                attributes: [
                    {
                        attributeName: 'position',
                        count: 3,
                        data: data
                    }
                ]
            }
        }
    }

    private static toModel2(pos: Float32Array, color: Float32Array): Model {
        return {
            material: undefined,
            geometry: {
                attributes: [
                    {
                        attributeName: 'position',
                        count: 3,
                        data: pos
                    },
                    {
                        attributeName: 'color',
                        count: 3,
                        data: color
                    }
                ]
            }
        }

        // static modelToObjects(model: Model): Array<Object3D> {
        //     if (model.type == "LINE") {
        //
        //         return model.shapes.map(shape => {
        //             if (shape.indices.length == 0) return
        //
        //             let points = []
        //
        //             shape.indices.forEach(it => {
        //                 let vec = model.vertex[it]
        //                 points.push(new Vector3(vec.x, vec.y, vec.z))
        //             })
        //
        //             let material = new LineBasicMaterial()
        //
        //             let height = model.vertex[shape.indices[0]].y
        //             let range = clamp(height, 0, 800) / 800
        //
        //             let low = new Color(0, 1, 0)
        //             let high = new Color(1, 0, 0)
        //             material.color.set(low.lerp(high, range))
        //
        //             return new Line(new BufferGeometry().setFromPoints(points), material)
        //         })
        //     } else if (model.type == "POLYGONS") {
        //         let shapes = []
        //         let height = 0
        //
        //         model.shapes.forEach(s => {
        //             let shape = new Shape()
        //             let pos = s.indices[0]
        //
        //             height = model.vertex[pos].y
        //
        //             shape.moveTo(model.vertex[pos].x, model.vertex[pos].z)
        //             for (let i = 1; i < s.indices.length; i++) {
        //                 shape.lineTo(model.vertex[s.indices[i]].x, model.vertex[s.indices[i]].z)
        //
        //             }
        //             shapes.push(shape)
        //         })
        //
        //         let geometry = new ExtrudeGeometry(shapes, {
        //             steps: 2,
        //             amount: height,
        //             bevelEnabled: false
        //         });
        //         geometry.rotateX(Math.degToRad(-90))
        //
        //         let material = new MeshBasicMaterial({color: 0x00ff00})
        //
        //         return [new Mesh(new BufferGeometry().fromGeometry(geometry), material)]
        //
        //     } else {
        //         let geometry = new Geometry()
        //         let material = new MeshPhongMaterial()
        //         material.wireframe = true
        //         material.vertexColors = VertexColors
        //
        //
        //         model.vertex.forEach(vec => {
        //             geometry.vertices.push(new Vector3(vec.x, vec.y / 100, vec.z))
        //         })
        //
        //         let low = new Color(0, 1, 0)
        //         let high = new Color(0x22 / 255, 0x20 / 255, 0x1E / 255)
        //         let blue = new Color(0, 0.5, 1)
        //
        //         model.shapes.forEach(shape => {
        //             let ind = shape.indices
        //             let face = new Face3(ind[0], ind[1], ind[2])
        //
        //             let h1 = model.vertex[ind[0]].y / 2000
        //             let h2 = model.vertex[ind[1]].y / 2000
        //             let h3 = model.vertex[ind[2]].y / 2000
        //
        //             face.vertexColors[0] = h1 == 0 ? blue : low.clone().lerp(high, h1)
        //             face.vertexColors[1] = h2 == 0 ? blue : low.clone().lerp(high, h2)
        //             face.vertexColors[2] = h3 == 0 ? blue : low.clone().lerp(high, h3)
        //             geometry.faces.push(face)
        //         })
        //
        //         geometry.computeFaceNormals();
        //         geometry.computeVertexNormals();
        //
        //         return [new Mesh(new BufferGeometry().fromGeometry(geometry), material)]
        //     }
        // }
    }
}