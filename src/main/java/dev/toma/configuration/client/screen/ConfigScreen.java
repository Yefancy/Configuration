package dev.toma.configuration.client.screen;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.client.DisplayAdapter;
import dev.toma.configuration.client.DisplayAdapterManager;
import dev.toma.configuration.client.widget.ConfigEntryWidget;
import dev.toma.configuration.config.adapter.TypeAdapter;
import dev.toma.configuration.config.validate.NotificationSeverity;
import dev.toma.configuration.config.value.ConfigValue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigScreen extends AbstractConfigScreen {

    private final Map<String, ConfigValue<?>> valueMap;

    public ConfigScreen(String ownerIdentifier, String configId, Map<String, ConfigValue<?>> valueMap, Screen previous) {
        this(Component.translatable("config.screen." + ownerIdentifier), configId, valueMap, previous);
    }

    public ConfigScreen(Component screenTitle, String configId, Map<String, ConfigValue<?>> valueMap, Screen previous) {
        super(screenTitle, previous, configId);
        this.valueMap = valueMap;
    }

    @Override
    protected void init() {
        final int viewportMin = HEADER_HEIGHT;
        final int viewportHeight = this.height - viewportMin - FOOTER_HEIGHT;
        this.pageSize = (viewportHeight - 20) / 25;
        this.correctScrollingIndex(this.valueMap.size());
        List<ConfigValue<?>> values = new ArrayList<>(this.valueMap.values());
        int errorOffset = (viewportHeight - 20) - (this.pageSize * 25 - 5);
        int offset = 0;
        for (int i = this.index; i < this.index + this.pageSize; i++) {
            int j = i - this.index;
            if (i >= values.size())
                break;
            int correct = errorOffset / (this.pageSize - j);
            errorOffset -= correct;
            offset += correct;
            ConfigValue<?> value = values.get(i);
            ConfigEntryWidget widget = addRenderableWidget(new ConfigEntryWidget(30, viewportMin + 10 + j * 25 + offset, this.width - 60, 20, value, this.configId));
            widget.setDescriptionRenderer(this::renderEntryDescription);
            TypeAdapter.AdapterContext context = value.getSerializationContext();
            Field field = context.getOwner();
            DisplayAdapter adapter = DisplayAdapterManager.forType(field.getType());
            if (adapter == null) {
                Configuration.LOGGER.error(MARKER, "Missing display adapter for {} type, will not be displayed in GUI", field.getType().getSimpleName());
                continue;
            }
            try {
                adapter.placeWidgets(value, field, widget);
                initializeGuiValue(value, widget);
            } catch (ClassCastException e) {
                Configuration.LOGGER.error(MARKER, "Unable to create config field for {} type due to error {}", field.getType().getSimpleName(), e);
            }
        }
        this.addFooter();
    }

    private void renderEntryDescription(GuiGraphics graphics, AbstractWidget widget, NotificationSeverity severity, List<FormattedCharSequence> text) {
        int x = widget.getX() + 5;
        int y = widget.getY() + widget.getHeight() + 10;
        if (!severity.isOkStatus()) {
            this.renderNotification(severity, graphics, text, x, y);
        } else {
            this.renderNotification(NotificationSeverity.INFO, graphics, text, x, y);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        // HEADER
        int titleWidth = this.font.width(this.title);
        graphics.drawString(font, this.title, (int)((this.width - titleWidth) / 2.0), (int)((HEADER_HEIGHT - this.font.lineHeight) / 2.0), 0xFFFFFF);
        graphics.fill(0, HEADER_HEIGHT, width, height - FOOTER_HEIGHT, 0x99 << 24);
        renderScrollbar(graphics, width - 5, HEADER_HEIGHT, 5, height - FOOTER_HEIGHT - HEADER_HEIGHT, index, valueMap.size(), pageSize);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int scale = (int) -amount;
        int next = this.index + scale;
        if (next >= 0 && next + this.pageSize <= this.valueMap.size()) {
            this.index = next;
            this.init(minecraft, width, height);
            return true;
        }
        return false;
    }
}
