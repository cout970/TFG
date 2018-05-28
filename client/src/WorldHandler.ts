import {MeshFactory} from "./MeshFactory";
import Environment from "./Environment";
import {AxesHelper, Group} from "three";
import {Defs} from "./Definitions";

export class WorldHandler {

    static init() {

        window['debug'] = this

        this.get("/api/scene/0").then(i => this.loadScene(i)).catch(console.log)
        // this.loadHeightMap()
        this.createOriginModel()
    }

    static loadArray(str: string): Promise<Float32Array> {
        return window.fetch(`/api/binary/${str}`)
        .then(i => i.arrayBuffer())
        .then(j => new Float32Array(j))
    }

    private static loadAllResources(scene: Defs.Scene): Promise<Map<string, Float32Array>> {
        let promises = []

        scene.layers.forEach(i => {
            i.rules.forEach(j => {
                j.shapes.forEach(k => {
                    k.models.forEach(l => {
                        l.second.forEach(str => {
                            promises.push(this.loadArray(str).then(array => [str, array]))
                        })
                    })
                })
            })
        })

        return Promise.all(promises).then(array => {
            let m = new Map<string, Float32Array>()
            array.forEach(i => m.set(i[0], i[1]))
            return m
        })
    }

    private static loadScene(scene: Defs.Scene) {
        const env = Environment
        console.log("Loading scene...")
        console.log(scene)

        this.loadAllResources(scene).then(map => {
            console.log("binary data loaded")
            console.log(map)


            let groups = scene.layers.map(layer => {
                let group = new Group()
                group.userData = layer
                group.name = layer.name

                layer.rules.forEach(rule => {
                    rule.shapes.forEach(i => {
                        let models = MeshFactory.toMeshModels(map, i.models)
                        console.log(models)
                        models.forEach(i => group.add(i))
                    })
                })

                layer.labels.forEach(label => {
                    group.add(MeshFactory.bakeLabel(label))
                })

                return group
            })
            groups.forEach(group => env.layers.add(group))

            let viewpoint = scene.viewPoints[0]
            let pos = viewpoint.location
            env.camera.position.set(pos.x, pos.y, pos.z)
            env.controls.target.set(0, 0, -10) // TODO
            env.controls.update()

            env.guiCallback()
            console.log("Scene loaded")
        })
    }

    private static createOriginModel() {
        Environment.axis.add(new AxesHelper(100))
    }

    private static loadHeightMap() {

        // this.get(`/api/height/-8/1`)
        // this.get(`/api/height/0/0`)
        // .then(i => MeshFactory.fromTerrain(i))
        // .then(i => Environment.ground.add(i))
        // .catch(i => console.log(i))
        //
        // for (let i = 0; i < 2024; i++) { // 399
        //     let pos = this.spiral(i)
        //     // this.get(`/api/height/${pos[0] - 8}/${pos[1] + 1}`)
        //     this.get(`/api/height/${pos[0]}/${pos[1]}`)
        //     .then(i => MeshFactory.fromTerrain(i))
        //     .then(i => Environment.ground.add(i))
        //     .catch(i => console.log(i))
        // }

        for (let i = -20; i < 20; i++) { // 399
            for (let j = -20; j < 20; j++) { // 399
                this.get(`/api/height/${i}/${j}`)
                .then(i => MeshFactory.fromTerrain(i))
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

    //https://stackoverflow.com/questions/398299/looping-in-a-spiral
    static spiral(n: number): Array<number> {
        // given n an index in the squared spiral
        // p the sum of point in inner square
        // a the position on the current square
        // n = p + a

        let r = Math.floor((Math.sqrt(n + 1) - 1) / 2) + 1;

        // compute radius : inverse arithmetic sum of 8+16+24+...=
        let p = (8 * r * (r - 1)) / 2;
        // compute total point on radius -1 : arithmetic sum of 8+16+24+...

        let en = r * 2;
        // points by face

        let a = (1 + n - p) % (r * 8);
        // compute de position and shift it so the first is (-r,-r) but (-r+1,-r)
        // so square can connect

        let pos = [0, 0, r];
        switch (Math.floor(a / (r * 2))) {
            // find the face : 0 top, 1 right, 2, bottom, 3 left
            case 0: {
                pos[0] = a - r;
                pos[1] = -r;
            }
                break;
            case 1: {
                pos[0] = r;
                pos[1] = (a % en) - r;

            }
                break;
            case 2: {
                pos[0] = r - (a % en);
                pos[1] = r;
            }
                break;
            case 3: {
                pos[0] = -r;
                pos[1] = r - (a % en);
            }
                break;
        }
        return pos;
    }
}

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


// this.get(`/api/camera`)
// .then(pair => {
//     const pos = pair.first
//     const target = pair.second
//
//     Environment.camera.position.set(pos.x, pos.y, pos.z)
//     Environment.controls.target.set(target.x, target.y, target.z)
//     Environment.controls.update()
// })


// for (let x = -1; x < 0; x++) {
//     for (let z = -1; z < 0; z++) {
//
//         this.get(`/api/buildings2/${x}/${z}`)
//         .then(i => {
//             console.log(i);
//             return MeshFactory.toMeshGeometry(i)
//         })
//         .then(i => Environment.buildings.add(i))
//         .catch(i => console.log(i))
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
