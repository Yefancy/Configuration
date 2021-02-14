package dev.toma.configuration.api.client;

import dev.toma.configuration.api.client.screen.CollectionScreen;
import dev.toma.configuration.api.client.screen.ComponentScreen;
import dev.toma.configuration.api.client.screen.ConfigScreen;
import dev.toma.configuration.api.type.AbstractConfigType;
import dev.toma.configuration.api.type.CollectionType;
import dev.toma.configuration.api.type.ObjectType;
import net.minecraft.client.gui.screen.Screen;

/**
 * Interface for handling client stuff like UIs or text rendering.
 * Thanks to this configs are highly customizable
 *
 * @author Toma
 */
public interface ClientHandles {

    /**
     * Handles construction of new screen for objects.
     * This is also the very first screen you'll see when opening your config
     *
     * @param screen Previous screen
     * @param type Config object to be displayed
     * @param iModID Object containing your mod ID
     * @return New screen instance
     * @see ConfigScreen for default implementation
     */
    ComponentScreen createConfigScreen(Screen screen, ObjectType type, IModID iModID);

    /**
     * Handles construction of new screen for collections.
     *
     * @param parentScreen Previous screen
     * @param type Config object to be displayed
     * @param iModID Object containing your mod ID
     * @param <T> Type of contained elements
     * @return New screen instance
     * @see CollectionScreen for default implementation
     */
    <T extends AbstractConfigType<?>> ComponentScreen createCollectionScreen(Screen parentScreen, CollectionType<T> type, IModID iModID);

    /**
     * Allows you to change color of config entry names
     * when using lighter background textures to maintain
     * text visibility
     *
     * @return color in ARGB format
     */
    int getTextColor();

    class DefaultClientHandles implements ClientHandles {

        public static final DefaultClientHandles DEFAULT_CLIENT_HANDLES = new DefaultClientHandles();

        @Override
        public ComponentScreen createConfigScreen(Screen screen, ObjectType type, IModID iModID) {
            return new ConfigScreen(screen, type, iModID.getModID(), getTextColor());
        }

        @Override
        public <T extends AbstractConfigType<?>> ComponentScreen createCollectionScreen(Screen parentScreen, CollectionType<T> type, IModID iModID) {
            return new CollectionScreen<>(parentScreen, type, iModID.getModID(), getTextColor());
        }

        @Override
        public int getTextColor() {
            return 0x999999;
        }
    }
}