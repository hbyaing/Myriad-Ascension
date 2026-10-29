package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.client.ScreenFitPolicy;

/** Boundary matrix uses the production policy and real panel dimensions, including high DPI/tiny windows. */
public final class ScreenFitRegression {
    public static void run(){
        int checks=0;
        int[][] resolutions={{320,240},{640,360},{854,480},{1024,768},{1280,720},{1366,768},{1719,1073},{1920,1080},{2560,1440},{3440,1440},{3840,2160}};
        int[][] panels={{452,352},{452,352},{332,274},{372,278},{392,316},{392,298},{462,372},{272,282},{252,242},{252,227},{208,220},{208,179},{334,238},{480,360}};
        for(var resolution:resolutions)for(var panel:panels)for(int option=1;option<=12;option++)for(boolean unicode:new boolean[]{false,true}){
            double scale=ScreenFitPolicy.scale(resolution[0],resolution[1],option,panel[0],panel[1],unicode);
            if(!Double.isFinite(scale)||scale<=0||scale>option||resolution[0]/scale+1e-7<panel[0]||resolution[1]/scale+1e-7<panel[1])
                throw new AssertionError("UI extends outside framebuffer");
            checks++;
        }
        if(ScreenFitPolicy.scale(1920,1080,4,452,352,false)!=3)throw new AssertionError("1080p alchemy should fit at GUI 3");
        if(ScreenFitPolicy.scale(1280,720,4,452,352,false)!=2)throw new AssertionError("720p alchemy should fit at GUI 2");
        if(ScreenFitPolicy.scale(1920,1080,1,452,352,false)!=1)throw new AssertionError("Respect smaller user scale");
        if(ScreenFitPolicy.scale(1440,1080,3,636,352,false)!=2)throw new AssertionError("JEI gutters must remain visible at 1440px");
        if(ScreenFitPolicy.scale(1920,1080,4,636,352,false)!=3)throw new AssertionError("1080p retains GUI3 with JEI gutters");
        System.out.println("Screen fit: "+checks+" resolution/panel/option/unicode bounds passed");
    }
    private ScreenFitRegression(){}
}
