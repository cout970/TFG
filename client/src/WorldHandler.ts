import Environment from "./Environment";
import {AxesHelper, Group, Object3D, Scene, Vector3} from "three";

const GLTFLoader = require("three-gltf-loader")

export class WorldHandler {

    static init() {

        window['debug'] = this

        this.get("/api/scenes").then(i => {
            console.log("Available scenes: " + JSON.stringify(i))
            this.loadScene2(i[0])
        })

        this.createOriginModel()
    }

    static loadScene2(id: string) {
        let loader = new GLTFLoader();

        loader.setPath('/api/files/')

        loader.load(
            `api/scene/${id}`,
            function (gltf) {
                WorldHandler.processScene(gltf.scene)
                console.log(`[Scene('${id}')] done`);
                Environment.guiCallback()
            },
            function (xhr) {
                if (xhr.total == 0) {
                    console.log(`[Scene('${id}')] loading...`);
                } else {
                    console.log(`[Scene('${id}')] ` + (xhr.loaded / xhr.total * 100) + '% loaded');
                }
            },
            function (error) {
                console.log(`[Scene('${id}')] An error happened:`);
                console.log(error);
            }
        );
    }

    private static processScene(scene: Scene) {
        scene.children.forEach(layer => {

            layer.children.forEach(rule => {

                let props: any[] = rule.userData.properties
                if (props == undefined) return

                props.forEach(p => {
                    if (p.type != undefined) {

                        let newChilds = rule.children.map(mesh => {
                            if (mesh.userData.position != undefined) {

                                let group = new Group()
                                let newMesh = mesh.clone(true)

                                group.add(newMesh)

                                group.position.x = mesh.userData.position[0]
                                group.position.z = mesh.userData.position[2]
                                return group
                            } else {
                                return mesh.clone(true)
                            }
                        })

                        while (rule.children.length > 0) {
                            rule.remove(rule.children[0])
                        }
                        newChilds.forEach(c => rule.add(c))
                    }
                })
            })
        })
        Environment.externalScene = scene
        Environment.scene.add(scene)
    }

    private static createOriginModel() {
        Environment.axis.add(new AxesHelper(100))
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
        WorldHandler.updateLOD()
    }

    static forEachChild(obj: Object3D, func: (Object3D) => void) {

        obj.children.forEach(c => {
            this.forEachChild(c, func)
        })

        func(obj)
    }

    static updateLOD() {
        let layers = Environment.externalScene.children
        if (layers.length == 0) return


        layers.forEach(layer => {
            layer.children.forEach(rule => {
                let props: any[] = rule.userData.properties
                if (props == undefined) return

                props.forEach(p => {
                    if (p.type == "follow_camera") {
                        let pos = Environment.camera.position

                        pos = new Vector3(pos.x, 0, pos.z)

                        rule.children.forEach(c => {
                            c.lookAt(pos)
                        })
                    } else if (p.type == "level_of_detail") {
                        let minDistance = p.minDistance
                        let maxDistance = p.maxDistance

                    }
                })
            })
        })
    }
}