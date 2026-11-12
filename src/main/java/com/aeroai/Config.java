package com.aeroai;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- General ---
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.ConfigValue<String> WAKE_WORD;
    public static final ModConfigSpec.BooleanValue CASE_SENSITIVE;
    public static final ModConfigSpec.BooleanValue RESPOND_TO_SELF;
    public static final ModConfigSpec.BooleanValue RESPOND_TO_SYSTEM;
    public static final ModConfigSpec.BooleanValue INCLUDE_SENDER;
    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS;

    // --- Ollama ---
    public static final ModConfigSpec.ConfigValue<String> OLLAMA_URL;
    public static final ModConfigSpec.ConfigValue<String> MODEL;
    public static final ModConfigSpec.ConfigValue<String> DISPLAY_NAME;
    public static final ModConfigSpec.ConfigValue<String> SYSTEM_PROMPT;
    public static final ModConfigSpec.DoubleValue TEMPERATURE;
    public static final ModConfigSpec.IntValue MAX_TOKENS;
    public static final ModConfigSpec.BooleanValue DISABLE_THINKING;
    public static final ModConfigSpec.IntValue HISTORY_SIZE;
    public static final ModConfigSpec.IntValue TIMEOUT_SECONDS;

    // --- Output ---
    public static final ModConfigSpec.ConfigValue<String> MESSAGE_FORMAT;
    public static final ModConfigSpec.IntValue MAX_MESSAGES;
    public static final ModConfigSpec.IntValue MESSAGE_DELAY_MS;

    static {
        BUILDER.push("general");
        ENABLED = BUILDER.comment("Master on/off switch").define("enabled", true);
        WAKE_WORD = BUILDER.comment("Word that triggers the bot. Everything after it in the chat line is sent to the model.")
                .define("wakeWord", "aero");
        CASE_SENSITIVE = BUILDER.comment("Whether the wake word must match case exactly").define("caseSensitive", false);
        RESPOND_TO_SELF = BUILDER.comment("Allow your own chat messages to trigger the bot").define("respondToSelf", true);
        RESPOND_TO_SYSTEM = BUILDER.comment("Also scan system/server messages (many servers relay player chat this way)")
                .define("respondToSystemMessages", true);
        INCLUDE_SENDER = BUILDER.comment("Tell the model the name of the player who asked (when known)")
                .define("includeSenderName", true);
        COOLDOWN_SECONDS = BUILDER.comment("Minimum seconds between requests (helps avoid spam kicks)")
                .defineInRange("cooldownSeconds", 3, 0, 3600);
        BUILDER.pop();

        BUILDER.push("ollama");
        OLLAMA_URL = BUILDER.comment("Base URL of the Ollama server").define("ollamaUrl", "http://localhost:11434");
        MODEL = BUILDER.comment("Ollama model name, e.g. gemma4:latest").define("model", "gemma4:latest");
        DISPLAY_NAME = BUILDER.comment("Name shown in chat instead of the model name (leave blank to use the model name)")
                .define("displayName", "");
        SYSTEM_PROMPT = BUILDER.comment("System prompt given to the model")
                .define("systemPrompt", "You are a helpful assistant inside a Minecraft chat. "
                        + "Reply in plain text with no markdown, in at most two short sentences.");
        TEMPERATURE = BUILDER.comment("Sampling temperature").defineInRange("temperature", 0.7, 0.0, 2.0);
        MAX_TOKENS = BUILDER.comment("Maximum tokens the model may generate (num_predict)")
                .defineInRange("maxTokens", 200, 16, 4096);
        DISABLE_THINKING = BUILDER.comment("Disable 'thinking' on reasoning models for faster replies")
                .define("disableThinking", true);
        HISTORY_SIZE = BUILDER.comment("How many previous question/answer pairs to remember (0 = no memory)")
                .defineInRange("historySize", 4, 0, 50);
        TIMEOUT_SECONDS = BUILDER.comment("Seconds to wait for the model before giving up")
                .defineInRange("timeoutSeconds", 120, 5, 600);
        BUILDER.pop();

        BUILDER.push("output");
        MESSAGE_FORMAT = BUILDER.comment("Chat format. Placeholders: {wakeword} {model} {reply}")
                .define("messageFormat", "[{wakeword}] [{model}] : {reply}");
        MAX_MESSAGES = BUILDER.comment("Long replies are split into several chat messages; this caps how many")
                .defineInRange("maxMessages", 3, 1, 10);
        MESSAGE_DELAY_MS = BUILDER.comment("Delay between split messages, in milliseconds")
                .defineInRange("messageDelayMs", 1200, 0, 10000);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Name shown in the chat prefix. */
    public static String displayName() {
        String name = DISPLAY_NAME.get().trim();
        return name.isEmpty() ? MODEL.get() : name;
    }

    private Config() {}
}
