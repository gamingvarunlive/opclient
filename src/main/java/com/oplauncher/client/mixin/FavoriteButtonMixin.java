package com.oplauncher.client.mixin;

import com.oplauncher.client.OpFavorites;
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
 * Adds a "Quick Play" star button to the Multiplayer screen: select a server, click the button, and it is added to
 * (or removed from) the Quick Play list in Op Launcher.
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class FavoriteButtonMixin extends Screen {
    @Shadow protected ServerSelectionList serverSelectionList;

    protected FavoriteButtonMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void op$addQuickPlayButton(CallbackInfo ci) {
        if (!OpFavorites.enabled()) return;
        final Button[] holder = new Button[1];
        holder[0] = Button.builder(Component.literal("☆ Quick Play"), btn -> {
            ServerData data = op$selected();
            if (data == null) {
                btn.setMessage(Component.literal("Select a server first"));
                return;
            }
            try {
                boolean now = OpFavorites.toggle(data.name, data.ip);
                btn.setMessage(Component.literal(now ? "★ Added to Quick Play" : "☆ Removed"));
            } catch (Exception e) {
                btn.setMessage(Component.literal("Not possible"));
            }
        }).bounds(6, 6, 130, 20).build();
        this.addRenderableWidget(holder[0]);
    }

    private ServerData op$selected() {
        try {
            ServerSelectionList.Entry sel = this.serverSelectionList.getSelected();
            if (sel instanceof ServerSelectionList.OnlineServerEntry online) return online.getServerData();
        } catch (Throwable ignored) {
            // different internals on this version: no star
        }
        return null;
    }
}
