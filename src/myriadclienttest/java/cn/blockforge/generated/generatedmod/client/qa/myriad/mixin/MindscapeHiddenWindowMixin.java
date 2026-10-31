package cn.blockforge.generated.generatedmod.client.qa.myriad.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Opt-in QA source only: no visible/focused client window is ever created. */
@Mixin(value=Window.class,priority=1100)
public abstract class MindscapeHiddenWindowMixin {
    @ModifyArg(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J",remap=false),index=0)
    private java.util.function.IntSupplier mindscapeHidden(java.util.function.IntSupplier width) {
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW,GLFW.GLFW_FALSE);
        return width;
    }
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lorg/lwjgl/glfw/GLFW;glfwShowWindow(J)V",remap=false),require=0)
    private void neverShowQaWindow(long window) {
        GLFW.glfwHideWindow(window);
    }
}
