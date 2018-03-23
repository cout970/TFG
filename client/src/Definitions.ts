interface Vector3f {
    x: number
    y: number
    z: number
}

interface Shape {
    indices: Array<number>
}

interface Model {
    vertex: Array<Vector3f>
    shapes: Array<Shape>
    type: string
}

