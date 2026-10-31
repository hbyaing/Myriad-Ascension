package cn.blockforge.generated.generatedmod.client.mindscape;

import com.mojang.logging.LogUtils;
import java.util.function.Function;

/** Optional Oculus adapter, restricted to this finite interior's dimension. */
public final class MindscapeShaderPipelineCompat {
    public static int independentPipelinesCreated;
    private MindscapeShaderPipelineCompat() {}

    /** Called only by the optional PipelineManager constructor mixin. */
    public static Function<Object, Object> wrapPipelineFactory(Function<Object, Object> original) {
        return dimension -> {
            if (!isMindscape(dimension)) return original.apply(dimension);
            try {
                String prefix = dimension.getClass().getName().startsWith("net.coderbot.")
                        ? "net.coderbot.iris" : "net.irisshaders.iris";
                Object result = Class.forName(prefix + ".pipeline.VanillaRenderingPipeline").getConstructor().newInstance();
                independentPipelinesCreated++;
                LogUtils.getLogger().info("Mindscape uses an independent vanilla pipeline; outside shader pack remains configured");
                return result;
            } catch (ReflectiveOperationException | LinkageError error) {
                throw new IllegalStateException("Cannot create the mindscape's independent rendering pipeline", error);
            }
        };
    }

    private static boolean isMindscape(Object dimension) {
        if (dimension == null) return false;
        try {
            return "wuxingxiantu".equals(dimension.getClass().getMethod("getNamespace").invoke(dimension))
                    && "mindscape".equals(dimension.getClass().getMethod("getName").invoke(dimension));
        } catch (ReflectiveOperationException error) {
            // An unknown loader's identifier must never redirect other worlds.
            return false;
        }
    }

    public static String pipelineName() {
        for (String name : new String[]{"net.irisshaders.iris.Iris", "net.coderbot.iris.Iris"}) {
            try {
                Object manager = Class.forName(name).getMethod("getPipelineManager").invoke(null);
                Object pipeline = manager.getClass().getMethod("getPipelineNullable").invoke(manager);
                return pipeline == null ? "unprepared" : pipeline.getClass().getName();
            } catch (ReflectiveOperationException | LinkageError ignored) {}
        }
        return "minecraft-native";
    }

    static String packName() {
        for (String name : new String[]{"net.irisshaders.iris.Iris", "net.coderbot.iris.Iris"}) {
            try { return String.valueOf(Class.forName(name).getMethod("getCurrentPackName").invoke(null)); }
            catch (ReflectiveOperationException | LinkageError ignored) {}
        }
        return "";
    }
}
