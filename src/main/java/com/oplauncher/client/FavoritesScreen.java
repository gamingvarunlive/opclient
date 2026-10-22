package com.oplauncher.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * "Quick Play favorites": lists the servers of the Multiplayer list. Pick one, then Add / Remove it from the
 * favorites that Op Launcher shows in its Quick Play bar. Only plain buttons are used so it works on every version.
 */
public class FavoritesScreen extends Screen {
    private static final int PER_PAGE = 6;

    private final Screen parent;
    private final List<ServerData> servers = new ArrayList<>();
    private Set<String> favs;
    private int selected = -1;
    private int page = 0;

    public FavoritesScreen(Screen parent) {
        super(Component.literal("Quick Play favorites"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        servers.clear();
        try {
            ServerList list = new ServerList(Minecraft.getInstance());
            list.load();
            for (int i = 0; i < list.size(); i++) servers.add(list.get(i));
        } catch (Throwable ignored) {
            // no readable server list: the screen just shows the empty message
        }
        favs = OpFavorites.ips();
        if (selected >= servers.size()) selected = -1;
        rebuild();
    }

    private static String key(ServerData d) {
        return String.valueOf(d.ip).toLowerCase(Locale.ROOT);
    }

    private void rebuild() {
        this.clearWidgets();
        final int cx = this.width / 2;

        Button title = Button.builder(Component.literal("Quick Play favorites"), b -> { }).bounds(cx - 150, 12, 300, 20).build();
        title.active = false;
        this.addRenderableWidget(title);

        if (servers.isEmpty()) {
            Button none = Button.builder(Component.literal("No servers yet. Add a server in Multiplayer first."), b -> { }).bounds(cx - 150, 44, 300, 20).build();
            none.active = false;
            this.addRenderableWidget(none);
        }

        int pages = Math.max(1, (servers.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        int y = 40;
        for (int i = page * PER_PAGE; i < Math.min(servers.size(), (page + 1) * PER_PAGE); i++) {
            final int index = i;
            ServerData d = servers.get(i);
            boolean fav = favs.contains(key(d));
            Component star = fav ? Component.literal("★ ").withStyle(ChatFormatting.GOLD) : Component.literal("☆ ").withStyle(ChatFormatting.GRAY);
            Component label = Component.literal(index == selected ? "> " : "  ").append(star).append(Component.literal(String.valueOf(d.name)));
            Button row = Button.builder(label, b -> {
                selected = index;
                rebuild();
            }).bounds(cx - 150, y, 300, 20).build();
            this.addRenderableWidget(row);
            y += 22;
        }

        if (servers.size() > PER_PAGE) {
            Button prev = Button.builder(Component.literal("<"), b -> { page--; selected = -1; rebuild(); }).bounds(cx - 150, y + 2, 24, 20).build();
            prev.active = page > 0;
            Button info = Button.builder(Component.literal("Page " + (page + 1) + " / " + pages), b -> { }).bounds(cx - 120, y + 2, 240, 20).build();
            info.active = false;
            Button next = Button.builder(Component.literal(">"), b -> { page++; selected = -1; rebuild(); }).bounds(cx + 126, y + 2, 24, 20).build();
            next.active = page < pages - 1;
            this.addRenderableWidget(prev);
            this.addRenderableWidget(info);
            this.addRenderableWidget(next);
        }

        // the action button changes with the selected server
        Component actionText;
        boolean actionOn = false;
        if (selected < 0 || selected >= servers.size()) {
            actionText = Component.literal("Select a server above");
        } else {
            ServerData d = servers.get(selected);
            if (!OpFavorites.isValidIp(d.ip)) actionText = Component.literal("This address is not supported");
            else {
                actionOn = true;
                actionText = Component.literal(favs.contains(key(d)) ? "Remove from favorites" : "Add to favorites");
            }
        }
        Button action = Button.builder(actionText, b -> toggleSelected()).bounds(cx - 100, this.height - 54, 200, 20).build();
        action.active = actionOn;
        this.addRenderableWidget(action);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose()).bounds(cx - 100, this.height - 30, 200, 20).build());
    }

    private void toggleSelected() {
        if (selected < 0 || selected >= servers.size()) return;
        ServerData d = servers.get(selected);
        try {
            boolean now = OpFavorites.toggle(d.name, d.ip);
            if (now) favs.add(key(d)); else favs.remove(key(d));
        } catch (Exception ignored) {
            // list full or file not writable: nothing changes
        }
        rebuild();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
