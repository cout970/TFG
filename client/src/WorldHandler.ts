import {MeshFactory} from "./MeshFactory";
import Environment from "./Environment";

export class WorldHandler {

    static init() {
        for (let i = 1; i < 94; i++) {

            let mun = i < 10
                ? `00${i}`
                : i < 100
                    ? `0${i}`
                    : `${i}`

            window.fetch(`/api/multiline/${mun}`)
                .then(i => {
                    if (!i.ok) throw new Error(i.statusText)
                    return i.text()
                })
                .then(i => JSON.parse(i))
                .then(i => MeshFactory.modelToObjects(i))
                .then(i => i.forEach(it => {
                    Environment.scene.add(it)
                }))
                .catch(i => console.log(i))
        }
    }

    static onTick() {

    }
}