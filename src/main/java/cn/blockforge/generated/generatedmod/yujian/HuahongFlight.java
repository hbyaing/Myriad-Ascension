package cn.blockforge.generated.generatedmod.yujian;

/** Server-owned rainbow flight lifecycle. Orthogonal to Dunxu collision permission. */
public final class HuahongFlight {
    public static final int STABLE_TICKS=20, GATHER_TICKS=6, ACCEL_TICKS=8, EXIT_TICKS=8, COOLDOWN=20;
    public static final double ENTRY_RATIO=.90, ENTRY_COST=.01, UPKEEP_PER_SECOND=.02;
    private int stage,ticks,stable,cooldown;
    private float strength,exitStrength;
    public int stage(){return stage;}
    public float strength(){return strength;}
    public boolean boosting(){return stage==2;}
    public double multiplier(){return boosting()?1.0+smooth(Math.min(1,ticks/(double)ACCEL_TICKS)):1;}
    public static boolean supported(int tier){return tier>=6;}
    public boolean tick(boolean eligible,boolean atSpeed,boolean funded){
        if(cooldown>0)cooldown--;
        if(stage==3){
            strength=exitStrength*(1-(float)++ticks/EXIT_TICKS);
            if(ticks>=EXIT_TICKS){stage=0;ticks=0;strength=0;}
            return false;
        }
        if(stage!=0&&(!eligible||!funded)){exit();return false;}
        if(stage==0){
            if(!eligible||!atSpeed||!funded||cooldown>0){stable=0;return false;}
            if(++stable<STABLE_TICKS)return false;
            stage=1;ticks=0;stable=0;return true; // caller charges entry exactly once
        }
        ticks++;
        if(stage==1){
            strength=(float)smooth(Math.min(1,ticks/(double)GATHER_TICKS));
            if(ticks>=GATHER_TICKS){stage=2;ticks=0;strength=1;}
        }else strength=1;
        return false;
    }
    public void exit(){
        stable=0;
        if(stage==0||stage==3)return;
        stage=3;ticks=0;exitStrength=strength;cooldown=COOLDOWN+EXIT_TICKS;
    }
    public void reset(){stage=ticks=stable=cooldown=0;strength=exitStrength=0;}
    private static double smooth(double t){return t*t*(3-2*t);}
}
