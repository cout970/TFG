interface Vector3f {
    x: number
    y: number
    z: number
}

interface Multiline {
    lines: Array<Array<Vector3f>>
}

interface Shape {
    indices: Array<number>
}

interface Model {
    vertex: Array<number>
    shapes: Array<Shape>
    type: string
}