package com.aeroai;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/** Formats replies, splits them to fit chat limits and sends them with a delay between parts. */
public final class ChatSender {
    private static final int CHAT_LIMIT = 256;
    private static final long ECHO_MEMORY_MS = 60_000;

    private static final Deque<String> QUEUE = new ArrayDeque<>();
    private static final Deque<Sent> RECENTLY_SENT = new ArrayDeque<>();
    private static long nextSendAt = 0;

    private record Sent(String text, long at) {}

    private static String rawFormat() {
        String f = Config.MESSAGE_FORMAT.get();
        return f.contains("{reply}") ? f : f + " {reply}";
    }

    private static String fill(String part) {
        return part.replace("{wakeword}", Config.WAKE_WORD.get()).replace("{model}", Config.displayName());
    }

    private static String prefix() {
        String f = rawFormat();
        return fill(f.substring(0, f.indexOf("{reply}")));
    }

    private static String suffix() {
        String f = rawFormat();
        return fill(f.substring(f.indexOf("{reply}") + "{reply}".length()));
    }

    public static String format(String reply) {
        return prefix() + reply + suffix();
    }

    public static void sendReply(String reply) {
        String prefix = prefix();
        String suffix = suffix();
        int room = Math.max(40, CHAT_LIMIT - prefix.length() - suffix.length());
        List<String> parts = split(reply, room, Config.MAX_MESSAGES.get());
        for (String part : parts) {
            String msg = prefix + part + suffix;
            QUEUE.add(msg.length() > CHAT_LIMIT ? msg.substring(0, CHAT_LIMIT) : msg);
        }
    }

    static List<String> split(String text, int room, int maxParts) {
        List<String> parts = new ArrayList<>();
        String rest = text;
        while (!rest.isEmpty()) {
            if (parts.size() == maxParts - 1 && rest.length() > room) {
                // Last allowed message: truncate with an ellipsis
                int cut = breakPoint(rest, room - 3);
                parts.add(rest.substring(0, cut).trim() + "...");
                break;
            }
            if (rest.length() <= room) {
                parts.add(rest);
                break;
            }
            int cut = breakPoint(rest, room);
            parts.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        return parts;
    }

    private static int breakPoint(String s, int max) {
        int space = s.lastIndexOf(' ', max);
        return space > max / 2 ? space : max;
    }

    /** True if this incoming chat line is (an echo of) something this mod sent. */
    public static boolean isOwnReply(String incoming) {
        String lower = incoming.toLowerCase(Locale.ROOT);
        String prefix = prefix().trim().toLowerCase(Locale.ROOT);
        if (!prefix.isEmpty() && lower.contains(prefix)) return true;
        long now = System.currentTimeMillis();
        RECENTLY_SENT.removeIf(s -> now - s.at() > ECHO_MEMORY_MS);
        for (Sent s : RECENTLY_SENT) {
            if (lower.contains(s.text())) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (QUEUE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            QUEUE.clear();
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextSendAt) return;
        String msg = QUEUE.poll();
        RECENTLY_SENT.add(new Sent(msg.toLowerCase(Locale.ROOT), now));
        mc.getConnection().sendChat(msg);
        nextSendAt = now + Config.MESSAGE_DELAY_MS.get();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        QUEUE.clear();
    }

    private ChatSender() {}
}
