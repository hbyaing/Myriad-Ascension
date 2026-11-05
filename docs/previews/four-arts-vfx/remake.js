import {add,clamp,ease,mix,mul,norm,polar,pulse,turn,wave} from "./math.js";

const forward=[1,0,0];
const ice=[.32,.76,1], frost=[.67,.91,1];
const ivory=[1,.92,.69], gold=[1,.70,.28], sun=[1,.81,.47], stain=[.28,.086,.30];
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
