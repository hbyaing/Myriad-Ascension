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
uniform vec4 iceGround;
uniform vec3 iceContact;
uniform vec4 flameGround;
uniform float time;
layout(location=0) out vec4 frag;
layout(location=1) out vec4 glow;
float stoneHash(vec2 p) {
  return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);
}
float iceGrain(vec2 p) {
  vec2 i=floor(p), f=fract(p);
  f=f*f*(3.-2.*f);
  return mix(mix(stoneHash(i),stoneHash(i+vec2(1,0)),f.x),
    mix(stoneHash(i+vec2(0,1)),stoneHash(i+1.),f.x),f.y);
}
float falloff(float x) { return exp(-x*x); }
void main(){
  float key=max(0.,dot(surface,normalize(vec3(-.6,.9,.48))));
  float rim=max(0.,dot(surface,normalize(vec3(.5,.26,-1.))));
  vec3 shaded=color*(.45+.57*key+.24*rim+daylight*.27);
  vec3 emissive=vec3(0.);
  bool actor=actorSurface>.5 && actorSurface<1.5;
  if(actor && recovery.w>0.) {
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
    bounce*=actor?.31:1.-smoothstep(.09,.65,world.y);
    shaded+=vec3(.29,.185,.063)*bounce;
  }
  if(flameGround.w>=0.) {
    vec2 p=world.xz-flameGround.xy;
    float r=length(p), angle=abs(atan(p.y,p.x+.00001)), head=flameGround.z;
    float fan=1.-smoothstep(.82,.960,angle);
    float relativeAge=flameGround.w-.9*(1.-pow(1.-clamp(r/6.5,0.,1.),.729927));
    float contact=smoothstep(-.03,.11,relativeAge)*(1.-smoothstep(.88,1.74,relativeAge));
    float floorLight=(.82*falloff((r-head+.30)*1.53)+.26*falloff((r-head+1.22)*.73))
      *fan*smoothstep(.17,1.4,head)*(1.-smoothstep(.70,1.34,flameGround.w));
    float facing=.30+.70*max(0.,surface.y);
    shaded+=vec3(.57,.205,.042)*floorLight*facing*(1.-smoothstep(.35,2.6,world.y));
    if(actorSurface>1.5 && surface.y>.65) {
      vec2 cell=floor(world.xz*26.);
      float grain=iceGrain(cell*.11+vec2(4.4,8.3));
      float splatter=iceGrain(cell*.29+8.);
      float scorched=contact*fan*smoothstep(.6,1.3,r)*(1.-smoothstep(6.42,6.51,r));
      shaded=mix(shaded,shaded*vec3(.61,.43,.37),scorched*smoothstep(.29,.79,grain)*.72);
      float cinder=smoothstep(.71,.91,stoneHash(cell))*smoothstep(.48,.79,grain*.68+.32*splatter);
      float shine=scorched*cinder*(1.-smoothstep(.28,1.25,relativeAge))*(.67+.33*sin(time*9.+splatter*13.));
      shaded+=vec3(.78,.31,.057)*(shine*.83+floorLight*grain*.30);
      emissive+=vec3(1.,.39,.066)*(shine*.71+floorLight*.21);
    }
  }
  if(actorSurface>1.5 && surface.y>.65) {
    vec2 tile=fract(world.xz+.5)-.5;
    float grain=stoneHash(floor(world.xz*23.));
    float blocks=iceGrain(floor(world.xz*27.)*.118);
    float chisels=iceGrain(floor(world.xz*11.)*.22+19.);
    float lip=1.-smoothstep(.010,.052,.5-max(abs(tile.x),abs(tile.y)));
    shaded*=.78+.23*blocks+.13*chisels+.08*grain;
    shaded=mix(shaded,shaded*.67,lip*.57);
    if(iceGround.w>=0.) {
      vec2 p=world.xz-iceGround.xy;
      float head=iceGround.z;
      float age=iceGround.w-(p.x-.88)*.09103448;
      vec2 chips=floor(world.xz*27.);
      float wind=iceGrain(chips*vec2(.095,.178)+vec2(8.1,2.4));
      float sweep=sin(p.x*1.12)*.14;
      float distanceToTrace=abs(p.y-sweep)+.31*(wind-.42);
      float spread=.58+.29*smoothstep(.1,.74,age);
      float track=(1.-smoothstep(spread*.47,spread*1.41,distanceToTrace))
        *smoothstep(.28,1.04,p.x)*(1.-smoothstep(head-.04,head+.38,p.x));
      float residue=smoothstep(-.045,.11,age)*(1.-smoothstep(.72,1.40,age));
      float rime=track*residue*smoothstep(.33,.78,wind*.75+.25*stoneHash(chips+51.));
      float flecks=track*residue*smoothstep(.56,.93,stoneHash(chips-15.))*smoothstep(.36,.70,wind);
      vec2 crystalCell=floor(world.xz*3.2), crystal=fract(world.xz*3.2)-.5;
      crystal+=(vec2(stoneHash(crystalCell+35.),stoneHash(crystalCell-27.))-.5)*.28;
      float cellPhase=stoneHash(crystalCell+91.)*.9;
      float crystalAngle=(fract((atan(crystal.y,crystal.x+.00001)+cellPhase)/1.04719755+.5)-.5)*1.04719755;
      vec2 ray=vec2(cos(crystalAngle),sin(crystalAngle))*length(crystal);
      float twig=1.-smoothstep(.012,.034,abs(abs(ray.y)-.68*(ray.x-.139)));
      twig*=smoothstep(.11,.17,ray.x)*(1.-smoothstep(.27,.36,ray.x));
      float twigStem=(1.-smoothstep(.014,.033,abs(ray.y)))*(1.-smoothstep(.24,.43,ray.x));
      float crystalMask=max(twig*.84,twigStem)*smoothstep(.34,.62,stoneHash(crystalCell));
      float frozen=crystalMask*track*residue;
      float leading=falloff((p.x-head+.44)*1.17);
      float wake=falloff((p.x-head+1.87)*.66)*.43;
      float halo=(leading+wake)*falloff((p.y-sweep)*1.47)
        *smoothstep(.35,1.22,head)*(1.-smoothstep(.66,1.20,iceGround.w));
      float band=.57+.43*iceGrain(chips*vec2(.073,.21)+vec2(14.,0.));
      vec2 hit=(p-iceContact.xy)*vec2(1.07,1.62);
      float hitAge=iceGround.w-iceContact.z;
      float impact=falloff(length(hit)*1.44)*smoothstep(0.,.11,hitAge)*(1.-smoothstep(.37,1.07,hitAge));
      float iceFilm=clamp(.79*rime+.47*flecks+.26*impact,0.,.83);
      shaded=mix(shaded,vec3(.43,.65,.79)*(.71+.39*blocks),iceFilm);
      shaded=mix(shaded,vec3(.66,.84,.96),clamp(frozen*.85+flecks*.22,0.,.87));
      shaded+=vec3(.15,.39,.57)*(halo*band*.79+impact*.45);
      emissive=vec3(.25,.69,1.)*(halo*band*.65+impact*.27+flecks*.13+frozen*.12);
    }
  }
  float fade=smoothstep(4.,12.,length(world.xz));
  frag=vec4(mix(shaded,fog,fade),1.);
  glow=vec4(emissive,0.);
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
    ["camera","daylight","fog","recovery","recoveryTint","groundLight","iceGround","iceContact","flameGround","time"]);
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
      gl.uniform4fv(opaque.uniforms.iceGround,effects.iceGround);
      gl.uniform3fv(opaque.uniforms.iceContact,effects.iceContact);
      gl.uniform4fv(opaque.uniforms.flameGround,effects.flameGround);
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
    if(attack==="ice") {
      for(let x=-7;x<=7;x++) {
        const tone=(x*x%5)*.013;
        box(out,[x,.14,-3.55],[.985,.31,.43],[.27+tone,.30+tone,.37+tone]);
        box(out,[x,.345,-3.55],[.97,.10,.53],[.37+tone,.40+tone,.44+tone]);
      }
      for(const x of [-5,4.85]) {
        box(out,[x,.29,-3.55],[.48,.63,.48],[.34,.37,.42]);
        box(out,[x,.69,-3.55],[.31,.22,.31],[.30,.26,.23]);
        box(out,[x,.71,-3.38],[.17,.14,.044],[.87,.63,.29]);
      }
      for(let i=0;i<22;i++) {
        const x=-7.4+i*.70;
        const z=i%3===0?2.84:-2.84;
        const height=.08+.17*(.5+.5*Math.sin(i*6.15));
        box(out,[x,height*.5,z],[.082,height,.062],[.27,.44,.21]);
        box(out,[x+.11,height*.35,z+.055],[.067,height*.69,.065],[.32,.47,.24]);
      }
    }
    for(let x=-8;x<=8;x++) for(let z=-5;z<=5;z++) {
      const road=attack==="ice"?x>=-6&&x<=6&&Math.abs(z)<=2:Math.abs(x)<=2 && z>=-2 && z<=2;
      const tint=((x*x*7+z*z*19+x*z*3)&7)*.009;
      const color=road?[.36+tint,.40+tint,.47+tint]:[.21+tint,.34+tint,.23+tint*.65];
      box(out,[x,-.046,z],[road?.978:1.01,.097,road?.978:1.01],color,identity,attack==="ice"&&road?2:0);
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
      box(out,[x,-.045,z],[.977,.095,.977],[.25+tint,.28+tint,.315+tint],identity,2);
    }
    for(let x=-7;x<=7;x++) {
      const tint=((x*x+11)&7)*.010;
      box(out,[x,.21,-4.83],[.99,.42,.49],[.24+tint,.27+tint,.29+tint]);
      box(out,[x,.47,-4.83],[.99,.10,.57],[.32+tint,.35+tint,.37+tint]);
    }
    for(const x of [-6.2,5.5]) {
      box(out,[x,.52,-4.82],[.54,1.05,.54],[.29,.32,.34]);
      box(out,[x,1.08,-4.82],[.64,.15,.64],[.36,.38,.40]);
      box(out,[x,1.28,-4.82],[.28,.24,.30],[.40,.23,.14]);
      box(out,[x,1.28,-4.65],[.15,.15,.046],[.87,.53,.26]);
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
    const swing=casting==="ice"?(side===1?1.37*hit+.29*guard:.31*guard):
      casting==="flame"?(side===1?1.48*hit+.67*guard:.60*guard+.18*hit):armSwing(side,stride,hit,guard);
    const spread=casting==="ice"?side*(.10+.51*guard):
      casting==="flame"?side*(.10+.21*guard):armSpread(side,hit,guard);
    limb(armAnchor(side),armLength,.176,swing,spread,true);
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
  let iceGround=[0,0,0,-1];
  let flameGround=[0,0,0,-1];
  let iceContact=[0,0,0];
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
  function crystal(tip,axis,length,radius,strength,seed,fragment=false) {
    if(strength<=.004) return;
    const side=norm(cross(axis,Math.abs(axis[1])>.95?[0,0,1]:[0,1,0]));
    const up=cross(side,axis);
    const outline=[[1.04,.12],[.56,.94],[-.39,1.02],[-.86,.48],[-.72,-.66],[-.10,-.98],[.73,-.68]];
    const faces=[[.34,.63,.93],[.50,.77,1],[.20,.43,.80],[.11,.32,.63],[.18,.44,.79],[.33,.62,.90],[.61,.83,1]];
    function prism(end,extent,girth,angle,profile,brightness,variant) {
      const c=Math.cos(angle), s=Math.sin(angle);
      const rows=profile.map(([u,r],row)=>outline.map(([y,z],i)=>{
        const facet=i*2.7+variant;
        const offset=girth*r*(1+.075*Math.sin(facet+row*.76));
        const lengthCut=(row===0||row===profile.length-1)?0:extent*.032*Math.sin(facet+row*1.71);
        const center=add(end,mul(axis,-extent*(1-u)+lengthCut));
        return add(center,add(mul(up,offset*(y*c-z*s)),mul(side,offset*(z*c+y*s))));
      }));
      for(let j=1;j<rows.length;j++) for(let i=0;i<outline.length;i++) {
        const next=(i+1)%outline.length;
        face([rows[j-1][i],rows[j-1][next],rows[j][next],rows[j][i]],
          [profile[j-1][0],profile[j-1][0],profile[j][0],profile[j][0]],faces[i],13,brightness,variant*7+i);
      }
      const heel=add(end,mul(axis,-extent));
      for(let i=0;i<outline.length;i++) {
        const next=(i+1)%outline.length;
        face([heel,rows[0][next],rows[0][i],heel],[0,.022,.022,0],faces[i],13,brightness*.85,variant*7+i);
      }
    }
    const angle=.12+Math.sin(seed*8.13)*(fragment?.71:.23);
    const shape=fragment?[[0,.08],[.15,.68],[.33,1],[.58,.69],[1,0]]
      :[[0,.46],[.086,.83],[.217,1.04],[.337,.89],[.58,.54],[.82,.22],[1,0]];
    prism(tip,length,radius,angle,shape,strength,seed);
    if(!fragment) for(let plate=0;plate<3;plate++) {
      const a=angle+plate*2.02+.51;
      const edge=add(mul(up,Math.cos(a)),mul(side,Math.sin(a)));
      const end=add(add(tip,mul(axis,-length*(.40+plate*.076))),mul(edge,radius*(.82+plate*.087)));
      prism(end,length*(.49-.053*plate),radius*(.39-.033*plate),a,
        [[0,.53],[.16,.96],[.43,.90],[1,0]],strength*.97,seed*3+plate+9);
    }
  }
  return {
    stamp,ribbon,ground,plane,crystal,face,
    heal:(mode,crest,bottom,strength,tint)=>{
      recovery=[mode,crest,bottom,strength];
      recoveryTint=tint;
    },
    groundLight:(root,radius,strength)=>{groundLight=[root[0],root[2],radius,strength];},
    iceGround:(root,front,age,target,hitTime)=>{
      iceGround=[root[0],root[2],front,age];
      iceContact=[target[0],target[2],hitTime];
    },
    flameGround:(root,front,age)=>{flameGround=[root[0],root[2],front,age];},
    finish:()=>({recovery,recoveryTint,groundLight,iceGround,iceContact,flameGround,refractive,
      vertices:surfaces.sort((a,b)=>a.depth-b.depth).flatMap(s=>s.data)}),
  };
}
