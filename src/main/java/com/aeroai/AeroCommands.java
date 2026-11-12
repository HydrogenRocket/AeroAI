package com.aeroai;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** Client-side /aeroai commands for quick in-game changes. */
public final class AeroCommands {
    private static List<String> knownModels = List.of();
    private static boolean openConfigNextTick = false;

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("aeroai")
                .executes(ctx -> status())
                .then(Commands.literal("on").executes(ctx -> setBool(Config.ENABLED, true, "AeroAI enabled")))
                .then(Commands.literal("off").executes(ctx -> setBool(Config.ENABLED, false, "AeroAI disabled")))
                .then(Commands.literal("config").executes(ctx -> {
                    openConfigNextTick = true;
                    return 1;
                }))
                .then(Commands.literal("models").executes(ctx -> listModels()))
                .then(Commands.literal("clear").executes(ctx -> {
                    AeroBot.clearHistory();
                    AeroBot.info("Conversation memory cleared");
                    return 1;
                }))
                .then(Commands.literal("wakeword")
                        .then(Commands.argument("word", StringArgumentType.word())
                                .executes(ctx -> setString(Config.WAKE_WORD, arg(ctx, "word"), "Wake word"))))
                .then(Commands.literal("model")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(knownModels, b))
                                .executes(ctx -> setString(Config.MODEL, arg(ctx, "name"), "Model"))))
                .then(Commands.literal("system")
                        .then(Commands.argument("prompt", StringArgumentType.greedyString())
                                .executes(ctx -> setString(Config.SYSTEM_PROMPT, arg(ctx, "prompt"), "System prompt"))))
                .then(Commands.literal("ask")
                        .then(Commands.argument("prompt", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    AeroBot.ask(arg(ctx, "prompt"), null, true, true);
                                    return 1;
                                })))
                .then(Commands.literal("test")
                        .then(Commands.argument("prompt", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    AeroBot.info("Asking " + Config.MODEL.get() + " (reply shown only to you)...");
                                    AeroBot.ask(arg(ctx, "prompt"), null, false, true);
                                    return 1;
                                }))));

        // Prime model suggestions
        OllamaClient.listModels().thenAccept(models -> knownModels = models);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        // Deferred so the chat screen closing after the command doesn't immediately close the config screen
        if (!openConfigNextTick) return;
        openConfigNextTick = false;
        Minecraft mc = Minecraft.getInstance();
        ModList.get().getModContainerById(AeroAI.MODID)
                .ifPresent(container -> mc.setScreen(LongTextConfigScreen.create(container, mc.screen)));
    }

    private static String arg(CommandContext<CommandSourceStack> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }

    private static int status() {
        AeroBot.info("Enabled: " + Config.ENABLED.get()
                + " | Wake word: " + Config.WAKE_WORD.get()
                + " | Model: " + Config.MODEL.get()
                + " | Ollama: " + Config.OLLAMA_URL.get(), ChatFormatting.WHITE);
        AeroBot.info("Commands: on, off, config, models, model <name>, wakeword <word>, system <prompt>, ask <prompt>, test <prompt>, clear");
        return 1;
    }

    private static int listModels() {
        OllamaClient.listModels().whenComplete((models, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                AeroBot.info("Could not reach Ollama at " + Config.OLLAMA_URL.get(), ChatFormatting.RED);
                return;
            }
            knownModels = models;
            AeroBot.info("Installed models: " + String.join(", ", models), ChatFormatting.WHITE);
        }));
        return 1;
    }

    private static int setBool(ModConfigSpec.BooleanValue value, boolean v, String msg) {
        value.set(v);
        Config.SPEC.save();
        AeroBot.info(msg);
        return 1;
    }

    private static int setString(ModConfigSpec.ConfigValue<String> value, String v, String label) {
        value.set(v);
        Config.SPEC.save();
        AeroBot.info(label + " set to: " + v);
        return 1;
    }

    private AeroCommands() {}
}
