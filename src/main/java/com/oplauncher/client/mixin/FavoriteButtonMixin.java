package com.oplauncher.client.mixin;

import com.oplauncher.client.FavoritesScreen;
import com.oplauncher.client.OpFavorites;
import net.minecraft.client.Minecraft;
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

import java.util.ArrayList;
import java.util.List;

/** Adds a "Favorites" button (top left) to the Multiplayer screen. It opens the Quick Play favorites menu. */
@Mixin(JoinMultiplayerScreen.class)
public abstract class FavoriteButtonMixin extends Screen {
    @Shadow protected ServerSelectionList serverSelectionList;

    protected FavoriteButtonMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void op$addFavoritesButton(CallbackInfo ci) {
        if (!OpFavorites.enabled()) return;
        final Screen self = this;
        this.addRenderableWidget(Button.builder(Component.literal("★ Favorites"), btn ->
                Minecraft.getInstance().setScreen(new FavoritesScreen(self, op$servers()))
        ).bounds(6, 6, 110, 20).build());
    }

    /** the servers of the list on this screen, in list order (empty if this version lays it out differently) */
    private List<ServerData> op$servers() {
        List<ServerData> out = new ArrayList<>();
        try {
            for (ServerSelectionList.Entry e : this.serverSelectionList.children()) {
                if (e instanceof ServerSelectionList.OnlineServerEntry online) out.add(online.getServerData());
            }
        } catch (Throwable ignored) {
            // the favorites menu then reads the saved server list itself
        }
        return out;
    }
}
