"use strict";

// Coordinates are in block units on the unfolded bark, starting at the front.
// Each integer x is a corner. The perimeter wraps at x = 4.
// y = 1 and 2 are the seams between logs.
const trunk = [
  [.38, .01], [.41, .13], [.46, .21], [.43, .34], [.47, .46], [.49, .54],
  [.45, .63], [.38, .65], [.35, .75], [.37, .86], [.42, .94], [.45, 1.03],
  [.54, 1.13], [.50, 1.29], [.55, 1.40], [.61, 1.48], [.64, 1.60],
  [.61, 1.72], [.67, 1.80], [.73, 1.83], [.81, 1.93], [.91, 1.98],
  [.95, 2.07], [1.00, 2.12], [1.05, 2.25], [1.09, 2.33], [1.21, 2.38],
  [1.22, 2.49], [1.32, 2.62], [1.42, 2.67], [1.40, 2.79], [1.46, 2.96]
];
const branches = [
  [9, [[.29, .87], [.21, .81], [.13, .76]]],
  [14, [[.49, 1.44], [.42, 1.43]]],
  [23, [[1.07, 2.01], [1.16, 1.91], [1.25, 1.90], [1.30, 1.96]]],
  [29, [[1.49, 2.57], [1.58, 2.50], [1.67, 2.53]]],
  [5, [[.58, .51], [.62, .44], [.68, .43], [.74, .35]]],
  [12, [[.61, 1.17], [.72, 1.13], [.78, 1.17], [.87, 1.10]]],
  [17, [[.55, 1.75], [.50, 1.86], [.40, 1.87], [.34, 1.99]]],
  [26, [[1.14, 2.44], [1.13, 2.54], [1.06, 2.60]]]
];
const sideVeins = [
  {
    start: .22, strength: .69,
    path: [[.12, .04], [.17, .26], [.10, .43], [.14, .59], [.09, .70],
      [.10, .92], [.18, 1.11], [.15, 1.28], [.21, 1.44], [.20, 1.57],
      [.27, 1.72], [.23, 1.86], [.25, 2.06], [.34, 2.24], [.30, 2.43]],
    forks: [[2, [[.03, .51], [-.04, .53], [-.08, .61]]],
      [6, [[.26, 1.16], [.29, 1.28], [.38, 1.34]]],
      [11, [[.16, 1.95], [.10, 1.98], [.05, 2.07]]]]
  },
  {
    start: .38, strength: .77,
    path: [[.78, .05], [.82, .19], [.77, .29], [.83, .45], [.88, .53],
      [.95, .58], [1.04, .69], [1.14, .68], [1.22, .81], [1.20, 1.01],
      [1.28, 1.15], [1.30, 1.32], [1.39, 1.44], [1.36, 1.61],
      [1.47, 1.76], [1.49, 1.97], [1.56, 2.13], [1.61, 2.33]],
    forks: [[5, [[.90, .68], [.87, .81], [.81, .89]]],
      [8, [[1.34, .87], [1.43, .81], [1.52, .85]]],
      [12, [[1.51, 1.49], [1.54, 1.61], [1.61, 1.65]]],
      [15, [[1.42, 2.03], [1.43, 2.14], [1.35, 2.23]]]]
  },
  {
    start: .09, strength: .69,
    path: [[1.64, .03], [1.70, .22], [1.67, .36], [1.77, .48], [1.79, .68],
      [1.87, .81], [1.83, .99], [1.91, 1.11], [2.01, 1.18], [2.11, 1.30],
      [2.12, 1.53], [2.21, 1.66], [2.16, 1.83], [2.25, 2.00],
      [2.28, 2.15], [2.37, 2.30], [2.33, 2.52], [2.43, 2.70], [2.41, 2.91]],
    forks: [[3, [[1.90, .40], [1.96, .31], [2.06, .32]]],
      [6, [[1.72, 1.05], [1.70, 1.18], [1.61, 1.23]]],
      [10, [[2.02, 1.59], [1.98, 1.74], [1.86, 1.81], [1.80, 1.96], [1.85, 2.19]]],
      [13, [[2.36, 2.01], [2.49, 2.09], [2.52, 2.21]]]]
  },
  {
    start: .24, strength: .88,
    path: [[2.56, .02], [2.51, .20], [2.60, .34], [2.57, .53], [2.66, .66],
      [2.63, .80], [2.72, .98], [2.78, 1.10], [2.72, 1.24], [2.80, 1.40],
      [2.92, 1.48], [2.96, 1.64], [3.08, 1.78], [3.11, 1.92],
      [3.20, 2.06], [3.16, 2.23], [3.27, 2.41], [3.25, 2.59], [3.36, 2.81]],
    forks: [[4, [[2.78, .65], [2.88, .57], [2.95, .62]]],
      [6, [[2.61, 1.02], [2.55, 1.14], [2.46, 1.16], [2.41, 1.27]]],
      [10, [[2.91, 1.36], [3.04, 1.29], [3.13, 1.31]]],
      [15, [[3.04, 2.30], [2.99, 2.42], [2.87, 2.47], [2.85, 2.61]]]]
  },
  {
    start: .33, strength: .80,
    path: [[3.52, .08], [3.46, .28], [3.56, .43], [3.52, .60], [3.60, .73],
      [3.57, .89], [3.64, 1.02], [3.73, 1.10], [3.67, 1.28], [3.74, 1.45],
      [3.70, 1.62], [3.79, 1.79], [3.86, 1.96], [3.84, 2.11],
      [3.93, 2.25], [3.98, 2.40], [4.09, 2.52], [4.07, 2.67], [4.18, 2.80], [4.17, 2.93]],
    forks: [[5, [[3.49, .95], [3.38, .96], [3.29, 1.09]]],
      [7, [[3.83, 1.13], [3.89, 1.28], [3.95, 1.32]]],
      [11, [[3.68, 1.87], [3.63, 1.98], [3.53, 2.02], [3.47, 2.16]]],
      [16, [[4.18, 2.51], [4.25, 2.56], [4.35, 2.55]]]]
  }
];
const fieldScale = 384;
const fieldRadius = .064;
const travelRange = 4.8;
const cycleLength = 5.6;

// The surface field stores distance to the nearest segment, travel distance from
// the root and strength. Forks inherit their parent's travel distance at the joint.
function buildField() {
  const w = 4 * fieldScale;
  const h = 3 * fieldScale;
  const bytes = new Uint8Array(w * h * 4);
  const distances = new Float32Array(w * h).fill(fieldRadius);
  let seed = 77137;

  function detail(points) {
    const samples = [points[0]];
    for (let i = 1; i < points.length; i++) {
      const [ax, ay] = points[i - 1];
      const [bx, by] = points[i];
      const dx = bx - ax;
      const dy = by - ay;
      const length = Math.hypot(dx, dy);
      const divisions = Math.max(2, Math.ceil(length / .019));
      for (let j = 1; j < divisions; j++) {
        seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
        const offset = (seed / 4294967296 - .5) * .022;
        const t = j / divisions;
        samples.push([ax + dx * t - dy / length * offset, ay + dy * t + dx / length * offset]);
      }
      samples.push(points[i]);
    }
    return samples;
  }

  function raster(points, start, strength, fork) {
    const knots = new Map([[points[0], start]]);
    let travel = start;
    for (let i = 1; i < points.length; i++) {
      const [ax, ay] = points[i - 1];
      const [bx, by] = points[i];
      const dx = bx - ax;
      const dy = by - ay;
      const length = Math.hypot(dx, dy);
      const x0 = Math.floor((Math.min(ax, bx) - fieldRadius) * fieldScale);
      const y0 = Math.max(0, Math.floor((Math.min(ay, by) - fieldRadius) * fieldScale));
      const x1 = Math.ceil((Math.max(ax, bx) + fieldRadius) * fieldScale);
      const y1 = Math.min(h, Math.ceil((Math.max(ay, by) + fieldRadius) * fieldScale));
      for (let y = y0; y < y1; y++) {
        for (let x = x0; x < x1; x++) {
          const rx = (x + .5) / fieldScale - ax;
          const ry = (y + .5) / fieldScale - ay;
          const t = Math.max(0, Math.min(1, (rx * dx + ry * dy) / (length * length)));
          const distance = Math.hypot(rx - dx * t, ry - dy * t);
          const pixel = y * w + (x + w) % w;
          if (distance >= distances[pixel]) continue;
          distances[pixel] = distance;
          const along = travel + length * t;
          const progress = (i - 1 + t) / (points.length - 1);
          const taper = fork ? 1 - .88 * progress
            : Math.min(1, Math.max(.13, (1 - progress) * 8), Math.max(.13, (3 - (ay + dy * t)) * 4));
          bytes[4 * pixel] = Math.round(255 * (1 - distance / fieldRadius));
          bytes[4 * pixel + 1] = Math.round(255 * along / travelRange);
          bytes[4 * pixel + 2] = Math.round(255 * strength * taper);
          bytes[4 * pixel + 3] = 255;
        }
      }
      travel += length;
      knots.set(points[i], travel);
    }
    return knots;
  }

  for (const {path, start, strength, forks} of [{path: trunk, start: 0, strength: 1, forks: branches}, ...sideVeins]) {
    const travel = raster(detail(path), start, strength, false);
    for (const [root, tips] of forks) {
      raster(detail([path[root], ...tips]), travel.get(path[root]), strength * .73, true);
    }
  }
  return {w, h, bytes};
}

const vertexSource = `#version 300 es
layout(location=0) in vec3 Position;
layout(location=1) in vec2 Uv;
layout(location=2) in float Face;
uniform vec4 Camera;
uniform vec2 Scale;
out vec2 uv;
flat out int face;
void main() {
  vec3 right = vec3(Camera.y, 0., -Camera.x);
  vec3 forward = vec3(Camera.x * Camera.w, Camera.z, Camera.y * Camera.w);
  vec3 up = cross(forward, right);
  gl_Position = vec4(dot(Position, right) * Scale.x, dot(Position, up) * Scale.y,
                     -.15 * dot(Position, forward), 1.);
  uv = Uv;
  face = int(Face);
}`;

const fragmentSource = `#version 300 es
precision highp float;
uniform sampler2D Wood;
uniform sampler2D Field;
uniform float Head;
uniform float Width;
uniform float Glow;
uniform int Mode;
in vec2 uv;
flat in int face;
out vec4 color;
void main() {
  float vertical = uv.y * 3.;
  vec2 local = face < 4 ? vec2(uv.x, fract(vertical)) : uv;
  vec2 tile = (vec2(32. * float(face), 0.) + clamp(local * 32., .5, 31.5)) / vec2(192., 32.);
  vec3 wood = texture(Wood, tile).rgb;
  float lighting = face == 4 ? 1.12 : face == 0 ? 1.02 : face == 1 ? .73 : .62;
  if (face < 4) {
    float seam = abs(vertical - round(vertical));
    float seamMask = (1. - smoothstep(.002, .009, seam)) * step(.5, vertical) * step(vertical, 2.5);
    lighting *= 1. - .44 * seamMask;
  }
  vec3 result = wood * lighting;
  if (Mode != 2 && face < 4) {
    vec4 field = texture(Field, vec2((float(face) + uv.x) * .25, uv.y));
    float d = (1. - field.r) * .064;
    float along = field.g * 4.8;
    float strength = field.b;
    float age = Head - along;
    float headOffset = (age - .018) / .092;
    float leading = exp(-headOffset * headOffset);
    float wake = smoothstep(-.04, .08, age) * exp(-max(age, 0.) * 3.1);
    float flicker = .9 + .1 * sin(along * 96. + Head * 37.);
    float pulse = Mode == 1 ? .7 : .075 + (.90 * leading + .61 * wake) * flicker;
    float halfWidth = Width * mix(.0017, .0037, strength);
    float aa = clamp(fwidth(d) * .70, .00065, .004);
    float core = 1. - smoothstep(max(0., halfWidth - aa), halfWidth + aa, d);
    float nearGlow = exp(-pow(d / (Width * .013), 2.));
    float spill = exp(-pow(d / (Width * .035), 2.));
    result += strength * pulse * (
      vec3(.63, .81, 1.) * core
      + Glow * (vec3(.13, .32, .77) * .48 * nearGlow + vec3(.07, .14, .33) * .22 * spill)
    );
  }
  color = vec4(result, 1.);
}`;

function modelVertices() {
  const corners = [
    [[-.5, 1.5, .5], [.5, 1.5, .5], [.5, -1.5, .5], [-.5, -1.5, .5]],
    [[.5, 1.5, .5], [.5, 1.5, -.5], [.5, -1.5, -.5], [.5, -1.5, .5]],
    [[.5, 1.5, -.5], [-.5, 1.5, -.5], [-.5, -1.5, -.5], [.5, -1.5, -.5]],
    [[-.5, 1.5, -.5], [-.5, 1.5, .5], [-.5, -1.5, .5], [-.5, -1.5, -.5]],
    [[-.5, 1.5, -.5], [.5, 1.5, -.5], [.5, 1.5, .5], [-.5, 1.5, .5]],
    [[-.5, -1.5, .5], [.5, -1.5, .5], [.5, -1.5, -.5], [-.5, -1.5, -.5]]
  ];
  const uv = [[0, 0], [1, 0], [1, 1], [0, 1]];
  const triangles = [0, 1, 2, 0, 2, 3];
  return new Float32Array(corners.flatMap((face, f) =>
    triangles.flatMap(index => [...face[index], ...uv[index], f])));
}

async function startPreview() {
  const canvas = document.querySelector("#preview");
  const gl = canvas.getContext("webgl2", {antialias: true});
  if (!gl) throw new Error("当前浏览器无法启动 WebGL2。");

  function compile(type, source) {
    const shader = gl.createShader(type);
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(shader));
    return shader;
  }

  const program = gl.createProgram();
  gl.attachShader(program, compile(gl.VERTEX_SHADER, vertexSource));
  gl.attachShader(program, compile(gl.FRAGMENT_SHADER, fragmentSource));
  gl.linkProgram(program);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(program));
  gl.useProgram(program);
  const uniforms = Object.fromEntries(["Camera", "Scale", "Wood", "Field", "Head", "Width", "Glow", "Mode"]
    .map(name => [name, gl.getUniformLocation(program, name)]));

  const buffer = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
  gl.bufferData(gl.ARRAY_BUFFER, modelVertices(), gl.STATIC_DRAW);
  for (const [index, size, offset] of [[0, 3, 0], [1, 2, 12], [2, 1, 20]]) {
    gl.enableVertexAttribArray(index);
    gl.vertexAttribPointer(index, size, gl.FLOAT, false, 24, offset);
  }

  function texture(unit, filter) {
    gl.activeTexture(gl.TEXTURE0 + unit);
    gl.bindTexture(gl.TEXTURE_2D, gl.createTexture());
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, filter);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, filter);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  }

  const image = new Image();
  image.src = woodAtlas;
  await image.decode();
  texture(0, gl.NEAREST);
  gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGB, gl.RGB, gl.UNSIGNED_BYTE, image);
  const field = buildField();
  texture(1, gl.LINEAR);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.REPEAT);
  gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, field.w, field.h, 0, gl.RGBA, gl.UNSIGNED_BYTE, field.bytes);
  gl.uniform1i(uniforms.Wood, 0);
  gl.uniform1i(uniforms.Field, 1);
  gl.enable(gl.DEPTH_TEST);

  let yaw = .58;
  let pitch = .39;
  let zoom = 1;
  let mode = 0;
  let paused = false;
  let head = .42;
  let previous = performance.now();
  const inputs = Object.fromEntries(["speed", "width", "glow"].map(id => [id, document.getElementById(id)]));
  const progress = document.querySelector("#progress");
  const pause = document.querySelector("#pause");
  const modeButtons = [...document.querySelectorAll("[data-mode]")];

  function chooseMode(next) {
    mode = next;
    modeButtons.forEach((button, index) => button.setAttribute("aria-pressed", String(index === mode)));
  }

  function setPaused(value) {
    paused = value;
    pause.setAttribute("aria-pressed", String(paused));
    pause.textContent = paused ? "继续" : "暂停";
  }

  for (const input of Object.values(inputs)) {
    const output = input.parentElement.querySelector("output");
    const show = () => { output.value = `${Number(input.value).toFixed(2)}×`; };
    input.addEventListener("input", show);
    show();
  }

  modeButtons.forEach((button, index) => button.addEventListener("click", () => chooseMode(index)));
  pause.addEventListener("click", () => {
    if (mode !== 0) chooseMode(0);
    setPaused(!paused);
  });
  document.querySelector("#replay").addEventListener("click", () => {
    head = -.18;
    chooseMode(0);
    setPaused(false);
  });
  document.querySelector("#reset").addEventListener("click", () => {
    yaw = .58;
    pitch = .39;
    zoom = 1;
  });
  canvas.addEventListener("pointerdown", event => canvas.setPointerCapture(event.pointerId));
  canvas.addEventListener("pointermove", event => {
    if (!canvas.hasPointerCapture(event.pointerId)) return;
    yaw -= event.movementX * .009;
    pitch = Math.max(-1.3, Math.min(1.3, pitch + event.movementY * .009));
  });
  canvas.addEventListener("wheel", event => {
    event.preventDefault();
    zoom = Math.max(.58, Math.min(2.2, zoom * Math.exp(-event.deltaY * .001)));
  }, {passive: false});

  function draw(now) {
    const dt = Math.min(.06, (now - previous) / 1000);
    previous = now;
    if (mode === 0 && !paused) {
      head += dt * Number(inputs.speed.value);
      if (head > cycleLength) head = -.18;
    }
    progress.style.transform = `scaleX(${mode === 0 ? Math.max(0, head) / cycleLength : 0})`;

    const dpr = Math.min(2, devicePixelRatio);
    const width = Math.max(1, Math.round(canvas.clientWidth * dpr));
    const height = Math.max(1, Math.round(canvas.clientHeight * dpr));
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
      gl.viewport(0, 0, width, height);
    }
    gl.clearColor(0, 0, 0, 0);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
    gl.uniform4f(uniforms.Camera, Math.sin(yaw), Math.cos(yaw), Math.sin(pitch), Math.cos(pitch));
    gl.uniform2f(uniforms.Scale, .50 * zoom * height / width, .50 * zoom);
    gl.uniform1f(uniforms.Head, head);
    gl.uniform1f(uniforms.Width, Number(inputs.width.value));
    gl.uniform1f(uniforms.Glow, Number(inputs.glow.value));
    gl.uniform1i(uniforms.Mode, mode);
    gl.drawArrays(gl.TRIANGLES, 0, 36);
    requestAnimationFrame(draw);
  }

  requestAnimationFrame(draw);
}

if (typeof module !== "undefined") {
  module.exports = {buildField};
} else {
  startPreview().catch(error => {
    const errorBox = document.querySelector("#error");
    errorBox.textContent = error.message;
    errorBox.hidden = false;
  });
}
