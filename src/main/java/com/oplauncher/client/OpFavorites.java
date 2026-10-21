package com.oplauncher.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quick Play favorites. Op Launcher passes the file with -Dop.favorites=...; it lists these servers in its title bar
 * and starts the game straight into the one you click. Plain Java on purpose (no Minecraft classes), so it works on every version.
 */
public final class OpFavorites {
    private static final String PATH = System.getProperty("op.favorites", "");
    private static final Pattern IP = Pattern.compile("^[A-Za-z0-9._-]{1,200}(:\\d{1,5})?$");
    private static final Pattern ENTRY = Pattern.compile("\\{\\s*\"name\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"ip\"\\s*:\\s*\"([^\"]*)\"\\s*}");
    private static final int MAX = 12;

    private OpFavorites() { }

    public static boolean enabled() { return !PATH.isEmpty(); }

    public static boolean isValidIp(String ip) {
        return ip != null && IP.matcher(ip).matches() && !ip.startsWith("-") && !ip.startsWith(".");
    }

    /** name, ip pairs */
    public static List<String[]> read(Path file) {
        List<String[]> out = new ArrayList<>();
        try {
            if (!Files.exists(file)) return out;
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher m = ENTRY.matcher(text);
            while (m.find()) {
                if (isValidIp(m.group(2))) out.add(new String[]{m.group(1), m.group(2)});
            }
        } catch (IOException | RuntimeException ignored) {
            // unreadable file: treat as empty
        }
        return out;
    }

    static void write(Path file, List<String[]> servers) throws IOException {
        StringBuilder sb = new StringBuilder("{\"servers\":[\n");
        for (int i = 0; i < servers.size(); i++) {
            sb.append(i == 0 ? "" : ",\n").append("{\"name\":\"").append(clean(servers.get(i)[0])).append("\",\"ip\":\"").append(servers.get(i)[1]).append("\"}");
        }
        sb.append("\n]}\n");
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String clean(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\' || c < 32 || c == 127) continue;
            sb.append(c);
            if (sb.length() >= 40) break;
        }
        return sb.toString().trim();
    }

    public static boolean contains(String ip) {
        return contains(Paths.get(PATH), ip);
    }

    static boolean contains(Path file, String ip) {
        for (String[] s : read(file)) if (s[1].equalsIgnoreCase(ip)) return true;
        return false;
    }

    /** Adds the server, or removes it when it is already a favorite. Returns true when it is a favorite afterwards. */
    public static boolean toggle(String name, String ip) throws IOException {
        return toggle(Paths.get(PATH), name, ip);
    }

    static boolean toggle(Path file, String name, String ip) throws IOException {
        if (!isValidIp(ip)) throw new IOException("unsupported server address");
        List<String[]> list = read(file);
        boolean removed = list.removeIf(s -> s[1].equalsIgnoreCase(ip));
        if (!removed) {
            if (list.size() >= MAX) throw new IOException("Quick Play is full (" + MAX + " servers)");
            String n = clean(name);
            list.add(new String[]{n.isEmpty() ? ip : n, ip});
        }
        write(file, list);
        return !removed;
    }
}
