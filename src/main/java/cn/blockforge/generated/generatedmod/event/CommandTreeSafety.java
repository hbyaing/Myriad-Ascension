package cn.blockforge.generated.generatedmod.event;

import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.blockforge.generated.generatedmod.GeneratedMod;

/**
 * Removes legacy root aliases from the optional 创世仙途 add-on.
 *
 * The add-on registers these aliases as Brigadier redirects to children of
 * another root.  Forge/Minecraft 1.20.1 serializes the resulting graph to
 * the client; in the affected add-on build that graph contains a redirect
 * cycle and the client overflows while resolving the command packet.  The
 * canonical /chuangshi tree remains available, while the aliases are safe
 * to drop because they are compatibility spellings of that tree.
 */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommandTreeSafety {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandTreeSafety.class);
    private static final String[] LEGACY_CREATION_ALIASES = {
            "wuxingchuangshi", "xianjie", "xianmen", "taixu"
    };

    private CommandTreeSafety() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void removeCyclicCreationAliases(RegisterCommandsEvent event) {
        RootCommandNode<CommandSourceStack> root = event.getDispatcher().getRoot();
        int removed = 0;
        for (String alias : LEGACY_CREATION_ALIASES) {
            if (root.getChild(alias) != null) {
                // Brigadier 1.1.8 does not expose removeCommand publicly, but
                // getChildren() is the live values view of the root map.
                root.getChildren().remove(root.getChild(alias));
                removed++;
            }
        }
        if (removed > 0) {
            LOGGER.warn("Removed {} legacy 创世仙途 command aliases before client sync", removed);
        }
    }
}
