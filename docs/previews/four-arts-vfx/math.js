const TAU = Math.PI * 2;
export const clamp = (x, lo = 0, hi = 1) => Math.min(hi, Math.max(lo, x));
export const mix = (a, b, k) => a + (b - a) * k;
export const ease = (a, b, x) => {
  const u = clamp((x - a) / (b - a));
  return u * u * (3 - 2 * u);
};
export const add = (a, b) => a.map((v, i) => v + b[i]);
export const sub = (a, b) => a.map((v, i) => v - b[i]);
export const mul = (a, k) => a.map(v => v * k);
export const dot = (a, b) => a.reduce((sum, v, i) => sum + v * b[i], 0);
export const cross = ([a,b,c], [d,e,f]) => [b*f-c*e,c*d-a*f,a*e-b*d];
export const norm = a => mul(a, 1 / (Math.hypot(...a) || 1));
export const polar = (r, y, a) => [r * Math.cos(a), y, r * Math.sin(a)];
export const wave = (t, period, offset) => ((t - offset) % period + period) % period;
export const pulse = (t, a, b, c) => ease(a,b,t) * (1-ease(b,c,t));
export const turn = TAU;

export function cameraFor(yaw, pitch, half, aspect, target) {
  const back = [Math.sin(yaw)*Math.cos(pitch), Math.sin(pitch), Math.cos(yaw)*Math.cos(pitch)];
  const right = norm(cross([0,1,0],back));
  const up = cross(back,right);
  const sx = 1/(half*aspect), sy = 1/half, sz = -1/21;
  return {
    back, right, up,
    matrix: new Float32Array([
      right[0]*sx, up[0]*sy, back[0]*sz, 0,
      right[1]*sx, up[1]*sy, back[1]*sz, 0,
      right[2]*sx, up[2]*sy, back[2]*sz, 0,
      -dot(right,target)*sx, -dot(up,target)*sy, -dot(back,target)*sz, 1,
    ]),
  };
}
