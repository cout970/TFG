import {Color} from "three";

// new format
export namespace Defs {

    export interface Vector3 {
        x: number
        y: number
        z: number
    }

    // export interface Color {
    //     r: number
    //     g: number
    //     b: number
    // }

    export interface Rotation {
        angle: number,
        axis: Vector3
    }

    export interface BufferAttribute {
        attributeName: string
        data: ArrayLike<number>
        count: number
    }

    export interface Geometry {
        attributes: Array<BufferAttribute>
    }

    export interface Material {
        ambientIntensity: number,
        shininess: number,
        diffuseColor: Color,
        emissiveColor: Color,
        specularColor: Color,
        transparency: number
    }

    export enum CameraType {
        PERSPECTIVE, ORTHOGRAPHIC
    }

    export interface ViewPoint {
        location: Vector3
        orientation: Rotation
        camera: CameraType
    }

    export interface Model {
        geometry: Geometry,
        material: Material
    }

    export interface Pair<A, B> {
        first: A,
        second: B
    }

    export interface Shape {
        models: Array<Pair<Material, Array<string>>>
    }

    export interface Rule {
        filter: string,
        minDistance: number,
        maxDistance: number,
        shapes: Array<Shape>
    }

    export interface Layer {
        name: string
        description: String
        rules: Array<Rule>
    }

    export interface Scene {
        title: string
        abstract: string
        viewPoints: Array<ViewPoint>
        layers: Array<Layer>
    }
}