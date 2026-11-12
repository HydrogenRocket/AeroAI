package com.aeroai;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Request pipeline: builds the prompt, calls Ollama, cleans the reply and hands it to {@link ChatSender}. */
public final class AeroBot {
    private static final Deque<OllamaClient.Message> HISTORY = new ArrayDeque<>();
    private static boolean busy = false;
    private static long lastRequestAt = 0;

    /**
     * @param prompt     the user's question (text after the wake word)
     * @param senderName who asked, or null if unknown
     * @param postToChat true = send the reply to the server, false = only show it locally
     * @param manual     true when run from a command (bypasses cooldown)
     */
    public static void ask(String prompt, String senderName, boolean postToChat, boolean manual) {
        long now = System.currentTimeMillis();
        if (busy) {
            info("Still waiting on the previous reply, ignoring: " + prompt);
            return;
        }
        if (!manual && now - lastRequestAt < Config.COOLDOWN_SECONDS.get() * 1000L) {
            AeroAI.LOGGER.debug("On cooldown, ignoring: {}", prompt);
            return;
        }
        busy = true;
        lastRequestAt = now;

        String userContent = Config.INCLUDE_SENDER.get() && senderName != null
                ? senderName + " asks: " + prompt
                : prompt;

        List<OllamaClient.Message> messages = new ArrayList<>();
        String system = Config.SYSTEM_PROMPT.get();
        if (!system.isBlank()) {
            messages.add(new OllamaClient.Message("system", system));
        }
        messages.addAll(HISTORY);
        messages.add(new OllamaClient.Message("user", userContent));

        AeroAI.LOGGER.info("Asking {}: {}", Config.MODEL.get(), userContent);

        OllamaClient.chat(messages).whenComplete((reply, error) -> Minecraft.getInstance().execute(() -> {
            busy = false;
            if (error != null) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                AeroAI.LOGGER.error("Ollama request failed", cause);
                info("Ollama error: " + cause.getMessage(), ChatFormatting.RED);
                return;
            }
            String cleaned = clean(reply);
            if (cleaned.isEmpty()) {
                info("Model returned an empty reply", ChatFormatting.RED);
                return;
            }
            remember(userContent, cleaned);
            if (postToChat) {
                ChatSender.sendReply(cleaned);
            } else {
                info(ChatSender.format(cleaned), ChatFormatting.WHITE);
            }
        }));
    }

    private static void remember(String question, String answer) {
        int max = Config.HISTORY_SIZE.get();
        if (max <= 0) {
            HISTORY.clear();
            return;
        }
        HISTORY.addLast(new OllamaClient.Message("user", question));
        HISTORY.addLast(new OllamaClient.Message("assistant", answer));
        while (HISTORY.size() > max * 2) {
            HISTORY.removeFirst();
        }
    }

    public static void clearHistory() {
        HISTORY.clear();
    }

    /** Makes model output safe and tidy for a single-line Minecraft chat message. */
    static String clean(String reply) {
        String s = reply.replaceAll("(?s)<think>.*?</think>", "");
        s = s.replaceAll("\\*\\*|__|`+|^#+\\s*", "");
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '\n' || c == '\r' || c == '\t') {
                out.append(' ');
            } else if (c != '\u00a7' && c >= ' ' && c != 127) {
                out.append(c);
            }
        }
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    public static void info(String msg) {
        info(msg, ChatFormatting.GRAY);
    }

    public static void info(String msg, ChatFormatting color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.gui.getChat().addMessage(Component.literal("[AeroAI] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(msg).withStyle(color)));
    }

    private AeroBot() {}
}
