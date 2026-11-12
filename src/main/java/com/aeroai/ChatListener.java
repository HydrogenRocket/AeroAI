package com.aeroai;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatListener {

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        if (!Config.ENABLED.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String fullText = event.getMessage().getString();
        // Never react to our own replies (prevents infinite loops)
        if (ChatSender.isOwnReply(fullText)) return;

        String text;
        String senderName = null;
        if (event instanceof ClientChatReceivedEvent.Player playerEvent) {
            UUID sender = playerEvent.getSender();
            if (sender.equals(mc.player.getUUID()) && !Config.RESPOND_TO_SELF.get()) return;
            // Use the raw message body so the sender's name can't accidentally match the wake word
            text = playerEvent.getPlayerChatMessage().signedContent();
            senderName = lookupName(mc, sender);
        } else {
            if (!Config.RESPOND_TO_SYSTEM.get()) return;
            text = fullText;
        }

        String prompt = extractPrompt(text, Config.WAKE_WORD.get(), Config.CASE_SENSITIVE.get());
        if (prompt == null || prompt.isEmpty()) return;

        AeroBot.ask(prompt, senderName, true, false);
    }

    private static String lookupName(Minecraft mc, UUID uuid) {
        if (mc.getConnection() == null) return null;
        PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
        return info != null ? info.getProfile().getName() : null;
    }

    /** Returns the text after the first whole-word occurrence of the wake word, or null if absent. */
    static String extractPrompt(String text, String wakeWord, boolean caseSensitive) {
        if (wakeWord == null || wakeWord.isBlank()) return null;
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern p = Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(wakeWord.trim()) + "(?![\\p{L}\\p{N}_])", flags);
        Matcher m = p.matcher(text);
        if (!m.find()) return null;
        return text.substring(m.end()).replaceFirst("^[\\s,:;.!?\\-]+", "").trim();
    }

    private ChatListener() {}
}
