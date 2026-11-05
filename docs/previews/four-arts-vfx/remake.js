import {add,clamp,ease,mix,mul,norm,polar,pulse,turn,wave} from "./math.js";

const ice=[.32,.76,1], frost=[.67,.91,1];
const ivory=[1,.92,.69], gold=[1,.70,.28], sun=[1,.81,.47], stain=[.28,.086,.30];
const sequence=(count,fn)=>Array.from({length:count},(_,i)=>fn(i/(count-1)));
export const iceTargets=[[7.52,0,.14],[7.0,0,1.75]];
const volley=[
  [0,.14,1.55,0,3.74,.425,.80,1.20],
  [.070,-.61,2.48,.64,2.50,.282,-1.60,1.88],
  [.112,.54,2.09,.08,2.16,.232,1.61,1.75],
  [.031,-.64,.70,.54,2.42,.259,-2.66,.84],
  [.134,.72,.93,.81,2.07,.226,2.42,.78],
  [.106,-.24,3.26,1.14,1.83,.188,-.46,2.19],
  [.183,.77,.27,1.22,1.59,.171,3.02,1.58],
];
const iceLaunch=.62;
const iceImpact=iceLaunch+.66*(iceTargets[0][0]-.88)/7.25;

function iceAnchor(bearing,level,time) {
  const settle=ease(.04,.57,time);
  return polar(1.36-.19*settle,level+.063*Math.sin(time*6.5+bearing),bearing+.39*settle);
}

function iceFlight(raw,side,height,lag,anchor) {
  const advance=clamp(raw);
  const bend=ease(.08,.46,raw);
  const bypass=anchor[0]<0 && Math.abs(anchor[2])<.72
    ? (Math.sign(anchor[2]||side)*.81-anchor[2])*ease(0,.12,raw)*(1-ease(.28,.46,raw)) : 0;
  return [Math.min(8,mix(anchor[0],.88,ease(0,.28,raw))+(7.25-lag)*advance),
    mix(anchor[1],height,bend),mix(anchor[2],side,bend)+bypass];
}

export function iceSpike(fx,root,t) {
  const time=wave(t,3.9,0);
  if(time>iceLaunch && time<2.64) {
    const lead=volley[0];
    const front=Math.min(iceTargets[0][0],iceFlight((time-iceLaunch)/.66,lead[1],lead[2],lead[3],
      iceAnchor(lead[6],lead[7],iceLaunch))[0]);
    fx.iceGround(root,front,time-iceLaunch,iceTargets[0],iceImpact-iceLaunch);
  }
  const gather=pulse(time,.025,.33,.74);
  if(gather>.004) for(let i=0;i<7;i++) {
    const angle=i*2.399963+time*1.6;
    const radius=1.47-.20*ease(.05,.59,time);
    fx.ribbon(sequence(21,u=>add(root,polar(radius-.22*u,.37+(i%5)*.32+.38*u,angle+u*1.21))),
      .062,ice,gather*.57,i,14);
    const point=add(root,polar(radius,.40+(i%4)*.35+time*.30,angle+.41));
    fx.stamp(point,.11+(i%3)*.021,.11+(i%3)*.021,frost,28,gather*.74,i,angle*.48);
  }
  for(const [i,[delay,side,height,back,length,width,bearing,level]] of volley.entries()) {
    const launch=iceLaunch+delay;
    const raw=(time-launch)/.66;
    if(raw<-.96 || raw>1.13) continue;
    const travel=clamp(raw);
    const appear=ease(-.88,-.17,raw);
    const visibility=appear*(1-ease(.88,1.11,raw));
    const anchor=iceAnchor(bearing,level,Math.min(launch,time));
    const [x,y,z]=iceFlight(raw,side,height,back,anchor);
    const contact=i===0?1-ease(iceImpact,iceImpact+.083,time):1;
    const leading=i===0?Math.min(x,iceTargets[0][0]):x;
    const tip=add(root,[leading,y,z]);
    const extent=(.22+.78*ease(.11,.43,raw))*length;
    const clearance=ease(.47,.90,Math.abs(z));
    const size=raw>0 && leading>.36
      ? Math.min(extent,mix(Math.max(.24,leading-.66),extent,clearance)) : extent;
    const turnIn=1-ease(-.50,.20,raw);
    const axis=norm([1,.022*(height-1)+.13*turnIn,side*.046+Math.sin(bearing)*.33*turnIn]);
    fx.crystal(tip,axis,size,width*(.36+.64*ease(.02,.32,raw)),visibility*contact,i);
    if(raw<=.015) {
      const condensate=pulse(raw,-.77,-.24,.03);
      fx.stamp(add(root,anchor),.48,.72,ice,27,condensate*.42,i+91,bearing*.12);
      continue;
    }
    const rear=x-size*.89;
    const lengthFade=ease(.015,.33,raw);
    const rearPhase=clamp((rear-.88)/(7.25-back));
    const tailPhase=Math.max(0,rearPhase-.53);
    const trailPoint=u=>{
      const phase=mix(tailPhase,rearPhase,u);
      const pos=iceFlight(phase,side,height,back,anchor);
      return [mix(pos[0],rear+.14,ease(.74,1,u)),
        mix(pos[1],y,ease(.38,1,u)),mix(pos[2],z,ease(.38,1,u))];
    };
    for(let j=0;j<3;j++) {
      const shift=(j-1)*width*.77;
      fx.ribbon(sequence(25,u=>{
        const point=trailPoint(u);
        return add(root,add(point,[0,shift*(.22+.78*u)+.048*Math.sin(u*10+i+j),shift*.28]));
      }),width*(j===1?.66:.33),j===1?frost:ice,visibility*lengthFade*(j===1?.91:.70),i*4+j,14);
    }
    for(let j=0;j<9;j++) {
      const fraction=(j+.43)/9;
      const seed=i*13+j;
      const radius=.08+.26*fraction;
      const point=add(root,add(trailPoint(1-fraction),[0,Math.sin(seed*2.1)*radius,Math.cos(seed*2.3)*radius]));
      const chip=.026+.083*Math.pow(.5+.5*Math.cos(seed*2.29),3);
      fx.stamp(point,chip,chip,frost,18,visibility*lengthFade*(1-fraction*.54),seed,seed*.27+time*.9);
    }
    const cloudStrength=visibility*lengthFade;
    for(let j=0;j<6;j++) {
      const along=(j+.37)/6;
      const roll=i*2.399963+j*1.71+time*2.8;
      const drift=.078+.19*(1-along);
      const wave=[0,Math.sin(roll)*drift,Math.cos(roll)*drift];
      const center=add(root,add(trailPoint(along),add(wave,[.16+.30*along,0,-.072])));
      const height=(.39+.28*(1-along)+travel*.25)*(i===0?1.29:.82);
      const length=(1.40+1.48*(1-along)+travel*.50)*(i===0?1.15:.84);
      const launchFade=ease(.06,.74,along);
      const pitch=.16*Math.sin(roll)+.18*(height-1);
      fx.stamp(center,height,length,ice,27,cloudStrength*(i===0?.73:.61)*(.35+.65*launchFade),i*7+j,pitch);
    }
    const shoulder=add(root,[rear+.55,y-.11,z-.19]);
    fx.stamp(shoulder,.40+width*.77,1.28+width*2.9,ice,27,cloudStrength*.57,i+64,.06);
    for(let j=0;j<14;j++) {
      const along=(j+.45)/14;
      const key=i*17+j;
      const swirl=key*2.399963+time*(2.0+(j%4)*.41);
      const radius=.19+.49*(1-along)+.17*travel;
      const center=add(root,add(trailPoint(along),[Math.sin(key*1.67)*.31,
        Math.cos(swirl)*radius,Math.sin(swirl)*radius]));
      const shape=.055+.178*Math.pow(.5+.5*Math.sin(key*3.7),3);
      const shimmer=.76+.24*Math.sin(key*.71+time*8.7);
      fx.stamp(center,shape,shape,frost,28,cloudStrength*shimmer*(.80+.20*along),key,swirl*.56);
    }
  }
  const impactAge=time-iceImpact;
  const impact=pulse(impactAge,0,.09,.84);
  if(impact>.004) {
    const point=add(root,iceTargets[0]);
    const grow=ease(0,.25,impactAge);
    fx.plane(add(point,[0,.043,0]),[.49+.85*grow,0,0],[0,0,.34+.60*grow],ice,24,impact*.91,7);
    fx.stamp(add(point,[0,1.27,0]),1.14,1.22,frost,6,pulse(impactAge,0,.043,.27)*.88,0,0);
    fx.stamp(add(point,[.15,.98+.38*grow,0]),.89+grow*.61,1.44+grow*.61,ice,26,impact*.50,8,-.19);
    for(let i=0;i<9;i++) {
      const a=i*2.399963;
      const gas=add(point,polar((.24+(i%3)*.19)*grow,.25+(i%5)*.18+.39*grow,a));
      fx.stamp(gas,.60+.59*grow,1.02+.53*grow,ice,27,impact*.64,i+54,a*.13);
    }
    for(let i=0;i<21;i++) {
      const angle=i*2.399963;
      const scatter=1-Math.pow(1-clamp(impactAge/.64),3);
      const radial=(.30+(i%7)*.109)*scatter;
      const y=1.18+(i%5)*.083+Math.sin(Math.PI*scatter)*(.26+(i%6)*.105)-.68*scatter*scatter;
      const axis=norm(polar(.76,.53*Math.cos(scatter*2.7+i*.41),angle+scatter*.86));
      const tip=add(add(point,[.47*scatter,y,0]),polar(radial,0,angle));
      fx.crystal(tip,axis,.11+(i%4)*.083,.029+(i%3)*.021,impact*.89,i,true);
      const flake=add(point,polar(radial*1.53,.43+y*.71,angle+.41));
      const flakeSize=.066+(i%6)*.022;
      fx.stamp(flake,flakeSize,flakeSize,frost,28,impact*.87,i+121,angle+scatter*.88);
    }
  }
  return time<iceLaunch?"凝聚 · 冰锥环身成形":time<iceImpact?"齐射 · 冰锥从周身发出":time<iceImpact+.84?"命中 · 冰体碎裂，霜纹铺开":"等待施术";
}


function lotus(fx,root,radius,angle,strength) {
  fx.plane(add(root,[0,.029,0]),polar(radius,0,angle),polar(radius,0,angle+Math.PI*.5),sun,17,strength,0);
}

function spiral(fx,root,time,strength) {
  if(strength<=.004) return;
  const rise=ease(.18,.78,time);
  const end=.13+.87*rise;
  const span=1.83*turn;
  const phase=-1.37+time*1.06;
  function point(u,y) {
    const radius=1.11+.25*Math.sin(Math.PI*u)-.16*u;
    return add(root,polar(radius,y+.13+u*3.40,phase+u*span));
  }
  for(let i=0;i<108;i++) {
    const u=i/108, v=(i+1)/108;
    const half=.086+.022*Math.sin(Math.PI*u);
    fx.face([point(u*end,-half),point(u*end,half),point(v*end,half),point(v*end,-half)],
      [u,u,v,v],ivory,21,strength,0);
  }
}

function lightPillars(fx,root,time,strength) {
  const height=3.85*(.39+.61*ease(.05,.54,time));
  for(let i=0;i<24;i++) {
    const angle=i*2.399963;
    const radius=.22+.90*((i*.381966)%1);
    const center=add(root,polar(radius,height*.5,angle));
    const width=.105+.091*((i*.627)%1);
    fx.plane(center,[0,height*(.46+.05*Math.sin(i)),0],
      polar(width,0,angle+Math.PI*.5),i%3===0?gold:sun,20,strength*(i%4===0?.76:.52),angle);
  }
}

function purgedStains(fx,root,time) {
  for(let i=0;i<6;i++) {
    const start=.29+i*.075;
    const age=(time-start)/.95;
    if(age<=0 || age>=1) continue;
    const alpha=pulse(age,0,.31,.87);
    const peel=ease(0,.74,age);
    const angle=i*turn/6+age*.56;
    const base=.25+(i%3)*.36;
    const path=u=>{
      const curl=Math.sin(Math.PI*u);
      const radial=.25+(.68+.26*curl)*peel*u;
      const y=base+.79*u+.59*peel+.15*curl;
      return add(root,polar(radial,y,angle+u*1.10));
    };
    fx.ribbon(sequence(27,path),.15+(i%3)*.034,stain,alpha*.90,i,16);
    for(let j=0;j<10;j++) {
      const u=(j+.5)/10;
      const ash=add(path(u),polar(.042+.16*age,.13*Math.sin(i*12.7+j*2.1),i*2.2+j*2.399963));
      const size=.024+.039*(.5+.5*Math.sin(j*2.36+i));
      fx.stamp(ash,size,size,[.36,.15,.34],23,alpha*(.71+.23*Math.sin(j+i)),j+i*10,j*.6);
    }
  }
}

export function purification(fx,root,t,passive) {
  const time=passive?wave(t,2.68,.37):wave(t,3.83,0);
  if(passive) {
    const strength=pulse(time,.025,.19,.88);
    if(strength<=.004) return "等待恢复事件";
    const swell=ease(.02,.70,time);
    fx.heal(3,0,root[1],strength*.50,ivory);
    fx.groundLight(root,1.10,strength*.24);
    lotus(fx,root,.91+.25*swell,.22+swell*.13,strength*.58);
    fx.stamp(add(root,[.10,1.26+.12*swell,.35]),.44,.42,ivory,15,strength*.88,0);
    for(let i=0;i<5;i++) {
      const p=add(root,polar(.41+.24*swell,.48+i*.21+.52*swell,i*2.399963+time*1.2));
      fx.stamp(p,.087,.055,sun,22,strength*.69,i,i*1.17);
    }
    return "回元 · 莲光初绽";
  }
  if(time>2.04) return "等待施术";
  const cleared=t<3.83;
  const cast=pulse(time,.025,.59,1.97);
  const brightness=cleared?1:.86;
  const waveFront=ease(.02,.49,time);
  const ward=pulse(time,.17,.58,1.57);
  const halo=pulse(time,.09,.61,1.79);
  const bloom=pulse(time,.25,.66,1.40);
  fx.heal(3,0,root[1],ward*.68,ivory);
  fx.groundLight(root,2.22,cast*.69*brightness);
  lotus(fx,root,.56+1.52*waveFront,.07+time*.17,cast*brightness);
  lightPillars(fx,root,time,ward*brightness);
  spiral(fx,root,time,halo*brightness);
  fx.stamp(add(root,[.13,1.30,.37]),.61,.56,ivory,15,bloom*brightness,0);
  for(let i=0;i<11;i++) {
    const delay=.17+(i%5)*.088;
    const age=(time-delay)/(1.12+(i%3)*.12);
    if(age<=0 || age>=1) continue;
    const phase=i*2.399963+age*1.86;
    const size=.14+(i%5)*.023;
    const p=add(root,polar(1.08+age*.54+(i%3)*.12,.30+(i%5)*.36+age*1.68,phase));
    fx.stamp(p,size*1.48,size*.78,i%3===0?ivory:sun,22,pulse(age,0,.29,1)*.95,i,phase+.38*Math.sin(age*9+i));
  }
  for(let i=0;i<36;i++) {
    const age=(time-.11-(i%12)*.061)/1.24;
    if(age<=0 || age>=1) continue;
    const angle=i*2.399963+age*.91;
    const point=add(root,polar(.34+(i%7)*.18+age*.24,.17+(i%11)*.179+age*1.13,angle));
    const size=.021+(i%6)*.009;
    fx.stamp(point,size,size,i%4===0?gold:ivory,18,pulse(age,0,.26,1)*.94,i,Math.sin(i)*.40);
  }
  if(cleared) purgedStains(fx,root,time);
  return time<.31?"起势 · 莲阵展开":time<1.29?(cleared?"净化 · 符光升腾，浊气剥离":"祈复 · 符光升腾"):"收势 · 金光入莲";
}
