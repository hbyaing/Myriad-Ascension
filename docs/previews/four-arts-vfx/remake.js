import {add,clamp,cross,ease,mix,mul,norm,polar,pulse,turn,wave} from "./math.js";

const up=[0,1,0], forward=[1,0,0];
const ice=[.32,.76,1], frost=[.67,.91,1];
const jade=[.70,.98,.88], ivory=[.97,.94,.79], gold=[1,.76,.39];
const sequence=(count,fn)=>Array.from({length:count},(_,i)=>fn(i/(count-1)));
export const iceTargets=[[7.52,0,.14],[7.0,0,1.75]];
const volley=[
  [0,.17,1.55,0,3.37,.39],
  [.06,-.67,2.36,.67,2.27,.275],
  [.09,.56,2.32,.04,2.08,.248],
  [.035,-.64,.67,.50,2.28,.260],
  [.10,.82,.76,.77,2.03,.235],
  [.13,-.24,3.45,1.15,1.89,.213],
  [.20,.84,.28,1.27,1.61,.199],
];

export function iceSpike(fx,root,t) {
  const time=wave(t,3.9,0);
  const seal=pulse(time,.015,.24,.94);
  for(let i=0;i<4;i++) {
    const angle=i*turn/4+time*1.25;
    fx.ribbon(sequence(16,u=>add(root,[.70,.99+.48*Math.cos(angle+u*1.32),.48*Math.sin(angle+u*1.32)])),
      .039,frost,seal*.79,i,11);
  }
  for(const [i,[delay,side,height,back,length,width]] of volley.entries()) {
    const launch=.37+delay;
    const raw=(time-launch)/.66;
    if(raw<-.37 || raw>1.13) continue;
    const travel=clamp(raw);
    const appear=ease(-.37,-.04,raw);
    const visibility=appear*(1-ease(.88,1.11,raw));
    const x=Math.max(.51,Math.min(8, .88+(7.25-back)*travel));
    const y=mix(.98,height,ease(-.25,.49,raw));
    const z=side*ease(-.22,.39,raw);
    const tip=add(root,[x,y,z]);
    const size=(.23+.77*ease(-.27,.31,raw))*length;
    const axis=norm([1,.022*(height-1),side*.046]);
    fx.crystal(tip,axis,size,width*(.39+.61*appear),visibility,i);
    if(raw<=.015) continue;
    const rear=Math.max(.24,x-size*.89);
    const tail=Math.max(.50,rear-(1.25+2.67*travel));
    const lengthFade=ease(.015,.33,raw);
    for(let j=0;j<3;j++) {
      const shift=(j-1)*width*.77;
      const tint=j===1?frost:ice;
      fx.ribbon(sequence(20,u=>add(root,[mix(tail,rear+.17,u),
        mix(.99,y,u)+shift*(.33+.67*u)+.025*Math.sin(u*7+i),
        mix(0,z,u)+shift*.27])),width*(j===1?.88:.34),tint,visibility*lengthFade*(j===1?.94:.64),i*4+j,14);
    }
    for(let j=0;j<7;j++) {
      const fraction=(j+.38)/7;
      const seed=i*13+j;
      const radius=.08+.18*fraction;
      const point=add(root,[mix(rear,tail,fraction),mix(y,.99,fraction)+Math.sin(seed*2.1)*radius,
        mix(z,0,fraction)+Math.cos(seed*2.3)*radius]);
      const chip=.028+.070*Math.pow(.5+.5*Math.cos(seed*2.29),2);
      fx.stamp(point,chip,chip,frost,18,visibility*lengthFade*(1-fraction*.6),seed,seed*.27+time*.9);
    }
    const ground=add(root,[Math.max(.32,x-size*.48),.027,z*.84]);
    fx.plane(ground,[Math.min(2.3,x*.45),0,0],[0,0,.26+width*.38],ice,19,visibility*lengthFade*.52,i);
  }
  const impactAge=time-.94;
  const impact=pulse(impactAge,0,.10,.78);
  if(impact>.004) {
    const point=add(root,iceTargets[0]);
    fx.plane(add(point,[0,.043,0]),[1.02,0,0],[0,0,.75],ice,19,impact*.89,7);
    for(let i=0;i<17;i++) {
      const a=i*2.399963, fall=ease(0,.70,impactAge);
      const axis=norm(polar(.73,.40,a));
      const tip=add(point,polar(.19+(.28+(i%5)*.085)*fall,.43+(i%6)*.135+Math.sin(fall*Math.PI)*.57,a));
      fx.crystal(tip,axis,.19+(i%4)*.083,.034+(i%3)*.018,impact*.82,i);
    }
  }
  return time<.36?"起诀 · 冰锥凝结":time<1.10?"齐射 · 七枚冰锥穿行":time<1.73?"命中 · 碎冰与霜斑":"等待施术";
}

function talisman(fx,point,angle,tilt,halfHeight,halfWidth,color,strength,seed) {
  const along=norm(add(up,polar(tilt,0,angle)));
  const across=norm(cross(along,polar(1,0,angle)));
  fx.plane(point,mul(along,halfHeight),mul(across,halfWidth),color,15,strength,seed);
}

export function purification(fx,root,t,passive) {
  const time=passive?wave(t,2.68,.19):wave(t,3.77,0);
  if(passive) {
    const strength=pulse(time,.025,.21,.70);
    if(strength<=.004) return "等待恢复事件";
    const upstroke=ease(.02,.64,time);
    fx.heal(3,0,root[1],strength*.51,ivory);
    for(const side of [-1,1]) {
      const a=side*1.45;
      talisman(fx,add(root,[side*(.44+.10*upstroke),1.10+.48*upstroke,.15]),a,
        side*.22,.18,.072,jade,strength*.66,side);
    }
    return "回元 · 双印轻明";
  }
  if(time>1.78) return "等待施术";
  const cleared=t<3.77;
  const rise=ease(.02,.38,time), fold=ease(.96,1.50,time);
  const orbit=pulse(time,.03,.34,1.45);
  const flash=pulse(time,.46,.72,1.13);
  const ward=pulse(time,.37,.68,1.47);
  fx.heal(3,0,root[1],ward*.55,ivory);
  for(let i=0;i<4;i++) {
    const angle=.55+i*turn/4+rise*.54+fold*.21;
    const radius=mix(.38,.83,rise)*(1-.24*fold);
    const y=mix(.57,1.26,rise)-fold*.45+(i%2)*.23;
    const seal=add(root,polar(radius,y,angle));
    talisman(fx,seal,angle+(i%2===0?.39:-.42),.42*(1-rise)-.45*fold,.30+.087*rise,.145+.038*rise,
      i%2===0?ivory:jade,orbit*.79+flash*.21,i);
    const bond=pulse(time,.17,.46,.95);
    fx.ribbon(sequence(17,u=>{
      const corner=Math.sin(Math.PI*u);
      return add(root,polar(radius+.10*corner,y-.34-.17*corner,angle+u*1.32));
    }),.021,gold,bond*.56,i,11);
    if(!cleared) continue;
    for(let band=0;band<2;band++) {
      const timeLag=i*.039+band*.10;
      const peel=ease(.22+timeLag,.76+timeLag,time);
      const intensity=pulse(time,.19+timeLag,.52+timeLag,.94+timeLag);
      const pull=angle-.59*(1-peel);
      fx.ribbon(sequence(25,u=>{
        const span=Math.sin(Math.PI*u);
        const r=mix(.25,radius+.10*span,u*peel);
        const a=pull+(.79-.43*peel)*u;
        const h=mix(.42+(i%3)*.32+band*.51,y,u)+.22*span;
        return add(root,polar(r,h,a));
      }),.14+.057*band,[.24,.16,.25],intensity,i*3+band,16);
    }
    for(let j=0;j<5;j++) {
      const age=(time-.58-i*.037-j*.031)/.64;
      if(age<=0 || age>=1) continue;
      const f=pulse(age,0,.19,1);
      const offset=polar(age*(.26+j*.081),.16*age-.36*age*age,angle+j*.91);
      const point=add(seal,offset);
      const size=.032+(j%3)*.023;
      fx.stamp(point,size,size,j%3===0?gold:ivory,18,f*.90,i*6+j,angle+age*1.8);
    }
  }
  const ground=pulse(time,.84,1.10,1.74);
  const scale=.53+.70*ease(.84,1.58,time);
  const a=.23+fold*.31;
  fx.plane(add(root,[0,.038,0]),polar(scale,0,a),polar(scale,0,a+Math.PI/2),gold,17,ground*.83,0);
  return time<.38?"起诀 · 四方立印":time<1.02?(cleared?"剥浊 · 污痕离身":"合印 · 玉光入体"):"收势 · 地印散去";
}
