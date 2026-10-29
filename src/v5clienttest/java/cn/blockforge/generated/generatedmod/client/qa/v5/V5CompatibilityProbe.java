package cn.blockforge.generated.generatedmod.client.qa.v5;

import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import com.google.gson.GsonBuilder;

/** Opt-in QA only. Must observe a loaded pipeline, not just an installed Oculus jar. */
final class V5CompatibilityProbe {
    static void verifyScreen(Minecraft mc){if(Boolean.getBoolean("wuxingxiantu.v5Compatibility")&&Boolean.getBoolean("wuxingxiantu.v5ExpectedJei")&&mc.screen instanceof cn.blockforge.generated.generatedmod.client.AlchemyScreen)JeiProbe.verifyOverlay();}
    static void verify(Minecraft mc,Path output)throws Exception{
        boolean expectedJei=Boolean.getBoolean("wuxingxiantu.v5ExpectedJei"),expectedShaders=Boolean.getBoolean("wuxingxiantu.v5ExpectedShaders");
        boolean jei=net.minecraftforge.fml.ModList.get().isLoaded("jei"),shader=false;String pack="none";
        if(jei!=expectedJei)throw new AssertionError("JEI load state mismatch");
        if(jei)verifyJei();
        for(String type:new String[]{"net.irisshaders.iris.api.v0.IrisApi","net.coderbot.iris.api.v0.IrisApi"})try{
            var api=Class.forName(type);shader=(boolean)api.getMethod("isShaderPackInUse").invoke(api.getMethod("getInstance").invoke(null));break;
        }catch(ClassNotFoundException ignored){}
        if(shader)for(String type:new String[]{"net.irisshaders.iris.Iris","net.coderbot.iris.Iris"})try{
            Object value=Class.forName(type).getMethod("getCurrentPackName").invoke(null);pack=value instanceof Optional<?> o?String.valueOf(o.orElse(null)):String.valueOf(value);break;
        }catch(ClassNotFoundException ignored){}
        if(shader!=expectedShaders||expectedShaders&&!pack.contains("ComplementaryReimagined"))throw new AssertionError("Actual shader pipeline mismatch: "+pack+" active="+shader);
        if(org.lwjgl.opengl.GL11.glGetError()!=0)throw new AssertionError("GL error after acceptance");
        Files.writeString(output.resolve(Boolean.getBoolean("wuxingxiantu.v5Resume")?"compatibility-resume.json":"compatibility.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",true,"jeiLoaded",jei,"shaderActive",shader,"shaderPack",pack,"world",mc.level.dimension().location().toString(),"glError",0,"blockedGrabs",V5ClientAcceptance.blockedGrabs)));
    }
    private static void verifyJei()throws Exception{
        // Keep optional API references inside a separately loaded helper on the JEI-only path.
        JeiProbe.verify();
    }
    private static final class JeiProbe {
        static void verifyOverlay(){var rt=cn.blockforge.generated.generatedmod.compat.jei.WuXingJeiPlugin.runtime;if(rt==null||!rt.getIngredientListOverlay().isListDisplayed()||rt.getIngredientListOverlay().getVisibleIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK).isEmpty())throw new AssertionError("JEI list hidden by fitted alchemy panel");}
        static void verify(){
            var rt=cn.blockforge.generated.generatedmod.compat.jei.WuXingJeiPlugin.runtime;if(rt==null)throw new AssertionError("JEI runtime not initialized");
            for(var entry:cn.blockforge.generated.generatedmod.compat.jei.WuXingJeiPlugin.entries())if(!entry.core()){
                String text=entry.tooltip(true,0).stream().map(net.minecraft.network.chat.Component::getString).collect(java.util.stream.Collectors.joining("\n"));
                if(!text.contains("一至九品")||text.contains("与品质分别计算")||text.contains("品质：下品～极品"))throw new AssertionError("JEI retained legacy pill axes: "+entry.id());
            }
            if(rt.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK).isEmpty())throw new AssertionError("JEI ingredient registry empty");
        }
    }
}
