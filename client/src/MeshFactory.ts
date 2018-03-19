import {
    BufferGeometry, Color, ExtrudeGeometry, Face3, FaceColors, Geometry, Line, LineBasicMaterial, Math, Mesh,
    MeshBasicMaterial,
    MeshPhongMaterial,
    MeshStandardMaterial,
    Object3D, Shape, ShapeGeometry,
    ShapeUtils,
    Vector3, VertexColors
} from "three";

import clamp = Math.clamp;

export class MeshFactory {

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

            let material = new MeshBasicMaterial({color: 0x00ff00 * height / (3.5 * 14)})

            return [new Mesh(new BufferGeometry().fromGeometry(geometry), material)]

        } else {
            let geometry = new Geometry()
            let material = new MeshPhongMaterial()
            // material.wireframe = true
            material.vertexColors = VertexColors


            model.vertex.forEach(vec => {
                geometry.vertices.push(new Vector3(vec.x, vec.y, vec.z))
            })

            let low = new Color(0, 1, 0)
            let high = new Color(1, 0, 0)

            model.shapes.forEach(shape => {
                let ind = shape.indices
                let face = new Face3(ind[0], ind[1], ind[2])

                let h1 = model.vertex[ind[0]].y
                let h2 = model.vertex[ind[1]].y
                let h3 = model.vertex[ind[2]].y

                face.vertexColors[0] = low.clone().lerp(high, h1 / 15)
                face.vertexColors[1] = low.clone().lerp(high, h2 / 15)
                face.vertexColors[2] = low.clone().lerp(high, h3 / 15)
                geometry.faces.push(face)
            })

            geometry.computeFaceNormals();
            geometry.computeVertexNormals();

            return [new Mesh(new BufferGeometry().fromGeometry(geometry), material)]
        }
    }
}