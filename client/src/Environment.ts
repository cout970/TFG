import {
    AmbientLight, Color, CullFaceNone, FrontFaceDirectionCCW, FrontFaceDirectionCW, Group, OrbitControls,
    PerspectiveCamera, Scene, Vector3,
    WebGLRenderer
} from "three";
import * as THREE from "three";

const StatsCtr = require("stats.js")
const OrbitControlsCtr = require("ndb-three-orbit-controls")(require("three"))

export default class Environment {

    static renderer: WebGLRenderer
    static scene: Scene
    static camera: PerspectiveCamera
    static controls: OrbitControls
    static stats: Stats

    static ground: Group
    static buildings: Group
    static streets: Group
    static axis: Group

    static onTick: () => any

    static init() {
        const body = document.body!!
        const canvas: HTMLCanvasElement = document.getElementById("webGL") as HTMLCanvasElement

        canvas.height = window.innerHeight
        canvas.width = Math.floor(window.innerWidth * 0.8)

        this.scene = new Scene()
        this.scene.add(new AmbientLight())

        // WebGL renderer
        this.renderer = new WebGLRenderer({
            canvas,
            antialias: true
        })

        this.renderer.setClearColor(new Color("#00BFFF"), 1)

        // Camera and render dimensions
        this.renderer.setSize(canvas.width, canvas.height)
        this.camera = new PerspectiveCamera(75, canvas.width / canvas.height, 0.1, 50000)

        // Move the camera away from the center so the object is not inside you
        this.camera.position.setZ(5)
        this.camera.position.setY(25)

        // FPS counter
        this.stats = new StatsCtr()
        body.appendChild(this.stats.dom)

        // Camera controls
        this.controls = new OrbitControlsCtr(this.camera, this.renderer.domElement)
        this.controls.target.set(16, 0, 16)
        this.controls.update()
        this.controls.keyPanSpeed = 25
        this.controls.keys.UP = 32
        this.controls.keys.BOTTOM = 16

        window.onkeydown = event => {

            if (event.which == 38 || event.which == 40) {

                let dir = new Vector3()
                    .copy(this.controls.target)
                    .sub(this.camera.position)
                    .normalize()
                    .multiplyScalar(50)

                if (event.which == 38) {
                    // UP ARROW
                    this.controls.target.add(dir)
                    this.camera.position.add(dir)
                    this.controls.update()

                } else {
                    // DOWN ARROW
                    this.controls.target.sub(dir)
                    this.camera.position.sub(dir)
                    this.controls.update()
                }
            } else if (event.which == 65 || event.which == 68 || event.which == 87 || event.which == 83) {

                let dir = new Vector3(1, 0, 0)
                    .applyAxisAngle(new Vector3(0, 1, 0), this.controls.getAzimuthalAngle())
                    .multiplyScalar(50)

                let perpendicular = new Vector3()
                    .copy(dir)
                    .applyAxisAngle(new Vector3(0, 1, 0), THREE.Math.degToRad(90))

                if (event.which == 65) {
                    // A
                    this.controls.target.sub(dir)
                    this.camera.position.sub(dir)
                    this.controls.update()

                } else if (event.which == 68) {
                    // D
                    this.controls.target.add(dir)
                    this.camera.position.add(dir)
                    this.controls.update()

                } else if (event.which == 87) {
                    // W
                    this.controls.target.add(perpendicular)
                    this.camera.position.add(perpendicular)
                    this.controls.update()

                } else {
                    // S
                    this.controls.target.sub(perpendicular)
                    this.camera.position.sub(perpendicular)
                    this.controls.update()
                }
            }
        }

        // On window change size
        window.addEventListener("resize", () => {
            canvas.height = window.innerHeight
            canvas.width = Math.floor(window.innerWidth * 0.8)
            this.camera.aspect = canvas.width / canvas.height
            this.camera.updateProjectionMatrix()
            this.renderer.setSize(canvas.width, canvas.height)
        }, false)

        this.setupGroups()
    }

    static setupGroups() {
        this.ground = new Group()
        this.ground.name = "Ground"

        this.buildings = new Group()
        this.buildings.name = "Buildings"

        this.streets = new Group()
        this.streets.name = "Streets"

        this.axis = new Group()
        this.axis.name = "axis"

        this.scene.add(this.ground)
        this.scene.add(this.buildings)
        this.scene.add(this.streets)
        this.scene.add(this.axis)
    }

    static gameLoop() {
        window.requestAnimationFrame(it => {
            this.onTick()
            this.gameLoop()
        })
        this.renderer.render(this.scene, this.camera)
        this.stats.update()
    }
}