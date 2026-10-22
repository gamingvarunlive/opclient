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

import java.util.ArrayList;
import java.util.List;

/**
 * Op Client main menu: blurred background, Op wordmark and cyan buttons.
 * The real (vanilla) buttons only handle the clicks; the look is drawn on top of them in render().
 */
public class OpTitleScreen extends Screen {
    private static final Identifier BG = Identifier.fromNamespaceAndPath("opclient", "textures/gui/menu_bg.png");
    private static final Identifier WORDMARK = Identifier.fromNamespaceAndPath("opclient", "textures/gui/wordmark.png");

    private static final int CYAN = 0xFF22D3EE;
    private static final int PANEL = 0xD0081018;
    private static final int PANEL_HOVER = 0xE0103848;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFF9FB3BF;

    private static final int BTN_W = 220, BTN_H = 24, GAP = 6;

    private static final class Item {
        final String label;
        final Runnable action;
        int x, y;
        Item(String label, Runnable action) { this.label = label; this.action = action; }
    }

    private final List<Item> items = new ArrayList<>();
    private final long opened = System.currentTimeMillis();

    public OpTitleScreen() {
        super(Component.literal("Op Client"));
    }

    @Override
    protected void init() {
        items.clear();
        final Minecraft mc = Minecraft.getInstance();
        final Screen self = this;
        items.add(new Item("Singleplayer", () -> mc.setScreen(new SelectWorldScreen(self))));
        items.add(new Item("Multiplayer", () -> mc.setScreen(new JoinMultiplayerScreen(self))));
        items.add(new Item("Favorites", () -> mc.setScreen(new FavoritesScreen(self, new ArrayList<>()))));
        items.add(new Item("Options", () -> mc.setScreen(new OptionsScreen(self, mc.options))));
        items.add(new Item("Quit Game", mc::stop));

        int total = items.size() * BTN_H + (items.size() - 1) * GAP;
        int top = Math.max(this.height / 2 - total / 2 + 22, 96);
        int x = this.width / 2 - BTN_W / 2;
        for (int i = 0; i < items.size(); i++) {
            final Item it = items.get(i);
            it.x = x;
            it.y = top + i * (BTN_H + GAP);
            // invisible hit area: the vanilla button gets clicks and keyboard focus, its look is covered in render()
            this.addRenderableWidget(Button.builder(Component.empty(), b -> it.action.run())
                    .bounds(it.x, it.y, BTN_W, BTN_H).build());
        }
    }

    /** our own background instead of the vanilla blur / panorama */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.blit(RenderPipelines.GUI_TEXTURED, BG, 0, 0, 0f, 0f, this.width, this.height, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);   // background + the (invisible-by-cover) buttons

        // wordmark
        int ww = Math.min(300, this.width - 40);
        int wh = ww * 220 / 900;
        int wy = Math.max(8, items.get(0).y - wh - 14);
        g.blit(RenderPipelines.GUI_TEXTURED, WORDMARK, this.width / 2 - ww / 2, wy, 0f, 0f, ww, wh, ww, wh);

        double pulse = 0.5 + 0.5 * Math.sin((System.currentTimeMillis() - opened) / 450.0);
        int glowA = 0x40 + (int) (pulse * 0x50);

        for (Item it : items) {
            boolean hover = mouseX >= it.x && mouseX < it.x + BTN_W && mouseY >= it.y && mouseY < it.y + BTN_H;
            boolean quit = it.label.equals("Quit Game");
            g.fill(it.x, it.y, it.x + BTN_W, it.y + BTN_H, hover ? PANEL_HOVER : PANEL);
            int edge = hover ? CYAN : (glowA << 24) | 0x0022D3EE;
            if (!hover && quit) edge = 0x60FFFFFF;
            g.fill(it.x, it.y, it.x + BTN_W, it.y + 1, edge);
            g.fill(it.x, it.y + BTN_H - 1, it.x + BTN_W, it.y + BTN_H, edge);
            g.fill(it.x, it.y, it.x + 1, it.y + BTN_H, edge);
            g.fill(it.x + BTN_W - 1, it.y, it.x + BTN_W, it.y + BTN_H, edge);
            if (hover) g.fill(it.x, it.y + 3, it.x + 3, it.y + BTN_H - 3, CYAN);   // accent bar on the left
            int tw = this.font.width(it.label);
            g.drawString(this.font, it.label, it.x + BTN_W / 2 - tw / 2, it.y + (BTN_H - 8) / 2, hover ? CYAN : WHITE, true);
        }

        String foot = "Op Client  |  Minecraft 1.21.11";
        g.drawString(this.font, foot, this.width / 2 - this.font.width(foot) / 2, this.height - 14, GREY, true);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
