package com.aeroai;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = AeroAI.MODID, dist = Dist.CLIENT)
public class AeroAI {
    public static final String MODID = "aeroai";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AeroAI(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        // In-game config screen: Mods list -> AeroAI -> Config
        container.registerExtensionPoint(IConfigScreenFactory.class, LongTextConfigScreen::create);

        NeoForge.EVENT_BUS.register(ChatListener.class);
        NeoForge.EVENT_BUS.register(ChatSender.class);
        NeoForge.EVENT_BUS.register(AeroCommands.class);
    }
}
