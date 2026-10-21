package com.oplauncher.client.mixin;

import com.oplauncher.client.OpFavorites;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

/**
 * Adds a Quick Play button to the Multiplayer screen: select a server, click the button, and it is added to
 * (or removed from) the Quick Play list in Op Launcher. The button shows the state of the selected server.
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class FavoriteButtonMixin extends Screen {
    @Shadow protected ServerSelectionList serverSelectionList;

    private Button op$button;
    private ServerData op$shown;      // the server the button label was written for
    private boolean op$shownKnown;

    protected FavoriteButtonMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void op$addQuickPlayButton(CallbackInfo ci) {
        if (!OpFavorites.enabled()) return;
        op$shownKnown = false;
        op$button = Button.builder(Component.literal("☆ Quick Play"), btn -> {
            ServerData data = op$selected();
            if (data == null) return;
            try {
                OpFavorites.toggle(data.name, data.ip);
            } catch (Exception ignored) {
                // address not supported / list full: the label below stays as it was
            }
            op$refreshLabel(data);
        }).bounds(6, 6, 150, 20).build();
        this.addRenderableWidget(op$button);
    }

    /** Keeps the label in step with the selected server (checked every frame, the favorites file is read only when the selection changes). */
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void op$updateLabel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (op$button == null) return;
        ServerData cur = op$selected();
        if (!op$shownKnown || cur != op$shown) op$refreshLabel(cur);
    }

    private void op$refreshLabel(ServerData data) {
        op$shown = data;
        op$shownKnown = true;
        if (op$button == null) return;
        if (data == null) {
            op$button.setMessage(Component.literal("☆ Quick Play"));
            op$button.active = false;
            return;
        }
        op$button.active = OpFavorites.isValidIp(data.ip);
        boolean fav = OpFavorites.isValidIp(data.ip) && OpFavorites.contains(data.ip);
        op$button.setMessage(Component.literal(fav ? "★ In Quick Play (remove)" : "☆ Add to Quick Play"));
    }

    private ServerData op$selected() {
        try {
            ServerSelectionList.Entry sel = this.serverSelectionList.getSelected();
            if (sel instanceof ServerSelectionList.OnlineServerEntry online) return online.getServerData();
        } catch (Throwable ignored) {
            // different internals on this version: no state
        }
        return null;
    }
}
