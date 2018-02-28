import {AmbientLight, Color, OrbitControls, PerspectiveCamera, Scene, WebGLRenderer} from "three";

const StatsCtr = require("stats.js")
const OrbitControlsCtr = require("ndb-three-orbit-controls")(require("three"))

export default class Environment {

    static renderer: WebGLRenderer
    static scene: Scene
    static camera: PerspectiveCamera
    static controls: OrbitControls
    static stats: Stats

    static terrain: any //  MutableMap<Pair<Int, Int>, Object3D>

    static onTick: () => any;

    static init() {
        const body = document.body!!
        const canvas: HTMLCanvasElement = document.getElementById("webGL") as HTMLCanvasElement

        canvas.height = window.innerHeight
        canvas.width = Math.floor(window.innerWidth * 0.8)

        Environment.scene = new Scene()
        Environment.scene.add(new AmbientLight())

        // WebGL renderer
        Environment.renderer = new WebGLRenderer({
            canvas,
            antialias: true
        })

        Environment.renderer.setClearColor(new Color("#00BFFF"), 1)

        // Camera and render dimensions
        Environment.renderer.setSize(canvas.width, canvas.height)
        Environment.camera = new PerspectiveCamera(75, canvas.width / canvas.height, 0.1, 5000)

        // Move the camera away from the center so the object is not inside you
        Environment.camera.position.setZ(5)
        Environment.camera.position.setY(25)

        // FPS counter
        Environment.stats = new StatsCtr()
        body.appendChild(Environment.stats.dom)

        // Camera controls
        Environment.controls = new OrbitControlsCtr(Environment.camera, Environment.renderer.domElement)
        Environment.controls.target.set(16, 0, 16)
        Environment.controls.update()

        // On window change size
        window.addEventListener("resize", it => {
            canvas.height = window.innerHeight
            canvas.width = Math.floor(window.innerWidth * 0.8)
            Environment.camera.aspect = canvas.width / canvas.height
            Environment.camera.updateProjectionMatrix()
            Environment.renderer.setSize(canvas.width, canvas.height)
        }, false)
    }

    static gameLoop() {
        window.requestAnimationFrame(it => {
            Environment.onTick()
            Environment.gameLoop()
        })
        Environment.renderer.render(Environment.scene, Environment.camera)
        Environment.stats.update()
    }
}