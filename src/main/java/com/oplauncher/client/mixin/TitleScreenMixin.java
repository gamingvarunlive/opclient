package com.oplauncher.client.mixin;

import com.oplauncher.client.OpTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Shows the Op Client main menu wherever Minecraft would open its own title screen. */
@Mixin(Minecraft.class)
public abstract class TitleScreenMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 0)
    private Screen op$replaceTitle(Screen screen) {
        if (screen != null && screen.getClass() == TitleScreen.class && !"false".equalsIgnoreCase(System.getProperty("op.menu"))) {
            return new OpTitleScreen();
        }
        return screen;
    }
}
