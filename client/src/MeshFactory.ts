import {
    BufferAttribute,
    BufferGeometry, Color, ExtrudeGeometry, Face3, FaceColors, Geometry, Line, LineBasicMaterial, Math, Mesh,
    MeshBasicMaterial, MeshNormalMaterial,
    MeshPhongMaterial,
    MeshStandardMaterial,
    Object3D, Shape, ShapeGeometry,
    ShapeUtils,
    Vector3, VertexColors
} from "three";

import clamp = Math.clamp;

export class MeshFactory {

    static toMesh(geom: ExtGeometry): Object3D {
        let geometry = new BufferGeometry()
        let material = new MeshPhongMaterial()

        // material.wireframe = true
        material.vertexColors = FaceColors

        console.log(geom.attributes)
        geom.attributes.forEach(attr => {
            console.log(attr.attributeName)
            console.log(attr.data)
            geometry.addAttribute(attr.attributeName, new BufferAttribute(Float32Array.from(attr.data), attr.count))
        })

        geometry.computeVertexNormals()

        return new Mesh(geometry, material)
    }

    static modelToObjects(model: Model): Array<Object3D> {
        if (model.type == "LINE") {

            return model.shapes.map(shape => {
                if (shape.indices.length == 0) return

                let points = []

                shape.indices.forEach(it => {
                    let vec = model.vertex[it]
                    points.push(new Vector3(vec.x, vec.y, vec.z))
                })

                let material = new LineBasicMaterial()

                let height = model.vertex[shape.indices[0]].y
                let range = clamp(height, 0, 800) / 800

                let low = new Color(0, 1, 0)
                let high = new Color(1, 0, 0)
                material.color.set(low.lerp(high, range))

                return new Line(new BufferGeometry().setFromPoints(points), material)
            })
        } else if (model.type == "POLYGONS") {
            let shapes = []
            let height = 0

            model.shapes.forEach(s => {
                let shape = new Shape()
                let pos = s.indices[0]

                height = model.vertex[pos].y

                shape.moveTo(model.vertex[pos].x, model.vertex[pos].z)
                for (let i = 1; i < s.indices.length; i++) {
                    shape.lineTo(model.vertex[s.indices[i]].x, model.vertex[s.indices[i]].z)

                }
                shapes.push(shape)
            })

            let geometry = new ExtrudeGeometry(shapes, {
                steps: 2,
                amount: height,
                bevelEnabled: false
            });
            geometry.rotateX(Math.degToRad(-90))

            let material = new MeshBasicMaterial({color: 0x00ff00})

            return [new Mesh(new BufferGeometry().fromGeometry(geometry), material)]

        } else {
            let geometry = new Geometry()
            let material = new MeshPhongMaterial()
            material.wireframe = true
            material.vertexColors = VertexColors


            model.vertex.forEach(vec => {
                geometry.vertices.push(new Vector3(vec.x, vec.y / 100, vec.z))
            })

            let low = new Color(0, 1, 0)
            let high = new Color(0x22 / 255, 0x20 / 255, 0x1E / 255)
            let blue = new Color(0, 0.5, 1)

            model.shapes.forEach(shape => {
                let ind = shape.indices
                let face = new Face3(ind[0], ind[1], ind[2])

                let h1 = model.vertex[ind[0]].y / 2000
                let h2 = model.vertex[ind[1]].y / 2000
                let h3 = model.vertex[ind[2]].y / 2000

                face.vertexColors[0] = h1 == 0 ? blue : low.clone().lerp(high, h1)
                face.vertexColors[1] = h2 == 0 ? blue : low.clone().lerp(high, h2)
                face.vertexColors[2] = h3 == 0 ? blue : low.clone().lerp(high, h3)
                geometry.faces.push(face)
            })

            geometry.computeFaceNormals();
            geometry.computeVertexNormals();

            return [new Mesh(new BufferGeometry().fromGeometry(geometry), material)]
        }
    }
}