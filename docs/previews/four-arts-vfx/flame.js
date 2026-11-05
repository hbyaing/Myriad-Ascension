import {add,clamp,ease,mix,mul,polar,pulse,turn,wave} from "./math.js";

const copper=[1,.43,.095], vermilion=[.96,.21,.040], gold=[1,.69,.24], ash=[.30,.18,.15];
const reach=6.5, edge=108*Math.PI/360, release=.35;
export const flameTargets=[[3.4,0,-2.6],[4.8,0,.42],[3.35,0,2.95],[1.4,0,3.25]];
export const flameTime=t=>wave(t,4,0);
const hash=x=>((Math.sin(x*127.1+21.7)*43758.5453)%1+1)%1;
const sequence=(n,fn)=>Array.from({length:n},(_,i)=>fn(i/(n-1),i));
const distance=t=>reach*(1-Math.pow(1-clamp((t-release)/.90),1.37));
const arrival=r=>release+.90*(1-Math.pow(1-clamp(r/reach),1/1.37));

function ignition(fx,root,hand,time) {
  const light=pulse(time,.02,.29,.52);
  if(light<.004) return;
  const point=hand??add(root,[.53,1.03,.34]);
  fx.stamp(point,.48,.54,gold,6,light*.94,7,time);
  fx.stamp(point,.61,.32,copper,32,light*.86,13,.2);
  for(let i=0;i<4;i++) {
    const path=sequence(21,u=>{
      const angle=i*turn/4+time*9.1+u*2.9;
      return add(point,polar(.62*Math.pow(1-u,1.38),-.43*(1-u)+Math.sin(u*5.1)*.09,angle));
    });
    fx.ribbon(path,.047,i%2?gold:copper,light*.68,i,14);
  }
  for(let i=0;i<18;i++) {
    const a=i*2.399963+time*6, radius=(.13+hash(i)*.52)*(1-ease(.08,.37,time))+.10;
    const mote=add(point,polar(radius,(hash(i+37)-.55)*radius,a));
    fx.stamp(mote,.065,.046,i%3?copper:gold,33,light*.84,i,a);
  }
}

function rollingCrest(fx,root,time,front) {
  const volume=ease(.36,.57,time)*(1-ease(1.00,1.49,time));
  if(volume<.004) return;
  // Each section rises from the floor, rolls forward and hooks back into its wake.
  const profiles=[
    [0,.025],[-.023,.12],[-.055,.23],[-.10,.35],[-.14,.46],[-.17,.57],[-.17,.66],
    [-.143,.78],[-.08,.87],[-.01,.955],[.075,1],[.172,.982],
    [.26,.93],[.34,.865],[.39,.77],[.40,.68],[.38,.59]
  ];
  for(let layer=0;layer<3;layer++) {
    const outer=layer===0;
    const points=sequence(89,u=>{
      const a=mix(-edge,edge,u);
      const taper=Math.pow(Math.sin(Math.PI*u),.53);
      const rolls=.56+.43*Math.pow(.5+.5*Math.sin(u*32.-time*7.8+layer*.79),1.65);
      const scallops=.83+.17*Math.sin(u*63+Math.sin(u*17)*1.6+time*12+layer*.64);
      const rise=(.62+1.66*rolls*scallops)*taper*ease(.24,2.38,front)*(1-.17*layer);
      const ripples=.13*Math.sin(u*33+time*8)+.07*Math.sin(u*72-time*11);
      const settle=.26*ease(4.40,reach,front);
      const r=clamp(front-settle-layer*(.27+.061*front)+ripples*taper,.03,reach-.035);
      return profiles.map(([shift,h])=>{
        const offset=shift*rise;
        const room=reach-r;
        const curled=offset<=0?r+offset:r+room*(1-Math.exp(-offset/room));
        return add(root,polar(curled,.026+rise*h,a));
      });
    });
    for(let i=1;i<points.length;i++) for(let j=1;j<profiles.length;j++) {
      const left=points[i-1],right=points[i], u=(i-1)/(points.length-1), v=i/(points.length-1);
      const h=(j-1)*2/(profiles.length-1)-1, k=j*2/(profiles.length-1)-1;
      fx.face([left[j-1],left[j],right[j],right[j-1]],[u,u,v,v],
        outer?copper:vermilion,29,volume*(outer?.92:layer===1?.62:.36),layer*9,[h,k,k,h]);
    }
  }
  const sheet=sequence(83,u=>{
    const a=mix(-edge,edge,u);
    const r=Math.max(.06,front+.07*Math.sin(u*50+time*9));
    return [add(root,polar(Math.max(.03,r-1.53),.038,a)),add(root,polar(Math.min(reach,r+.09),.064,a))];
  });
  for(let i=1;i<sheet.length;i++) {
    const [a,b]=sheet[i-1], [c,d]=sheet[i], u=(i-1)/(sheet.length-1), v=i/(sheet.length-1);
    fx.face([a,b,d,c],[u,u,v,v],gold,30,volume*.80,5);
  }
}

function remnants(fx,root,time) {
  for(let i=0;i<96;i++) {
    const a=mix(-edge,edge,hash(i+37));
    const r=mix(.65,6.32,hash(i+90));
    const age=time-arrival(r)-hash(i+46)*.12;
    if(age<0 || age>1.47) continue;
    const seed=hash(i+132);
    const drift=.40+seed*1.22;
    const fade=pulse(age,0,.075,.59+seed*.78);
    const rise=.14+age*(1.10+seed*1.9)-age*age*.74;
    const point=add(root,polar(r+age*drift,rise,a+age*(hash(i+58)-.5)*.24));
    const size=.021+.098*seed*seed;
    if(i%4===0 && age<.56) {
      const trail=sequence(8,u=>add(point,polar(-.18*(1-u),-(1-u)*(.10+.18*seed),a)));
      fx.ribbon(trail,size*.24,copper,fade*.79,i,14);
    }
    fx.stamp(point,size*(1.28+seed),size,gold,33,fade*.93,i,a-.55);
  }
  for(let i=0;i<22;i++) {
    const r=mix(1.17,6.30,hash(i+255));
    const a=mix(-edge*.95,edge*.95,hash(i+382));
    const age=time-arrival(r)-.13;
    if(age<0 || age>1.5) continue;
    const grow=ease(0,1.5,age);
    const point=add(root,polar(r+.29*grow,.15+grow*(.77+hash(i+173)*.48),a+grow*.07));
    fx.stamp(point,.36+grow*.87,.55+grow*.72,ash,31,pulse(age,0,.35,1.5)*.36,i,a*.16);
  }
}

function impacts(fx,root,time) {
  for(const [index,target] of flameTargets.entries()) {
    const [x,,z]=target,r=Math.hypot(x,z),a=Math.atan2(z,x);
    if(r>reach || Math.abs(a)>edge) continue;
    const age=time-arrival(r);
    if(age<0 || age>1.48) continue;
    const point=add(root,target);
    const flash=pulse(age,0,.08,.28);
    fx.stamp(add(point,[-.24,.70,0]),.87,1.07,gold,6,flash*.75,index,0);
    for(let i=0;i<7;i++) {
      const theta=i*2.399963+age*1.46;
      const height=.25+hash(i+index*11)*.65+.30*ease(.06,.63,age);
      const local=add(point,polar(.31+hash(i+25)*.17,height,theta));
      fx.stamp(local,.42+hash(i+39)*.68,.26+hash(i+66)*.14,copper,32,
        pulse(age,.035,.26,1.38)*(.53+.35*hash(i+index*17)),i+index*9,Math.sin(theta)*.31);
    }
  }
}

export function flameWave(fx,root,t,pose) {
  const time=flameTime(t),front=distance(time);
  ignition(fx,root,pose.hand,time);
  if(time>=release && time<=2.98) {
    fx.flameGround(root,front,time-release);
    rollingCrest(fx,root,time,front);
    remnants(fx,root,time);
    impacts(fx,root,time);
  }
  return time<release?"聚火 · 掌心凝焰":time<1.25?"推浪 · 卷焰贴地推进":time<1.85?"命中 · 木桩引燃":time<2.98?"余火 · 烟烬渐散":"等待施术";
}
