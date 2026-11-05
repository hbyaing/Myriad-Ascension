import {cameraFor,clamp} from "./math.js";
import {actor,box,createRenderer,effectMesh,stage} from "./render.js";
import {drawArt,performance as choreography} from "./arts.js";

const duration = 8;
const get = id => document.getElementById(id);
const canvas = get("canvas"), gallery = get("gallery");
const play = get("play"), slider = get("time"), clock = get("clock"), light = get("light");
const views = [...gallery.querySelectorAll(".view")].map(node=>({
  node, phase:node.querySelector(".phase"), art:Number(node.dataset.art), passive:node.dataset.passive==="1",
}));
const defaults = {yaw:.43,pitch:.32,half:1.41};
const orbit = {...defaults};
let renderer, playing = true, time = 0, day = false, drag = null, last = null, dirty = true;

function playback(next) {
  playing = next;
  play.textContent = playing ? "暂停" : "播放";
  play.setAttribute("aria-pressed",String(playing));
  last = null;
}
function seek(t) {
  time = clamp(t,0,duration);
  slider.value = time.toFixed(2);
  clock.value = time.toFixed(2)+" / 8 秒";
  dirty = true;
}
function showError(error) {
  get("error").hidden = false;
  get("error").textContent = "预览渲染失败 · "+error.message;
  document.documentElement.dataset.preview = "error";
  console.error(error);
}

play.addEventListener("click",()=>playback(!playing));
get("restart").addEventListener("click",()=>{seek(0);playback(true);});
slider.addEventListener("input",()=>{playback(false);seek(Number(slider.value));});
light.addEventListener("click",()=>{
  day = !day;
  light.textContent = day?"暗景":"亮景";
  light.setAttribute("aria-pressed",String(day));
  dirty = true;
});
get("reset").addEventListener("click",()=>{Object.assign(orbit,defaults);dirty=true;});
function focus(pick) {
  get("focus").value=pick;
  const single=pick!=="all";
  gallery.classList.toggle("focus",single);
  get("view-caption").textContent=single?"单术 / 主动与被动":"五术 / 十组演出";
  get("layout-note").textContent=single?"左侧为主动施术，右侧为被动触发。":"上排为主动施术，下排为被动触发。";
  for(const [i,article] of [...gallery.querySelectorAll("article")].entries()){
    article.hidden=single && Number(pick)!==i;
  }
  dirty=true;
}
get("focus").addEventListener("change",e=>focus(e.target.value));
document.addEventListener("keydown",e=>{
  if(e.code==="Space" && (e.target===document.body || e.target===document.documentElement)) {
    e.preventDefault();
    playback(!playing);
  }
});
for(const {node} of views) {
  node.addEventListener("pointerdown",e=>{
    if(drag || e.button!==0) return;
    drag={id:e.pointerId,x:e.clientX,y:e.clientY};
    node.setPointerCapture(e.pointerId);
  });
  node.addEventListener("pointermove",e=>{
    if(drag?.id!==e.pointerId) return;
    orbit.yaw+=(e.clientX-drag.x)*.012;
    orbit.pitch=clamp(orbit.pitch+(e.clientY-drag.y)*.012,-.19,1.12);
    drag={id:e.pointerId,x:e.clientX,y:e.clientY};
    dirty=true;
  });
  node.addEventListener("lostpointercapture",e=>{if(drag?.id===e.pointerId) drag=null;});
  node.addEventListener("wheel",e=>{
    e.preventDefault();
    orbit.half=clamp(orbit.half*Math.exp(e.deltaY*.0009),.87,3.2);
    dirty=true;
  },{passive:false});
}
canvas.addEventListener("webglcontextlost",e=>{
  e.preventDefault();
  renderer=null;
  showError(Error("WebGL 上下文中断，正在尝试恢复。"));
});
canvas.addEventListener("webglcontextrestored",()=>start());
const stageMesh = stage();
const boxes = new ResizeObserver(()=>{dirty=true;});
boxes.observe(gallery);

const params = new URLSearchParams(location.search);
const selectedTime = params.get("t");
if(selectedTime!==null && selectedTime.trim()!=="" && Number.isFinite(Number(selectedTime))) {
  playback(false);
  seek(Number(selectedTime));
}
if(["0","1","2","3","4"].includes(params.get("art"))) focus(params.get("art"));

function draw() {
  const root = gallery.getBoundingClientRect();
  const scale = clamp(window.devicePixelRatio||1,1.28,1.5);
  const width = Math.round(root.width*scale), height = Math.round(root.height*scale);
  if(!width || !height) return;
  if(canvas.width!==width || canvas.height!==height) {
    canvas.width=width;
    canvas.height=height;
  }
  renderer.clear();
  for(const view of views) {
    const {node,phase,art,passive} = view;
    const r = node.getBoundingClientRect();
    if(!r.width || !r.height) continue;
    const w = Math.round(r.width*scale), h = Math.round(r.height*scale);
    const x = Math.round((r.left-root.left)*scale), y = height-Math.round((r.bottom-root.top)*scale);
    const state = choreography(art,passive,time);
    const tailRoom = art===0 ? Math.min(.86,state.speed*.74)*(passive?.67:1) : 0;
    const strikeRoom = art===3 ? state.hit*(passive?.17:.50) : 0;
    const camera = cameraFor(orbit.yaw,orbit.pitch,orbit.half,w/h,
      [state.root[0]*.90-tailRoom+strikeRoom,1.03+state.root[1]*.76,0]);
    const scene = stageMesh.slice();
    box(scene,[state.root[0],.004,state.root[2]],[.51,.005,.34],[.132,.20,.19]);
    const armed=art===3 && !passive && time>=.04 && time<7.55;
    const pose = actor(scene,state.root,state.stride,state.yaw,state.hit,state.guard,armed);
    const fx = effectMesh(camera);
    const description = drawArt(fx,art,passive,state,time,pose);
    if(phase.textContent!==description) phase.textContent=description;
    renderer.draw([x,y,w,h],camera,scene,fx.finish(),time,Number(day));
  }
  renderer.finish();
  dirty=false;
}

function frame(now) {
  if(!renderer) return;
  try {
    if(playing) {
      const delta = last===null?0:Math.min(.08,(now-last)/1000);
      seek((time+delta*Number(get("speed").value))%duration);
    }
    last=now;
    if(dirty) draw();
    requestAnimationFrame(frame);
  } catch(error) { showError(error); }
}

async function source(file) {
  const response = await fetch(file);
  if(!response.ok) throw Error(file+" "+response.status);
  return response.text();
}
async function start() {
  try {
    const [vertex,fragment] = await Promise.all([source("effect.vert"),source("effect.frag")]);
    renderer=createRenderer(canvas,vertex,fragment);
    dirty=true;
    draw();
    const error=renderer.error();
    if(error!==0) throw Error("WebGL "+error);
    document.documentElement.dataset.preview="ready";
    get("error").hidden=true;
    requestAnimationFrame(frame);
  } catch(error) { showError(error); }
}
start();
