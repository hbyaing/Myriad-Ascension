import {add, clamp, cross, ease, mix, mul, norm, polar, pulse, turn, wave} from "./math.js";
import {weaponTip} from "./render.js";
import {iceSpike,iceTargets,purification} from "./remake.js";

const wind = [.53,.93,1], paleWind = [.82,1,.97];
const leafColors = [[.286,.616,.212],[.471,.769,.302],[.620,.835,.420],[.224,.545,.294]];
const up = [0,1,0];
const spiritBlue = [.13,.48,1], spiritIce = [.49,.83,1];
const carmine = [1,.10,.22], ruby = [.69,.045,.17];
const recoveryCyan = [86/255,238/255,219/255];
const sequence = (count,fn) => Array.from({length:count},(_,i) => fn(i/(count-1),i));
const ember=[1,.43,.10], coal=[.83,.17,.065];
const attacks={
  6:{range:6.5,angle:108*Math.PI/360,targets:[[3.4,0,-2.6],[4.8,0,.42],[3.35,0,2.95],[1.4,0,3.25]]},
};
const attackTime=t=>wave(t,3.6,0);
const flameDistance=t=>6.5*ease(.15,.83,t);

export function attackTargets(art) { return art===5?iceTargets:attacks[art]?.targets??[]; }
function inAttack(art,[x,,z]) {
  const attack=attacks[art];
  return Math.hypot(x,z)<=attack.range && Math.abs(Math.atan2(z,x))<=attack.angle;
}

function windRoot(t, passive) {
  const distance = passive ? 2.75 : 3.50;
  const x = -1.57+distance*ease(.45,4.85,t);
  const z = .19*Math.sin(ease(.45,4.85,t)*turn);
  const falling = passive ? .76*pulse(t,4.78,5.38,7.08) : 0;
  return [x,falling,z];
}
function breathRoot(t) {
  return [.61*ease(2.85,4.85,t),0,.17*Math.sin(ease(2.85,4.85,t)*Math.PI)];
}
const attackTimes = [2.18,4.08,6.02];
export function performance(art,passive,t) {
  if(art>=5) {
    const time=art===5?wave(t,3.9,0)-.21:attackTime(t);
    return {root:[-3.6,0,0],speed:0,stride:0,yaw:Math.PI/2,
      guard:pulse(time,0,.15,.40),hit:pulse(time,.07,.24,.58)};
  }
  const root = art===0 ? windRoot(t,passive) : art===2 ? breathRoot(t) : [0,0,0];
  const speed = art===0 ? Math.abs(windRoot(t+.03,passive)[0]-root[0])/.03 :
    art===2 ? Math.abs(breathRoot(t+.03)[0]-root[0])/.03 : 0;
  const hit = art===3 ? Math.max(...attackTimes.map(at => pulse(t,at-.34,at,at+.37))) : 0;
  const ward=art===4?pulse(wave(t,passive?2.68:3.83,passive?.37:0),.04,.59,1.43):0;
  const guard = art===3 ? hit : art===1 ? (passive ? .14 : pulse(t,0,.48,1.25)) : art===2 ? .38 : ward;
  return {
    root, speed, hit, guard,
    stride: clamp(speed)*.69*Math.sin(t*(art===0?11:9)) + (art===0 && passive ? root[1]*.38 : 0),
    yaw: art===0 ? .59+ease(.3,.9,t)*.76-ease(4.3,5.1,t)*.62 : art===3 ? .23+hit*.36 : .24,
  };
}

function lightBody(fx,state,t,passive) {
  const root = state.root;
  const launch = passive ? 0 : pulse(t,0,.23,.78);
  const air = passive ? root[1] : 0;
  const fade = passive ? 1 : ease(.05,.28,t)*(1-ease(6.55,7.32,t));
  const moving = ease(.04,.5,state.speed);
  if(launch>.01) fx.ground([root[0],.012,root[2]],.91,wind,launch*.68,2);
  if (launch>.01 || (air<.05 && moving<.22 && !passive)) {
    for(let strand=0;strand<2;strand++) {
      fx.ribbon(sequence(34,u=>add(root,polar(.48+.1*strand,.085+u*.14,
        (u*.92+t*.85+strand*.53)*turn))),.045,wind,fade*(.15+launch*.75),strand);
    }
  }
  if (moving>.02) {
    for(let side of [-1,1]) for(let strand=0;strand<(passive?2:4);strand++) {
      const lifetime = (passive?.53:.83)+strand*.076;
      const points = sequence(26,u=>{
        const ago = (1-u)*lifetime;
        const stamp = t-ago;
        const broad = Math.sin(ago*2.5+side*.65);
        return add(windRoot(stamp,passive),[
          -.21-ago*(passive?.56:1.10),
          .15+strand*.098+ago*.32+.10*Math.sin(stamp*8.5+side+strand),
          side*(.16+ago*.32+strand*.085)+broad*.19+Math.sin(stamp*7+strand)*.13,
        ]);
      });
      const gauze = strand < (passive?1:2);
      const width = gauze ? (passive?.075:.173)/(1+strand*.21) : .028;
      fx.ribbon(points,width,strand===0?paleWind:wind,
        fade*moving*(passive?.48:.90)/(1+strand*.16),side+strand*3,gauze?5:0);
    }
    if(!passive) for(let i=0;i<13;i++) {
      const age=wave(t,1.06,i*.087);
      const p=add(windRoot(t-age,false),[-.18-age*.96,.14+age*.33+(i%4)*.08,Math.sin(i*2.4)*(.21+age*.37)]);
      fx.stamp(p,.036+age*.025,.060+age*.018,paleWind,3,fade*moving*(1-age/1.06)*.56,i,-.9);
    }
  }
  if (air>.04) {
    const lift=ease(.04,.48,air);
    for(let layer=0;layer<3;layer++) {
      const depth=.07+layer*.135;
      const height=Math.max(.026,air-depth);
      const radius=.46+layer*.116;
      const angle=.35+t*1.15+layer*2.15;
      const points=sequence(40,u=>{
        const swell=Math.sin(u*Math.PI);
        return add([root[0],height,root[2]],polar(radius*(.81+.19*swell),
          .087*swell-.03*u,angle+u*(turn*.71-layer*.15)));
      });
      fx.ribbon(points,.127-layer*.025,layer===0?paleWind:wind,
        lift*(.83-layer*.13)*ease(.025,.26,air-depth+.08),layer,8);
    }
    for(let i=0;i<7;i++) {
      const phase=wave(t,.94,i*.14)/.94;
      const angle=i*2.39996+t*.64;
      const p=add([root[0],Math.max(.02,air-.31+phase*.29),root[2]],
        polar(.64-phase*.24,0,angle));
      fx.stamp(p,.043,.073,paleWind,3,lift*Math.sin(phase*Math.PI)*.45,i,angle);
    }
  }
  return passive && air>.06 ? "缓降 · 气流托足" : moving>.14 ? "移动 · 历史风尾" :
    launch>.05 ? "起术 · 脚踝卷风" : fade>.04 && !passive ? "静止 · 脚边余风" : "待机";
}

function renewal(fx,root,t,passive) {
  const time=passive?wave(t,2.45,.62):t;
  const lifetime=passive?.93:1.56;
  if(time>=lifetime) return passive?"等待恢复事件":"叶影已收";
  const age=time/.78;
  if(age<(passive?1:2)) {
    const pass=age%1;
    const glow=ease(0,.08,pass)*(1-ease(.84,1,pass))*(passive?.73:age<1?.91:.68);
    fx.heal(0,-.09+pass*1.25,root[1],glow,recoveryCyan);
  }

  const leaves=passive?9:21;
  const span=passive?.77:1;
  for(let i=0;i<leaves;i++) {
    const age=(time-.014*(i%7))/lifetime;
    if(age<=0 || age>=1) continue;
    const alpha=ease(0,.16,age)*(1-ease(.68,1,age))*(passive?.77:.97);
    const angle=i*2.399963+age*(3.15+(i%4)*.26);
    const radius=(.48+(i%6)*.075+.24*Math.sin(age*Math.PI))*span;
    const height=(.12+(i%8)*.086+.30*age+.047*Math.sin(angle*2+i))*1.8;
    const position=add(root,polar(radius,height,angle));
    const heading=angle+.69+Math.sin(age*8+i)*.48;
    const tilt=.76*Math.sin(age*9.4+i*1.75);
    const along=add(mul(up,Math.cos(tilt)),polar(Math.sin(tilt),0,heading));
    const transverse=polar(1,0,heading+Math.PI*.5);
    const roll=.91*Math.sin(age*13.7+i*.83);
    const across=add(mul(transverse,Math.cos(roll)),mul(cross(along,transverse),Math.sin(roll)));
    const size=(.099+(i%5)*.016)*span;
    fx.plane(position,mul(along,size),mul(across,size*.60),leafColors[i%leafColors.length],1,alpha,i);
  }
  return "回血 · 青光上行";
}

function flameWave(fx,root,t) {
  const time=attackTime(t);
  const radius=flameDistance(time);
  const flame=pulse(time,.10,.37,1.03);
  const edge=attacks[6].angle;
  const ignition=pulse(time,0,.12,.24);
  fx.stamp(add(root,[.48,.85,0]),.46,.52,ember,6,ignition*.86,2);
  for(let layer=0;layer<2;layer++) {
    const r=Math.max(.18,radius-layer*.31);
    const crest=sequence(43,u=>{
      const angle=mix(-edge,edge,u);
      const push=r*(.986+.014*Math.sin(17*angle+time*9));
      const taper=.52+.48*Math.sin(u*Math.PI);
      const y=(.65+.54*Math.pow(.5+.5*Math.sin(u*38+time*9+layer),2))*taper;
      return [add(root,polar(push,.033,angle)),add(root,polar(push-.15,y,angle))];
    });
    for(let i=1;i<crest.length;i++){
      const [a,b]=crest[i-1], [c,d]=crest[i];
      const u=(i-1)/(crest.length-1), v=i/(crest.length-1);
      fx.face([a,b,d,c],[u,u,v,v],layer===0?ember:coal,12,flame*(layer===0?.97:.76),layer*5);
    }
    fx.ribbon(sequence(43,u=>add(root,polar(r,.045,mix(-edge,edge,u)))),
      .065,layer===0?ember:coal,flame*.87,layer,11);
  }
  for(let i=0;i<24;i++) {
    const age=clamp((time-.16-(i%6)*.042)/.85);
    const angle=mix(-edge,edge,(i*0.618034)%1);
    const r=Math.max(.06,radius-age*(.48+(i%4)*.33));
    const point=add(root,polar(r,.12+age*(.48+(i%7)*.16),angle));
    fx.stamp(point,.12,.073,ember,7,pulse(age,.01,.23,1)*flame,i,angle);
  }
  for(const target of attackTargets(6)) {
    if(!inAttack(6,target) || radius<Math.hypot(target[0],target[2])) continue;
    const burn=ease(.34,.47,time)*(1-ease(.95,1.87,time));
    const point=add(root,target);
    for(let i=0;i<3;i++) {
      const angle=i*turn/3+time*2;
      fx.stamp(add(point,polar(.27,.49+i*.21,angle)),.48,.34,i===1?ember:coal,6,burn*.68,i,angle*.16);
    }
  }
  return time<.15?"起火 · 火核压低":time<.86?"推浪 · 108° 扇面":time<1.87?"命中引燃 · 余烬散去":"等待施术";
}

function gathering(fx,root,t,passive) {
  const event = passive ? wave(t,2.4,.80) : t;
  const strength = passive ? pulse(event,0,.32,.92) : ease(.07,.66,t)*(1-ease(6.74,7.58,t));
  if (strength<.004) return passive ? "等待补灵事件" : t<.5 ? "起势 · 引气" : "收势 · 灵核归体";
  const strands = passive ? 2 : 7;
  const close = passive ? ease(.27,.94,event) : ease(6.65,7.51,t);
  if(!passive) fx.ground([root[0],.014,root[2]],1.47,spiritBlue,strength*(1-close)*.61,4);
  for(let i=0;i<strands;i++) {
    const seed = i*turn/strands;
    const phase = wave(event,passive?1.3:1.65,i*.11)/(passive?1.3:1.65);
    function path(u) {
      const r = mix(passive?.89:1.60,.10,u)*(1-.87*close);
      const height = .29+(i%3)*.41+Math.sin(seed+t*.82)*.16;
      const y = mix(height,.99,u)+Math.sin(Math.PI*u)*(.22+(i%2)*.19);
      const a = seed+(passive?.49:.79)*t+u*(1.47+(i%3)*.22);
      return add(root,polar(r,y,a));
    }
    fx.ribbon(sequence(27,path),passive?.038:i%2===0?.084:.043,i%3===0?spiritIce:spiritBlue,
      strength*(passive?.59:.68),i,(!passive&&i%2===0)?5:0);
    const u = phase*phase*.40+phase*.60;
    for(let k=0;k<(passive?3:6);k++) {
      const v = u-k*.051;
      if(v<0) continue;
      fx.stamp(path(v),.082+u*.045,.079+u*.035,spiritIce,3,strength*(1-k*.13),i);
    }
    if(!passive && u>.16) fx.ribbon(sequence(13,v=>path(u-.16+v*.16)),
      .053,spiritIce,strength*.79,i);
  }
  const core = add(root,[0,.995,.23]);
  const breathing = passive ? strength*.48 : strength*(.72+.23*Math.sin(t*4.9));
  fx.stamp(core,(.43+.19*breathing)*(1-close*.45),.52,spiritBlue,6,breathing,0);
  if (!passive) {
    fx.stamp(core,.21,.20,spiritIce,3,breathing*.96,0);
    for(let i=0;i<2;i++){
      const loop=sequence(20,u=>add(core,polar(.25*(1-close*.67),.14*Math.sin(u*turn),u*turn+t*2.7+i*1.7)));
      fx.ribbon(loop,.025,spiritIce,breathing*.61,i);
    }
  }
  return passive ? "补灵 · 淡光归身" : close>.15 ? "收势 · 灵核归体" : "聚气 · 向丹田汇流";
}

function bloodlust(fx,t,passive,pose) {
  const activation=passive?0:pulse(t,0,.31,.83);
  const buff=passive?1:ease(.04,.29,t)*(1-ease(6.62,7.55,t));
  const strike=attackTimes.find(at=>t>=at-.04 && t<at+.64);
  const elapsed=strike===undefined?0:t-strike;
  const impact=strike===undefined?0:pulse(elapsed,-.04,.094,.64);
  if(!passive && buff>.004) {
    const anchor=pose.blade??pose.hand;
    const charge=buff*(.81+.18*Math.sin(t*7.3));
    for(let i=0;i<4;i++) {
      const angle=t*2.35+i*turn/4;
      const p=add(anchor,polar(.11+(i%2)*.027,(i-1.5)*.054,angle));
      fx.stamp(p,.28+(i%2)*.06,.22,ruby,2,charge*(.62+activation*.32),i+2,angle);
    }
    for(let i=0;i<12;i++) {
      const age=wave(t,1.19,i*.10)/1.19;
      const angle=i*2.39996+age*2.14;
      const radius=(.31+(i%5)*.069)*(1-age);
      const p=add(anchor,polar(radius,-.36*(1-age)+.04*Math.cos(angle),angle));
      fx.stamp(p,.072+(i%3)*.030,.034+(i%2)*.027,carmine,7,
        charge*Math.sin(age*Math.PI)*.81,i,angle);
    }
  }
  if(impact>.01 && buff>.03) {
    const tip=weaponTip(performance(3,passive,strike));
    const up=norm(cross(tip.forward,tip.side));
    const push=ease(-.023,.41,elapsed);
    const size=passive?.46:1;
    const strength=impact*buff;
    const center=add(tip.center,mul(tip.forward,.23*push*size));
    fx.stamp(center,(.27+.57*push)*size,(.33+.68*push)*size,ruby,2,strength,10,-.27);
    const inkCount=passive?2:5;
    for(let i=0;i<inkCount;i++) {
      const angle=i*2.39996+.40;
      const spread=size*push*(.25+(i%3)*.14);
      const offset=add(mul(tip.side,Math.cos(angle)*spread),mul(up,Math.sin(angle)*spread));
      const p=add(add(center,offset),mul(tip.forward,push*(.15+i*.05)*size));
      const cloud=(.18+.36*push)*size;
      fx.stamp(p,cloud*1.25,cloud*.84,i%2===0?carmine:ruby,2,strength*.76,12+i,angle);
    }
    for(let i=0;i<(passive?5:11);i++){
      const angle=i*2.39996-.71;
      const spread=push*size*(.38+(i%7)*.091);
      const radial=add(mul(tip.side,Math.cos(angle)*spread),mul(up,Math.sin(angle)*spread));
      const p=add(add(center,radial),mul(tip.forward,push*size*(.21+(i%5)*.095)));
      const chip=(.073+(i%4)*.026)*size;
      fx.stamp(p,chip*1.6,chip,carmine,7,strength*(1-push*.34),i,angle+push*.75);
    }
    fx.stamp(tip.center,.29*size,.28*size,carmine,3,pulse(elapsed,-.024,.038,.19)*buff,0);
  }
  return impact>.12 && buff>.10?"命中 · 血墨迸裂":passive?"待击":
    activation>.09?"起势 · 血墨凝聚":buff>.05?"强化 · 血墨附刃":t<.5?"起势 · 引动血气":"收势 · 血墨散去";
}

export function drawArt(fx,art,passive,state,t,pose) {
  switch(art) {
    case 0: return lightBody(fx,state,t,passive);
    case 1: return renewal(fx,state.root,t,passive);
    case 2: return gathering(fx,state.root,t,passive);
    case 3: return bloodlust(fx,t,passive,pose);
    case 4: return purification(fx,state.root,t,passive);
    case 5: return iceSpike(fx,state.root,t);
    case 6: return flameWave(fx,state.root,t);
  }
}
