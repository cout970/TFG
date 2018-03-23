import {MeshFactory} from "./MeshFactory";
import Environment from "./Environment";
import {Mesh, MeshBasicMaterial, Shape, ShapeGeometry} from "three";

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


        // for (let x = -5; x < 0; x++) {
        //     for (let z = -5; z < 0; z++) {
        //
        //         this.get(`/api/buildings/${x}/${z}`)
        //         .then(i => MeshFactory.modelToObjects(i))
        //         .then(i => i.forEach(it => {
        //             Environment.buildings.add(it)
        //         }))
        //         .catch(i => console.log(i))
        //
        //     }
        // }

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


        let x = 0, y = 0;
        let heartShape = new Shape();

        heartShape.moveTo(x + 5, y + 5);
        heartShape.bezierCurveTo(x + 5, y + 5, x + 4, y, x, y);
        heartShape.bezierCurveTo(x - 6, y, x - 6, y + 7, x - 6, y + 7);
        heartShape.bezierCurveTo(x - 6, y + 11, x - 3, y + 15.4, x + 5, y + 19);
        heartShape.bezierCurveTo(x + 12, y + 15.4, x + 16, y + 11, x + 16, y + 7);
        heartShape.bezierCurveTo(x + 16, y + 7, x + 16, y, x + 10, y);
        heartShape.bezierCurveTo(x + 7, y, x + 5, y + 5, x + 5, y + 5);


        let geometry = new ShapeGeometry(heartShape);
        let material = new MeshBasicMaterial({color: 0x00ff00});
        Environment.axis.add(new Mesh(geometry, material))
        //
        for (let x = 0; x < 18; x++) {
            for (let z = 0; z < 18; z++) {
                this.get(`/api/height/${x}/${z}/level/0`)
                .then(i => MeshFactory.modelToObjects(i))
                .then(i => i.forEach(it => {
                    Environment.ground.add(it)
                }))
                .catch(i => console.log(i))
            }
        }


        // for (let x = 0; x < 18; x++) {
        //     for (let z = 0; z < 18; z++) {
        //         this.get(`/api/height/${x}/${z}/level/1`)
        //         .then(i => MeshFactory.modelToObjects(i))
        //         .then(i => i.forEach(it => {
        //             Environment.ground.add(it)
        //         }))
        //         .catch(i => console.log(i))
        //     }
        // }
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