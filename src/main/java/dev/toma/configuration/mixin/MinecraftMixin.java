package dev.toma.configuration.mixin;

import com.mojang.blaze3d.platform.WindowEventHandler;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.io.ConfigIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler, net.minecraftforge.client.extensions.IForgeMinecraft {

    public MinecraftMixin(String p_i50401_1_) {
        super(p_i50401_1_);
    }

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;resetData()V"))
    private void configuration_reloadClientConfigs(Screen screen, CallbackInfo ci) {
        ConfigHolder.getSynchronizedConfigs().stream()
                .map(ConfigHolder::getConfig)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(ConfigIO::reloadClientValues);
    }
}
