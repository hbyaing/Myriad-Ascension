package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.yujian.HuahongFlight;

final class HuahongFlightRegression {
    static void run(){
        check(HuahongFlight.ENTRY_COST==.0025&&HuahongFlight.UPKEEP_PER_SECOND==.005,"reduced entry and sustained mana budget");
        check(!HuahongFlight.supported(5)&&HuahongFlight.supported(6)&&HuahongFlight.supported(7),"sword tier boundary");
        var f=new HuahongFlight();
        for(int i=0;i<100;i++)check(!f.tick(true,false,true)&&f.stage()==0,"stationary input must not charge");
        for(int i=0;i<HuahongFlight.STABLE_TICKS-1;i++)check(!f.tick(true,true,true),"stable interval");
        check(f.tick(true,true,true)&&f.stage()==1,"one entry charge");
        int charges=1;
        for(int i=0;i<100;i++){
            if(f.tick(true,true,true))charges++;
            check(f.multiplier()>=1&&f.multiplier()<=2,"bounded multiplier");
        }
        check(charges==1&&f.stage()==2&&f.multiplier()==2,"sustained exact double without repeated charge");
        for(int i=0;i<40;i++)check(!f.tick(true,false,true)&&f.stage()==2,"entry speed is not a turn-time exit threshold");
        for(int i=0;i<HuahongFlight.SPRINT_GRACE_TICKS;i++)
            check(!f.tick(true,false,true,false)&&f.stage()==2,"brief sprint release should retain rainbow during deceleration");
        f.tick(true,true,true,true);
        check(f.stage()==2&&f.multiplier()==2,"resumed sprint restores the grace window without another entry charge");
        for(int i=0;i<HuahongFlight.SPRINT_GRACE_TICKS;i++)f.tick(true,false,true,false);
        check(f.stage()==2,"sprint grace should reset on a resumed input");
        f.tick(false,true,true);
        check(f.stage()==3&&f.multiplier()==1,"brake immediately removes speed bonus");
        float previous=f.strength();
        for(int i=0;i<8;i++){f.tick(true,true,true);check(f.strength()<=previous,"exit opacity");previous=f.strength();}
        check(f.stage()==0&&f.strength()==0,"fully restored");
        for(int i=0;i<HuahongFlight.COOLDOWN-1;i++)check(!f.tick(true,true,true),"cooldown");
        for(int i=0;i<100;i++)f.tick(true,true,false);
        check(f.stage()==0,"cannot enter without mana");
        for(int i=0;i<34;i++)f.tick(true,true,true);
        f.tick(true,true,false);check(f.stage()==3,"low mana exits");
        f.reset();
        for(int i=0;i<HuahongFlight.STABLE_TICKS+HuahongFlight.GATHER_TICKS;i++)f.tick(true,true,true);
        for(int i=0;i<=HuahongFlight.SPRINT_GRACE_TICKS;i++)f.tick(true,false,true,false);
        check(f.stage()==3,"sustained sprint release still exits");
        f.reset();check(f.stage()==0&&f.multiplier()==1&&f.strength()==0,"lifecycle reset");
        System.out.println("Huahong flight lifecycle regression passed");
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError("Huahong: "+message);}
}
