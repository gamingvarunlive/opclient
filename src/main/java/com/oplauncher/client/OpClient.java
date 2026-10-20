package com.oplauncher.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Op Client. Settings come from Op Launcher as JVM properties:
 *   -Dop.ownName=true|false          show your own name tag in third person
 *   -Dop.server / -Dop.user / -Dop.token   friend server, used to find other Op Launcher players
 */
public class OpClient implements ClientModInitializer {
    private static final String TEAM = "opclient_logo";
    private static final String GLYPH = " ";           // the Op logo, see assets/minecraft/font/default.json
    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private static final boolean OWN_NAME = Boolean.parseBoolean(System.getProperty("op.ownName", "true"));
    private static final String SERVER = System.getProperty("op.server", "");
    private static final String USER = System.getProperty("op.user", "");
    private static final String TOKEN = System.getProperty("op.token", "");

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static Method profileMethod, nameMethod;

    public static boolean shouldShowOwnName(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        return OWN_NAME && entity != null && entity == mc.player && !mc.options.getCameraType().isFirstPerson();
    }

    @Override
    public void onInitializeClient() {
        if (SERVER.isEmpty() || USER.isEmpty() || TOKEN.isEmpty()) return;   // no friend server: only the name tag feature
        Thread t = new Thread(OpClient::loop, "OpClient-logo");
        t.setDaemon(true);
        t.start();
    }

    private static void loop() {
        Minecraft mc = Minecraft.getInstance();
        while (true) {
            try {
                Thread.sleep(15_000);
                CompletableFuture<List<String>> names = new CompletableFuture<>();
                mc.execute(() -> names.complete(tabNames(mc)));
                List<String> list = names.get(5, TimeUnit.SECONDS);
                if (list.isEmpty()) continue;
                Set<String> active = ask(list);
                if (active != null) mc.execute(() -> apply(mc, list, active));
            } catch (InterruptedException e) {
                return;
            } catch (Exception ignored) {
                // offline / server down: just try again later
            }
        }
    }

    private static List<String> tabNames(Minecraft mc) {
        List<String> out = new ArrayList<>();
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) return out;
        for (PlayerInfo info : conn.getOnlinePlayers()) {
            String n = profileName(info);
            if (n != null && NAME.matcher(n).matches() && out.size() < 100) out.add(n);
        }
        return out;
    }

    /** GameProfile.getName() became name() in newer versions, so look it up by reflection. */
    private static String profileName(PlayerInfo info) {
        try {
            if (profileMethod == null) profileMethod = PlayerInfo.class.getMethod("getProfile");
            Object profile = profileMethod.invoke(info);
            if (nameMethod == null) {
                for (String m : new String[]{"name", "getName"}) {
                    try { nameMethod = profile.getClass().getMethod(m); break; } catch (NoSuchMethodException ignored) { }
                }
            }
            return nameMethod == null ? null : String.valueOf(nameMethod.invoke(profile));
        } catch (Exception e) {
            return null;
        }
    }

    private static Set<String> ask(List<String> names) throws Exception {
        StringBuilder body = new StringBuilder("{\"names\":[");
        for (int i = 0; i < names.size(); i++) body.append(i == 0 ? "" : ",").append('"').append(names.get(i)).append('"');
        body.append("]}");
        HttpRequest req = HttpRequest.newBuilder(URI.create(SERVER.replaceAll("/+$", "") + "/v1/active"))
                .timeout(Duration.ofSeconds(8))
                .header("content-type", "application/json")
                .header("x-user", USER).header("x-token", TOKEN)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) return null;
        Matcher m = Pattern.compile("\"active\":\\[(.*?)]").matcher(res.body());
        Set<String> out = new HashSet<>();
        if (m.find()) {
            Matcher n = Pattern.compile("\"([A-Za-z0-9_]{3,16})\"").matcher(m.group(1));
            while (n.find()) out.add(n.group(1).toLowerCase(Locale.ROOT));
        }
        return out;
    }

    /** Puts Op Launcher players into a client-side team whose prefix is the logo. Servers' own teams are left alone. */
    private static void apply(Minecraft mc, List<String> names, Set<String> active) {
        ClientLevel level = mc.level;
        if (level == null) return;
        Scoreboard sb = level.getScoreboard();
        PlayerTeam team = sb.getPlayerTeam(TEAM);
        if (team == null) {
            team = sb.addPlayerTeam(TEAM);
            team.setPlayerPrefix(Component.literal(GLYPH));
        }
        for (String n : names) {
            PlayerTeam current = sb.getPlayersTeam(n);
            boolean isOp = active.contains(n.toLowerCase(Locale.ROOT));
            if (isOp && current == null) sb.addPlayerToTeam(n, team);
            else if (!isOp && current == team) sb.removePlayerFromTeam(n, team);
        }
    }
}
