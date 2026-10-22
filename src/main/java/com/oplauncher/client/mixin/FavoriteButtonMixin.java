package com.oplauncher.client.mixin;

import com.oplauncher.client.OpFavorites;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A star at the bottom right of every server in the Multiplayer list. Click it: the server is added to Quick Play in
 * Op Launcher and the star turns gold. Click again: it is removed.
 *
 * The stars are normal buttons that follow their server row while the list scrolls. If this Minecraft version lays the
 * list out differently, a single "Quick Play" button at the top left is used instead (it acts on the selected server).
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class FavoriteButtonMixin extends Screen {
    private static final int ROW_HEIGHT = 36;
    private static final int STAR = 14;

    @Shadow protected ServerSelectionList serverSelectionList;

    private final List<Button> op$stars = new ArrayList<>();
    private final List<ServerData> op$bound = new ArrayList<>();
    private Set<String> op$favs = new HashSet<>();
    private boolean op$ready;
    private boolean op$broken;
    private Button op$fallback;

    protected FavoriteButtonMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void op$init(CallbackInfo ci) {
        op$stars.clear();
        op$bound.clear();
        op$broken = false;
        op$fallback = null;
        op$ready = OpFavorites.enabled();
        op$favs = OpFavorites.ips();
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void op$layoutStars(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!op$ready || op$broken) return;
        try {
            ServerSelectionList list = this.serverSelectionList;
            List<ServerSelectionList.Entry> rows = list.children();
            int top = list.getY();
            int bottom = top + list.getHeight();
            int left = op$int(list, "getRowLeft", new Class<?>[0]);
            int width = op$int(list, "getRowWidth", new Class<?>[0]);
            int used = 0;
            for (int i = 0; i < rows.size(); i++) {
                if (!(rows.get(i) instanceof ServerSelectionList.OnlineServerEntry online)) continue;
                int rowTop = op$int(list, "getRowTop", new Class<?>[]{int.class}, i);
                Button star = op$star(used);
                ServerData data = online.getServerData();
                op$bound.set(used, data);
                int y = rowTop + ROW_HEIGHT - STAR - 4;
                star.setX(left + width - STAR - 4);
                star.setY(y);
                star.visible = y >= top && y + STAR <= bottom;
                boolean ok = OpFavorites.isValidIp(data.ip);
                star.active = ok;
                boolean fav = ok && op$favs.contains(data.ip.toLowerCase(Locale.ROOT));
                star.setMessage(op$label(fav));
                used++;
            }
            for (int j = used; j < op$stars.size(); j++) op$stars.get(j).visible = false;
        } catch (Throwable t) {
            op$broken = true;
            for (Button b : op$stars) b.visible = false;
            System.err.println("[OpClient] star buttons not available on this version, using the top button: " + t);
            op$addFallback();
        }
    }

    private Button op$star(int index) {
        while (op$stars.size() <= index) {
            final int slot = op$stars.size();
            Button b = Button.builder(op$label(false), btn -> op$toggle(op$bound.get(slot)))
                    .bounds(0, 0, STAR, STAR).build();
            op$stars.add(b);
            op$bound.add(null);
            this.addRenderableWidget(b);
            // clicks go to the stars before the list underneath them
            @SuppressWarnings({"rawtypes", "unchecked"})
            List kids = (List) this.children();
            kids.remove(b);
            kids.add(0, (GuiEventListener) b);
        }
        return op$stars.get(index);
    }

    private void op$toggle(ServerData data) {
        if (data == null || !OpFavorites.isValidIp(data.ip)) return;
        try {
            boolean now = OpFavorites.toggle(data.name, data.ip);
            String key = data.ip.toLowerCase(Locale.ROOT);
            if (now) op$favs.add(key); else op$favs.remove(key);
        } catch (Exception ignored) {
            // list full / file not writable: the star stays as it was
        }
    }

    private static Component op$label(boolean fav) {
        return fav ? Component.literal("★").withStyle(ChatFormatting.GOLD)
                   : Component.literal("☆").withStyle(ChatFormatting.GRAY);
    }

    private void op$addFallback() {
        if (op$fallback != null) return;
        op$fallback = Button.builder(Component.literal("☆ Quick Play"), btn -> {
            ServerData data = op$selected();
            if (data == null) { btn.setMessage(Component.literal("Select a server first")); return; }
            op$toggle(data);
            boolean fav = op$favs.contains(String.valueOf(data.ip).toLowerCase(Locale.ROOT));
            btn.setMessage(Component.literal(fav ? "★ Added" : "☆ Removed"));
        }).bounds(6, 6, 130, 20).build();
        this.addRenderableWidget(op$fallback);
    }

    private ServerData op$selected() {
        try {
            ServerSelectionList.Entry sel = this.serverSelectionList.getSelected();
            if (sel instanceof ServerSelectionList.OnlineServerEntry online) return online.getServerData();
        } catch (Throwable ignored) {
            // different internals on this version
        }
        return null;
    }

    /** The list's row helpers are protected on some versions, so they are called by reflection. */
    private static int op$int(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, types);
                m.setAccessible(true);
                return ((Number) m.invoke(target, args)).intValue();
            } catch (NoSuchMethodException ignored) {
                // try the parent class
            }
        }
        throw new NoSuchMethodException(name);
    }
}
