package cn.blockforge.generated.generatedmod.client.qa.v5.mixin;

import cn.blockforge.generated.generatedmod.client.qa.v5.V5ClientAcceptance;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Test-only: set visibility/focus hints before Forge creates the independent client window. */
@Mixin(value=Window.class,priority=1100)
public abstract class HiddenWindowMixin {
    // Insert the hints before the invocation without owning its redirect: Embeddium must
    // keep its context-creation workaround around the same call.
    @ModifyArg(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J",remap=false),index=0)
    private java.util.function.IntSupplier hidden(java.util.function.IntSupplier width){
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED,GLFW.GLFW_FALSE);GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW,GLFW.GLFW_FALSE);
        return width;
    }
}
