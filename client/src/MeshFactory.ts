import {
    BufferGeometry, Face3, Geometry, Line, LineBasicMaterial, Math, Mesh, MeshPhongMaterial, Object3D,
    Vector3
} from "three";
import clamp = Math.clamp;

export class MeshFactory {

    modelToObjects(model: Model): Array<Object3D> {
        if (model.type == ShapeType.LINE) {

            return model.shapes.map(shape => {
                if (shape.indices.length == 0) return

                let points = []
                shape.indices.forEach(it => {
                    let x = model.vertex[it * 3]
                    let y = model.vertex[it * 3 + 1]
                    let z = model.vertex[it * 3 + 2]
                    points.push(new Vector3(x, y, z))
                })

                let height = model.vertex[shape.indices[0] * 3 + 1]

                let range = (clamp(height, 0.25, 8.0) - 0.25) / 7.75

                let material = new LineBasicMaterial()
                material.color.setRGB(range, range, range)

                return new Line(new BufferGeometry().setFromPoints(points), material)
            })
        } else {
            let geometry = new Geometry()
            let material = new MeshPhongMaterial()
            material.wireframe = true

            const end = model.vertex.length / 3

            for (let ind; ind < end; ind++) {
                let i = model.vertex[ind * 3]
                let h = model.vertex[ind * 3 + 1]
                let j = model.vertex[ind * 3 + 2]
                geometry.vertices.push(new Vector3(i, h, j))
            }

            model.shapes.forEach(it => {
                let ind = it.indices
                geometry.faces.push(new Face3(ind[0], ind[1], ind[2]))
            })

            return [new Mesh(geometry, material)]
        }
    }
}