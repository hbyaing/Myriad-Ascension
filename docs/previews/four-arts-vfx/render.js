import {add, cross, dot, mul, norm, sub} from "./math.js";

const solidVertex = `#version 300 es
in vec3 position;
in vec3 normal;
in vec3 tint;
in float character;
uniform mat4 camera;
out vec3 surface;
out vec3 color;
out vec3 world;
out float actorSurface;
void main(){
  gl_Position=camera*vec4(position,1.);
  surface=normal;
  color=tint;
  world=position;
  actorSurface=character;
}`;
const solidFragment = `#version 300 es
precision highp float;
in vec3 surface;
in vec3 color;
in vec3 world;
in float actorSurface;
uniform float daylight;
uniform vec3 fog;
uniform vec4 recovery;
uniform vec3 recoveryTint;
uniform vec4 groundLight;
uniform float time;
layout(location=0) out vec4 frag;
layout(location=1) out vec4 glow;
void main(){
  float key=max(0.,dot(surface,normalize(vec3(-.6,.9,.48))));
  float rim=max(0.,dot(surface,normalize(vec3(.5,.26,-1.))));
  vec3 shaded=color*(.45+.57*key+.24*rim+daylight*.27);
  if(actorSurface>.5 && recovery.w>0.) {
    float y=(world.y-recovery.z)/2.075;
    float mode=recovery.x;
    float offset=(mode>1.5?abs(y-.60):y)-recovery.y;
    float edge=offset*(mode>1.5?22.:28.);
    float tail=(offset+(mode==1.?-.037:.037))*10.;
    float ridge=exp(-edge*edge);
    float wake=exp(-tail*tail);
    float shimmer=.80+.20*cos(time*23.+y*31.);
    float opacity=recovery.w*(ridge*.29*shimmer+wake*.15);
    if(mode>2.5) {
      opacity=recovery.w*(.09+.44*exp(-pow((y-.61)*5.2,2.)))*(.75+.25*key);
    }
    shaded=mix(shaded,mix(recoveryTint,vec3(.84,1.,1.),ridge*.25),opacity);
  }
  if(groundLight.w>0.) {
    float r=length(world.xz-groundLight.xy)/groundLight.z;
    float bounce=groundLight.w*exp(-r*r*2.85)*(.45+.55*max(0.,surface.y));
    bounce*=actorSurface>.5?.31:1.-smoothstep(.09,.65,world.y);
    shaded+=vec3(.29,.185,.063)*bounce;
  }
  float fade=smoothstep(4.,12.,length(world.xz));
  frag=vec4(mix(shaded,fog,fade),1.);
  glow=vec4(0.);
}`;
const skyVertex = `#version 300 es
out vec2 uv;
void main(){
  vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);
  uv=p;
  gl_Position=vec4(p*2.-1.,0.,1.);
}`;
const skyFragment = `#version 300 es
precision mediump float;
in vec2 uv;
uniform float daylight;
uniform float outdoor;
out vec4 frag;
void main(){
  vec3 low=mix(vec3(.11,.18,.18),vec3(.48,.59,.58),daylight);
  vec3 high=mix(vec3(.026,.065,.076),vec3(.26,.40,.43),daylight);
  vec3 color=mix(low,high,smoothstep(0.,.95,uv.y));
  color+=vec3(.055,.095,.080)*(1.-smoothstep(.0,.87,distance(uv,vec2(.49,.57))));
  float ridge=.42+sin(uv.x*9.+.5)*.05+sin(uv.x*18.)*.019;
  color=mix(color,low*.57,.5*(1.-smoothstep(ridge-.012,ridge+.009,uv.y)));
  if(outdoor>.5) {
    color=mix(vec3(.51,.59,.64),vec3(.19,.34,.60),smoothstep(.02,.99,uv.y));
    for(int i=0;i<5;i++) {
      float x=float(i)*.237+.02;
      vec2 p=uv-vec2(x,.79+sin(x*21.)*.075);
      float cloud=step(abs(p.x),.073)*step(abs(p.y),.009)
        +step(abs(p.x+.03),.032)*step(abs(p.y-.011),.008);
      color=mix(color,vec3(.77,.72,.72),clamp(cloud,0.,1.)*.54);
    }
    color*=.73+.27*daylight;
  }
  frag=vec4(color,1.);
}`;
const compositeFragment = `#version 300 es
precision highp float;
in vec2 uv;
uniform sampler2D scene;
uniform sampler2D emission;
uniform vec4 crop;
out vec4 frag;
void main(){
  vec2 p=mix(crop.xy,crop.zw,uv);
  vec2 pixel=1./vec2(textureSize(scene,0));
  vec3 bloom=texture(emission,p).rgb*.13;
  for(int i=0;i<8;i++){
    float a=float(i)*.785398;
    vec2 d=vec2(cos(a),sin(a))*pixel;
    bloom+=texture(emission,clamp(p+d*3.8,crop.xy+pixel*.5,crop.zw-pixel*.5)).rgb*.061;
    bloom+=texture(emission,clamp(p+d*10.4,crop.xy+pixel*.5,crop.zw-pixel*.5)).rgb*.037;
  }
  vec3 color=texture(scene,p).rgb;
  frag=vec4(1.-(1.-color)*exp(-bloom*1.75),1.);
}`;

function program(gl, vs, fs, attributes, uniformNames) {
  function shader(type, source) {
    const sh = gl.createShader(type);
    gl.shaderSource(sh, source.replace(/^\uFEFF/, ""));
    gl.compileShader(sh);
    if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) throw Error(gl.getShaderInfoLog(sh));
    return sh;
  }
  const vert = shader(gl.VERTEX_SHADER, vs), frag = shader(gl.FRAGMENT_SHADER, fs);
  const handle = gl.createProgram();
  gl.attachShader(handle, vert);
  gl.attachShader(handle, frag);
  gl.linkProgram(handle);
  gl.deleteShader(vert);
  gl.deleteShader(frag);
  if (!gl.getProgramParameter(handle, gl.LINK_STATUS)) throw Error(gl.getProgramInfoLog(handle));
  const uniforms = Object.fromEntries(uniformNames.map(key => [key, gl.getUniformLocation(handle, key)]));
  const stride = attributes.reduce((sum, [,size]) => sum + size, 0);
  const vao = gl.createVertexArray();
  gl.bindVertexArray(vao);
  const buffer = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
  let offset = 0;
  for (const [name,size] of attributes) {
    const location = gl.getAttribLocation(handle, name);
    gl.enableVertexAttribArray(location);
    gl.vertexAttribPointer(location,size,gl.FLOAT,false,stride*4,offset*4);
    offset += size;
  }
  return {handle,uniforms,vao,buffer,stride};
}

export function createRenderer(canvas, vs, fs) {
  const gl = canvas.getContext("webgl2", {alpha:true, antialias:true});
  if (!gl) throw Error("此浏览器未提供 WebGL 2。");
  const sky = program(gl,skyVertex,skyFragment,[],["daylight","outdoor"]);
  const opaque = program(gl,solidVertex,solidFragment,[["position",3],["normal",3],["tint",3],["character",1]],
    ["camera","daylight","fog","recovery","recoveryTint","groundLight","time"]);
  const effect = program(gl,vs,fs,[["position",3],["uv",2],["tint",3],["style",3]],
    ["camera","time","opaqueScene","sampleBounds","viewRight","viewUp","viewBack"]);
  const composite = program(gl,skyVertex,compositeFragment,[],["scene","emission","crop"]);
  const fbo = gl.createFramebuffer(), depth = gl.createRenderbuffer();
  const attachments = [gl.COLOR_ATTACHMENT0,gl.COLOR_ATTACHMENT1];
  const textures = attachments.map(()=>gl.createTexture());
  const opaqueScene=gl.createTexture();
  let targetWidth=0, targetHeight=0;
  const rects=[];
  gl.blendFunc(gl.ONE,gl.ONE_MINUS_SRC_ALPHA);
  function target() {
    gl.bindFramebuffer(gl.FRAMEBUFFER,fbo);
    if(targetWidth===canvas.width && targetHeight===canvas.height) return;
    targetWidth=canvas.width;
    targetHeight=canvas.height;
    [...textures,opaqueScene].forEach((texture,i)=>{
      gl.bindTexture(gl.TEXTURE_2D,texture);
      gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA8,targetWidth,targetHeight,0,gl.RGBA,gl.UNSIGNED_BYTE,null);
      gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.LINEAR);
      gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.LINEAR);
      gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE);
      gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE);
      if(i<attachments.length) gl.framebufferTexture2D(gl.FRAMEBUFFER,attachments[i],gl.TEXTURE_2D,texture,0);
    });
    gl.bindRenderbuffer(gl.RENDERBUFFER,depth);
    gl.renderbufferStorage(gl.RENDERBUFFER,gl.DEPTH_COMPONENT24,targetWidth,targetHeight);
    gl.framebufferRenderbuffer(gl.FRAMEBUFFER,gl.DEPTH_ATTACHMENT,gl.RENDERBUFFER,depth);
    if(gl.checkFramebufferStatus(gl.FRAMEBUFFER)!==gl.FRAMEBUFFER_COMPLETE) throw Error("发光缓冲初始化失败");
  }
  function bind(p) {
    gl.useProgram(p.handle);
    gl.bindVertexArray(p.vao);
  }
  function upload(p, data) {
    gl.bindBuffer(gl.ARRAY_BUFFER,p.buffer);
    gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(data),gl.STREAM_DRAW);
    gl.drawArrays(gl.TRIANGLES,0,data.length/p.stride);
  }
  return {
    clear() {
      rects.length=0;
      target();
      gl.drawBuffers(attachments);
      gl.disable(gl.SCISSOR_TEST);
      gl.clearColor(0,0,0,0);
      gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
      gl.enable(gl.SCISSOR_TEST);
    },
    draw(rect, camera, solid, effects, time, day, outdoor=false) {
      rects.push(rect);
      gl.viewport(...rect);
      gl.scissor(...rect);
      gl.drawBuffers([attachments[0]]);
      gl.disable(gl.DEPTH_TEST);
      gl.disable(gl.BLEND);
      bind(sky);
      gl.uniform1f(sky.uniforms.daylight,day);
      gl.uniform1f(sky.uniforms.outdoor,Number(outdoor));
      gl.drawArrays(gl.TRIANGLES,0,3);
      gl.enable(gl.DEPTH_TEST);
      gl.drawBuffers(attachments);
      bind(opaque);
      gl.uniformMatrix4fv(opaque.uniforms.camera,false,camera.matrix);
      gl.uniform1f(opaque.uniforms.daylight,day);
      gl.uniform3fv(opaque.uniforms.fog,outdoor?[.36,.49,.53]:day?[.30,.41,.40]:[.078,.133,.139]);
      gl.uniform4fv(opaque.uniforms.recovery,effects.recovery);
      gl.uniform3fv(opaque.uniforms.recoveryTint,effects.recoveryTint);
      gl.uniform4fv(opaque.uniforms.groundLight,effects.groundLight);
      gl.uniform1f(opaque.uniforms.time,time);
      upload(opaque,solid);
      const [x,y,w,h]=rect;
      gl.activeTexture(gl.TEXTURE2);
      gl.bindTexture(gl.TEXTURE_2D,opaqueScene);
      if(effects.refractive) {
        gl.readBuffer(gl.COLOR_ATTACHMENT0);
        gl.copyTexSubImage2D(gl.TEXTURE_2D,0,x,y,x,y,w,h);
      }
      gl.depthMask(false);
      gl.enable(gl.BLEND);
      bind(effect);
      gl.uniformMatrix4fv(effect.uniforms.camera,false,camera.matrix);
      gl.uniform1f(effect.uniforms.time,time);
      gl.uniform1i(effect.uniforms.opaqueScene,2);
      gl.uniform4f(effect.uniforms.sampleBounds,x+.5,y+.5,x+w-.5,y+h-.5);
      gl.uniform3fv(effect.uniforms.viewRight,camera.right);
      gl.uniform3fv(effect.uniforms.viewUp,camera.up);
      gl.uniform3fv(effect.uniforms.viewBack,camera.back);
      upload(effect,effects.vertices);
      gl.depthMask(true);
    },
    finish() {
      gl.bindFramebuffer(gl.FRAMEBUFFER,null);
      gl.disable(gl.DEPTH_TEST);
      gl.disable(gl.BLEND);
      gl.disable(gl.SCISSOR_TEST);
      gl.clear(gl.COLOR_BUFFER_BIT);
      bind(composite);
      textures.forEach((texture,i)=>{
        gl.activeTexture(gl.TEXTURE0+i);
        gl.bindTexture(gl.TEXTURE_2D,texture);
      });
      gl.uniform1i(composite.uniforms.scene,0);
      gl.uniform1i(composite.uniforms.emission,1);
      for(const [x,y,w,h] of rects) {
        gl.viewport(x,y,w,h);
        gl.uniform4f(composite.uniforms.crop,x/targetWidth,y/targetHeight,(x+w)/targetWidth,(y+h)/targetHeight);
        gl.drawArrays(gl.TRIANGLES,0,3);
      }
    },
    error: () => gl.getError(),
  };
}

const cubeFaces = [
  [[0,0,1],[4,5,7,6]],[[0,0,-1],[1,0,2,3]],[[1,0,0],[5,1,3,7]],
  [[-1,0,0],[0,4,6,2]],[[0,1,0],[6,7,3,2]],[[0,-1,0],[0,1,5,4]],
];
const quad = [0,1,2,0,2,3];
const identity = v => v;
export function box(out, center, size, color, orient = identity, character = 0) {
  const p = Array.from({length:8},(_, i) => add(center,orient(size.map((v,k) => i&(1<<k) ? v*.5 : -v*.5))));
  for (const [normal,corners] of cubeFaces) {
    const n = orient(normal);
    for (const index of quad) out.push(...p[corners[index]],...n,...color,character);
  }
}

export function stage(attack=false) {
  const out = [];
  if(attack==="ice" || attack==="lotus") {
    box(out,[0,-.20,.5],[32,.33,19],[.21,.33,.23]);
    for(let i=-5;i<=5;i++) {
      const rise=.21+.39*Math.pow(.5+.5*Math.sin(i*.91+1.2),2);
      box(out,[i*2.25,rise*.5-.11,-7.5],[2.28,rise,3.05],[.19,.31,.26]);
      box(out,[i*2.25,rise-.095,-8.27],[2.28,.08,1.57],[.26,.39,.31]);
    }
    for(const [x,z,h] of [[-7,-6.1,.92],[-4.5,-7.3,1.22],[3.5,-7.8,1.08],[7.1,-6.7,1.15]]) {
      box(out,[x,.51,z],[.16,1.02,.16],[.23,.24,.18]);
      box(out,[x,h+.22,z],[.97,.58,.93],[.23,.37,.26]);
      box(out,[x-.15,h+.56,z-.07],[.73,.36,.71],[.29,.43,.29]);
    }
    for(let x=-8;x<=8;x++) for(let z=-5;z<=5;z++) {
      const road=attack==="ice"?x<=-3 || (z===-3 && x<5):Math.abs(x)<=2 && z>=-2 && z<=2;
      const tint=((x*x*7+z*z*19+x*z*3)&7)*.009;
      const color=road?[.36+tint,.40+tint,.47+tint]:[.21+tint,.34+tint,.23+tint*.65];
      box(out,[x,-.046,z],[road?.975:1.01,.097,road?.97:1.01],color);
      if(road) continue;
      for(let i=0;i<5;i++) {
        const a=Math.sin(x*2.7+z*3.5+i*14.3);
        const b=Math.cos(x*6.4+z*2.1+i*19.2);
        const moss=.018*((x+i*11-z*5)&7);
        box(out,[x+a*.41,.004,z+b*.40],[.13+Math.abs(a)*.12,.004,.11+Math.abs(b)*.13],
          [.18+moss,.31+moss,.21+moss*.62]);
      }
    }
    return out;
  }
  if(attack) {
    box(out,[0,-.20,0],[17,.32,14],[.19,.24,.25]);
    for(let x=-7;x<=7;x++) for(let z=-5;z<=5;z++) {
      const tint=((x*x*7+z*z*19+x*z*3)&7)*.005;
      box(out,[x,-.045,z],[.977,.095,.977],[.24+tint,.29+tint,.30+tint]);
    }
    return out;
  }
  box(out,[0,-.20,1.34],[21,.32,10.8],[.19,.24,.25]);
  for(let x=-4;x<=4;x++) for(let z=-2;z<=3;z++) {
    const ink = ((x*x*7 + z*z*19 + x*z*3)&7)*.008;
    box(out,[x*.85,-.045,z*.85],[.826,.095,.826],[.255+ink,.315+ink,.32+ink]);
    if(z===-2 || z===3) box(out,[x*.85,.003,z*.85],[.80,.004,.029],[.34,.40,.36]);
  }
  for (let i=-4; i<=4; i++) {
    const x=i*.99;
    box(out,[x,.073,-2.66],[.982,.12,.39],[.22,.29,.28]);
    box(out,[x,.28,-2.66],[.925,.085,.165],[.32,.40,.38]);
    box(out,[x+.43,.17,-2.66],[.105,.21,.19],[.26,.33,.33]);
    if(i%2!==0) {
      box(out,[x,.20,-2.66],[.19,.38,.28],[.27,.35,.34]);
      box(out,[x,.40,-2.66],[.29,.071,.34],[.37,.45,.41]);
      box(out,[x,.455,-2.66],[.14,.043,.18],[.43,.47,.39]);
    }
  }
  return out;
}

const cloth = [.22,.36,.36], dark = [.12,.23,.24], jade = [.39,.67,.59];
const gold = [.67,.52,.30], skin = [.77,.60,.45], hair = [.094,.13,.14];
const armAnchor = side => [side*.371,1.37,0];
const armSwing = (side,stride,hit,guard) => -side*stride*.68+(side===1?.83*hit:.25*guard);
const armSpread = (side,hit,guard) => side*(.15+.20*guard)-(side===1?.66*hit:0);
const yawRotation = turn => v => [v[0]*Math.cos(turn)+v[2]*Math.sin(turn),v[1],v[2]*Math.cos(turn)-v[0]*Math.sin(turn)];
function limbFrame(root,yaw,anchor,swing,spread) {
  const orient = v => {
    const a = [v[0],v[1]*Math.cos(swing)+v[2]*Math.sin(swing),v[2]*Math.cos(swing)-v[1]*Math.sin(swing)];
    return yaw([a[0]*Math.cos(spread)-a[1]*Math.sin(spread),a[0]*Math.sin(spread)+a[1]*Math.cos(spread),a[2]]);
  };
  const origin=add(root,yaw(anchor));
  return {orient,point:v=>add(origin,orient(v))};
}
const armLength=.51;
const swordEnd=[.02,-armLength*1.05,.72];
export function weaponTip({root,yaw,stride,hit,guard}) {
  const arm=limbFrame(root,yawRotation(yaw),armAnchor(1),armSwing(1,stride,hit,guard),armSpread(1,hit,guard));
  return {center:arm.point(swordEnd), forward:arm.orient([0,0,1]), side:arm.orient([1,0,0])};
}
export function actor(out, root, stride, turn, hit, guard, armed, casting=false) {
  const yaw=yawRotation(turn);
  const body = (center,size,color) => box(out,add(root,yaw(center)),size,color,yaw,1);
  body([0,1.12,0],[.54,.63,.30],cloth);
  body([0,1.33,.151],[.38,.09,.024],jade);
  body([-.134,1.205,.16],[.054,.27,.02],dark);
  body([0,.943,0],[.57,.11,.328],dark);
  body([0,.945,.174],[.125,.07,.026],gold);
  body([0,.815,-.08],[.51,.15,.17],cloth);
  body([0,1.478,0],[.18,.13,.17],skin);
  body([0,1.69,0],[.412,.407,.385],skin);
  body([0,1.88,-.01],[.45,.123,.414],hair);
  body([0,1.71,-.17],[.44,.278,.106],hair);
  body([.16,1.815,.147],[.12,.093,.127],hair);
  body([0,2.005,-.112],[.15,.14,.15],hair);
  let hand;
  let blade;
  for (const side of [-1,1]) {
    body([side*.094,1.688,.197],[.049,.027,.015],[.14,.22,.23]);
    limb([side*.147,.825,0],.745,.21,side*stride,-side*.025,false);
    limb(armAnchor(side),armLength,.176,armSwing(side,stride,hit,guard),armSpread(side,hit,guard),true);
  }
  function limb(anchor,length,width,swing,spread,arm) {
    const {orient,point}=limbFrame(root,yaw,anchor,swing,spread);
    box(out,point([0,-length*.45,0]),[width,length*.92,width],arm?cloth:dark,orient,1);
    box(out,point([0,-length*.77,0]),[width*1.07,.082,width*1.08],arm?jade:cloth,orient,1);
    box(out,point([0,-length*.99,arm?0:.041]),[width*.85,.132,width*(arm?.86:1.39)],arm?skin:hair,orient,1);
    if(arm && anchor[0]>0) hand=point([.018,-length*.97,.105]);
    if(arm && anchor[0]>0 && !casting && (armed || hit>.09)) {
      box(out,point([.02,-length*1.05,.15]),[.21,.059,.063],gold,orient,1);
      box(out,point([.02,-length*1.05,.44]),[.102,.038,.56],[.57,.68,.71],orient,1);
      blade=point(swordEnd);
    }
  }
  return {hand,blade};
}

export function effectMesh(camera) {
  const surfaces = [];
  let recovery=[0,0,0,0], recoveryTint=[0,0,0];
  let groundLight=[0,0,1,0];
  let refractive=false;
  function face(corners, us, color, kind, strength, seed, ys=[-1,1,1,-1]) {
    if (strength <= .004) return;
    if(kind===13) refractive=true;
    const center = mul(corners.reduce(add),.25);
    const data = [];
    for (const i of quad) data.push(...corners[i],us[i],ys[i],...color,kind,strength,seed);
    surfaces.push({data,depth:dot(center,camera.back)});
  }
  function plane(center,h,w,color,kind,strength,seed) {
    face([sub(sub(center,h),w),add(sub(center,h),w),add(add(center,h),w),sub(add(center,h),w)],
      [0,0,1,1],color,kind,strength,seed);
  }
  function stamp(center, height, width, color, kind, strength, seed, angle = 0) {
    const a = add(mul(camera.up,Math.cos(angle)),mul(camera.right,Math.sin(angle)));
    const b = add(mul(camera.right,Math.cos(angle)),mul(camera.up,-Math.sin(angle)));
    const h = mul(a,height*.5), w = mul(b,width*.5);
    plane(center,h,w,color,kind,strength,seed);
  }
  function ribbon(points, width, color, strength, seed = 0, kind = 0) {
    if (strength <= .004) return;
    const tangents = points.map((_,i) => norm(sub(points[Math.min(i+1,points.length-1)],points[Math.max(0,i-1)])));
    const sides = tangents.map(t => {
      const c = cross(t,camera.back);
      return Math.hypot(...c)>.001 ? norm(c) : camera.right;
    });
    const distance = [0];
    for(let i=1;i<points.length;i++) distance.push(distance[i-1]+Math.hypot(...sub(points[i],points[i-1])));
    const length = distance.at(-1);
    if (length < .001) return;
    const edges = points.map((point,i) => {
      const u = distance[i]/length;
      const taper = .18+.82*Math.pow(Math.sin(Math.PI*u),.49);
      const side = mul(sides[i],width*taper);
      return [sub(point,side),add(point,side)];
    });
    for(let i=1;i<points.length;i++) {
      const a = distance[i-1]/length, b = distance[i]/length;
      face([...edges[i-1],edges[i][1],edges[i][0]],[a,a,b,b],color,kind,strength,seed);
    }
  }
  function ground(center,size,color,strength,seed) {
    const [x,y,z]=center;
    face([[x-size,y,z-size],[x+size,y,z-size],[x+size,y,z+size],[x-size,y,z+size]],
      [0,0,1,1],color,4,strength,seed);
  }
  function crystal(tip,axis,length,radius,strength,seed) {
    if(strength<=.004) return;
    const side=norm(cross(axis,Math.abs(axis[1])>.95?[0,0,1]:[0,1,0]));
    const up=cross(side,axis);
    const outline=[[1,.68],[.68,1],[-.68,1],[-1,.68],[-1,-.68],[-.68,-1],[.68,-1],[1,-.68]];
    const profile=[[0,.19],[.035,.57],[.09,.72],[.10,.88],[.174,.91],[.187,1.08],[.265,1.08],[.303,1],[.994,.018],[1,0]];
    const angle=.10+Math.sin(seed*8.13)*.24;
    const rows=profile.map(([u,r],row)=>outline.map(([y,z],i)=>{
      const lobe=row>=2&&row<=6?1+.08*Math.sin(i*2.7+seed):1;
      const offset=radius*r*lobe;
      const center=add(tip,mul(axis,-length*(1-u)));
      return add(center,add(mul(up,offset*(y*Math.cos(angle)-z*Math.sin(angle))),
        mul(side,offset*(z*Math.cos(angle)+y*Math.sin(angle)))));
    }));
    const faces=[[.19,.58,.92],[.48,.79,.99],[.63,.86,1],[.30,.69,.96],
      [.12,.39,.73],[.17,.45,.83],[.25,.57,.91],[.39,.73,.99]];
    for(let j=1;j<rows.length;j++) for(let i=0;i<outline.length;i++) {
      const next=(i+1)%outline.length;
      face([rows[j-1][i],rows[j-1][next],rows[j][next],rows[j][i]],
        [profile[j-1][0],profile[j-1][0],profile[j][0],profile[j][0]],faces[i],13,strength,seed*9+i);
    }
  }
  return {
    stamp,ribbon,ground,plane,crystal,face,
    heal:(mode,crest,bottom,strength,tint)=>{
      recovery=[mode,crest,bottom,strength];
      recoveryTint=tint;
    },
    groundLight:(root,radius,strength)=>{groundLight=[root[0],root[2],radius,strength];},
    finish:()=>({recovery,recoveryTint,groundLight,refractive,vertices:surfaces.sort((a,b)=>a.depth-b.depth).flatMap(s=>s.data)}),
  };
}
