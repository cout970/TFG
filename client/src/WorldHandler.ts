import {MeshFactory} from "./MeshFactory";
import Environment from "./Environment";

export class WorldHandler {

    static init() {

        window['debug'] = Environment

        // for (let i = 1; i < 10; i++) { //94
        let i = 78
        let mun = i < 10
            ? `00${i}`
            : i < 100
                ? `0${i}`
                : `${i}`

        this.get(`/api/multiline/${mun}`)
            .then(i => MeshFactory.modelToObjects(i))
            .then(i => i.forEach(it => {
                Environment.ground.add(it)
            }))
            .catch(i => console.log(i))
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