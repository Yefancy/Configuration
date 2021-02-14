package dev.toma.configuration.client.screen.component;

import dev.toma.configuration.api.type.AbstractConfigType;
import dev.toma.configuration.api.type.CollectionType;
import dev.toma.configuration.client.screen.ComponentScreen;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextFormatting;

public class AddCollectionElementComponent<T extends AbstractConfigType<?>> extends Component {

    final ComponentScreen screen;
    final CollectionType<T> configType;

    public AddCollectionElementComponent(ComponentScreen screen, CollectionType<T> type, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.screen = screen;
        this.configType = type;
    }

    @Override
    public void processClicked(double mouseX, double mouseY) {
        T element = configType.createElement();
        configType.add(element);
        screen.scheduleUpdate(screen1 -> screen1.init(screen1.getMinecraft(), screen1.width, screen1.height));
    }

    @Override
    public void drawComponent(FontRenderer font, int mouseX, int mouseY, float partialTicks, boolean hovered) {
        drawColorShape(x, y, x + width, y + height, 1.0F, 1.0F, 1.0F, 1.0F);
        drawColorShape(x + 1, y + 1, x + width - 1, y + height - 1, 0.0F, 0.0F, 0.0F, 1.0F);
        int tw = font.getStringWidth(TextFormatting.BOLD + "+");
        font.drawStringWithShadow(TextFormatting.BOLD + "+", x + (width - tw) / 2.0F, y + (height - font.FONT_HEIGHT) / 2.0F, hovered ? 0xFFFF00 : 0xFFFFFF);
    }
}