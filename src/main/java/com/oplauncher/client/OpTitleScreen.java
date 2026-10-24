package com.oplauncher.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Op Client main menu (same layout as FastClient, aqua accent): icon row top right, wordmark, menu buttons, Quit.
 * The vanilla buttons only take the clicks; the look is drawn on top of them in render().
 */
public class OpTitleScreen extends Screen {
    private static Identifier tex(String path) {
        return Identifier.fromNamespaceAndPath("opclient", "textures/gui/" + path + ".png");
    }

    private static final Identifier BG = tex("menu_bg");
    private static final Identifier WORDMARK = tex("wordmark");
    private static final Identifier LOGO = tex("logo");
    private static final Identifier DISCORD = tex("discord");
    private static final String DISCORD_URL = "https://discord.gg/zv2MjCwYaJ";

    private static final int CYAN = 0xFF22D3EE;
    private static final int PANEL = 0xC0101418;
    private static final int PANEL_HOVER = 0xD01A2A32;
    private static final int LINE = 0x40FFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFF9FB3BF;

    private static final int BTN_W = 176, BTN_H = 22, GAP = 3;
    private static final int TOP = 24, TOP_GAP = 7;

    private static final class Item {
        final String label, icon;       // icon may be null
        final boolean accent;
        final Runnable action;
        int x, y, w, h;
        Item(String label, String icon, boolean accent, Runnable action) {
            this.label = label; this.icon = icon; this.accent = accent; this.action = action;
        }
        boolean hit(int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Item> menu = new ArrayList<>();   // centre buttons
    private final List<Item> top = new ArrayList<>();    // top right row
    private Item quit;
    private Item discord;
    private Item chip;                                   // player name
    private String userName = "Player";

    public OpTitleScreen() {
        super(Component.literal("Op Client"));
    }

    private void place(Item it, int x, int y, int w, int h) {
        it.x = x; it.y = y; it.w = w; it.h = h;
        final Item f = it;
        this.addRenderableWidget(Button.builder(Component.empty(), b -> { if (f.action != null) f.action.run(); })
                .bounds(x, y, w, h).build());
    }

    @Override
    protected void init() {
        menu.clear(); top.clear();
        final Minecraft mc = Minecraft.getInstance();
        final Screen self = this;
        try { userName = mc.getUser().getName(); } catch (Throwable ignored) { userName = "Player"; }

        // centre
        menu.add(new Item("Singleplayer", "person", false, () -> mc.setScreen(new SelectWorldScreen(self))));
        menu.add(new Item("Multiplayer", "users", false, () -> mc.setScreen(new JoinMultiplayerScreen(self))));
        menu.add(new Item("Cosmetics", "hanger", false, null));
        // Mods opens the Mod Menu mod, so the button only exists when that mod is in this profile
        if (FabricLoader.getInstance().isModLoaded("modmenu")) menu.add(new Item("Mods", "grid", false, () -> openModMenu(mc, self)));
        menu.add(new Item("Store", "store", true, null));
        quit = new Item("Quit Game", null, true, mc::stop);
        discord = new Item("", null, false, () -> openLink(DISCORD_URL));

        int total = menu.size() * BTN_H + (menu.size() - 1) * GAP + 14 + 20;
        int y = Math.max(this.height / 2 - total / 2 + 24, 92);
        int x = this.width / 2 - BTN_W / 2;
        for (Item it : menu) { place(it, x, y, BTN_W, BTN_H); y += BTN_H + GAP; }
        place(quit, this.width / 2 - 45, y + 11, 90, 20);

        int dw = Math.min(112, this.width / 5), dh = dw * 125 / 202;
        place(discord, this.width - dw - Math.max(16, this.width / 28), this.height - dh - Math.max(16, this.height / 18), dw, dh);

        // top right: name, then the icon buttons, the last one is the accent logo button
        int nameW = Math.max(60, this.font.width(userName) + 34);
        String[] icons = {"gridplus", "chat", "camera", "gear", "diamond", "folder"};
        int n = icons.length + 1;
        int right = this.width - 10;
        int rowW = nameW + TOP_GAP + n * TOP + (n - 1) * TOP_GAP;
        int x0 = right - rowW;
        chip = new Item(userName, "person", false, null);
        place(chip, x0, 10, nameW, TOP);
        int ix = x0 + nameW + TOP_GAP;
        for (String ic : icons) {
            Item it = new Item("", ic, false, ic.equals("gear") ? () -> mc.setScreen(new OptionsScreen(self, mc.options)) : null);
            top.add(it);
            place(it, ix, 10, TOP, TOP);
            ix += TOP + TOP_GAP;
        }
        Item logo = new Item("", "LOGO", true, null);
        top.add(logo);
        place(logo, ix, 10, TOP, TOP);
    }

    /** our own background instead of the vanilla blur / panorama */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.blit(RenderPipelines.GUI_TEXTURED, BG, 0, 0, 0f, 0f, this.width, this.height, this.width, this.height);
    }

    private void box(GuiGraphics g, Item it, boolean hover) {
        int x2 = it.x + it.w, y2 = it.y + it.h;
        if (it.accent) {
            g.fillGradient(it.x, it.y, x2, y2, hover ? 0xFF6BEBFA : 0xFF2DD4EE, hover ? 0xFF1FB4D0 : 0xFF0C8CA6);
        } else {
            g.fill(it.x, it.y, x2, y2, hover ? PANEL_HOVER : PANEL);
            int edge = hover ? CYAN : LINE;
            g.fill(it.x, it.y, x2, it.y + 1, edge);
            g.fill(it.x, y2 - 1, x2, y2, edge);
            g.fill(it.x, it.y, it.x + 1, y2, edge);
            g.fill(x2 - 1, it.y, x2, y2, edge);
        }
    }

    private void icon(GuiGraphics g, String name, int x, int y, int size) {
        Identifier id = name.equals("LOGO") ? LOGO : tex("icons/" + name);
        g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, size, size, size, size);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);   // background + the buttons that the boxes below cover

        // wordmark
        int ww = Math.min(260, this.width - 40);
        int wh = ww * 220 / 900;
        int wy = Math.max(6, menu.get(0).y - wh - 10);
        g.blit(RenderPipelines.GUI_TEXTURED, WORDMARK, this.width / 2 - ww / 2, wy, 0f, 0f, ww, wh, ww, wh);

        // top row
        box(g, chip, chip.hit(mouseX, mouseY));
        icon(g, "person", chip.x + 8, chip.y + 6, 12);
        g.drawString(this.font, userName, chip.x + 24, chip.y + (chip.h - 8) / 2, WHITE, true);
        for (Item it : top) {
            box(g, it, it.hit(mouseX, mouseY));
            icon(g, it.icon, it.x + 4, it.y + 4, 16);
        }

        // menu
        for (Item it : menu) {
            boolean hover = it.hit(mouseX, mouseY);
            box(g, it, hover);
            int tw = this.font.width(it.label);
            int start = it.x + it.w / 2 - (12 + 5 + tw) / 2;
            icon(g, it.icon, start, it.y + (it.h - 12) / 2, 12);
            g.drawString(this.font, it.label, start + 17, it.y + (it.h - 8) / 2, WHITE, true);
        }
        boolean qh = quit.hit(mouseX, mouseY);
        box(g, quit, qh);
        int qw = this.font.width(quit.label);
        g.drawString(this.font, quit.label, quit.x + quit.w / 2 - qw / 2, quit.y + (quit.h - 8) / 2, WHITE, true);

        g.blit(RenderPipelines.GUI_TEXTURED, DISCORD, discord.x, discord.y, 0f, 0f, discord.w, discord.h, discord.w, discord.h);
        if (discord.hit(mouseX, mouseY)) {
            g.fill(discord.x, discord.y, discord.x + discord.w, discord.y + 1, CYAN);
            g.fill(discord.x, discord.y + discord.h - 1, discord.x + discord.w, discord.y + discord.h, CYAN);
            g.fill(discord.x, discord.y, discord.x + 1, discord.y + discord.h, CYAN);
            g.fill(discord.x + discord.w - 1, discord.y, discord.x + discord.w, discord.y + discord.h, CYAN);
        }

        String foot = "Op Client 1.0.9  |  Minecraft 1.21.11";
        g.drawString(this.font, foot, this.width / 2 - this.font.width(foot) / 2, quit.y + quit.h + 6, GREY, true);
    }

    /** Opens the Mod Menu screen by name (it is a separate mod, so no compile-time link to it). */
    private static void openModMenu(Minecraft mc, Screen parent) {
        try {
            Class<?> c = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            mc.setScreen((Screen) c.getConstructor(Screen.class).newInstance(parent));
        } catch (Throwable ignored) {
            // Mod Menu changed its screen: nothing happens
        }
    }

    private static void openLink(String url) {
        Thread t = new Thread(() -> {
            try {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                    return;
                }
            } catch (Throwable ignored) {
                // fall through to the OS command
            }
            try {
                String os = System.getProperty("os.name", "").toLowerCase();
                if (os.contains("win")) new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                else if (os.contains("mac")) new ProcessBuilder("open", url).start();
                else new ProcessBuilder("xdg-open", url).start();
            } catch (Throwable ignored) {
                // no browser available
            }
        }, "OpClient-open-link");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
