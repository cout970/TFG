import {MeshFactory} from "./MeshFactory";
import Environment from "./Environment";
import {Geometry, Line, LineBasicMaterial, Mesh, MeshBasicMaterial, Shape, ShapeGeometry, Vector3} from "three";

export class WorldHandler {

    static init() {

        window['debug'] = Environment

        // for (let i = 1; i < 10; i++) { //94
        // let i = 78
        // let mun = i < 10
        //     ? `00${i}`
        //     : i < 100
        //         ? `0${i}`
        //         : `${i}`
        //
        // this.get(`/api/multiline/${mun}`)
        // .then(i => MeshFactory.modelToObjects(i))
        // .then(i => i.forEach(it => {
        //     Environment.ground.add(it)
        // }))
        // .catch(i => console.log(i))
        // }


        this.get(`/api/camera`)
        .then(pair => {
            const pos = pair.first
            const target = pair.second

            Environment.camera.position.set(pos.x, pos.y, pos.z)
            Environment.controls.target.set(target.x, target.y, target.z)
            Environment.controls.update()
        })


        for (let x = -5; x < 0; x++) {
            for (let z = -5; z < 0; z++) {

                this.get(`/api/buildings/${x}/${z}`)
                .then(i => MeshFactory.modelToObjects(i))
                .then(i => i.forEach(it => {
                    Environment.buildings.add(it)
                }))
                .catch(i => console.log(i))

            }
        }

        // for (let x = -5; x < 0; x++) {
        //     for (let z = -5; z < 0; z++) {
        //
        //         this.get(`/api/streets/${x}/${z}`)
        //         .then(i => MeshFactory.modelToObjects(i))
        //         .then(i => i.forEach(it => {
        //             Environment.streets.add(it)
        //         }))
        //         .catch(i => console.log(i))
        //     }
        //
        // }

        let geometry = new Geometry();
        let geometry1 = new Geometry();
        let geometry2 = new Geometry();
        let material = new LineBasicMaterial({color: 0xff0000})
        let material1 = new LineBasicMaterial({color: 0x00ff00})
        let material2 = new LineBasicMaterial({color: 0x0000ff})

        geometry.vertices.push(new Vector3(-100, 0, 0))
        geometry.vertices.push(new Vector3(100, 0, 0))

        geometry1.vertices.push(new Vector3(0, -100, 0))
        geometry1.vertices.push(new Vector3(0, 100, 0))

        geometry2.vertices.push(new Vector3(0, 0, -100))
        geometry2.vertices.push(new Vector3(0, 0, 100))

        Environment.axis.add(new Line(geometry, material))
        Environment.axis.add(new Line(geometry1, material1))
        Environment.axis.add(new Line(geometry2, material2))

        this.loadHeightMap()
    }

    private static loadHeightMap() {
        for (let x = 0; x < 18; x++) {
            for (let z = 0; z < 18; z++) {
                this.get(`/api/height/${x}/${z}`)
                .then(i => MeshFactory.toMesh(i))
                .then(i => Environment.ground.add(i))
                .catch(i => console.log(i))
            }
        }
    }

    private static get(str: string): Promise<any> {

        return window.fetch(str)
        .then(i => {
            if (!i.ok) throw new Error(i.statusText)
            return i.json()
        })
        .catch(i => console.log(i))
    }

    static onTick() {

    }
}