package dev.toma.configuration.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.client.IValidationHandler;
import dev.toma.configuration.client.widget.ConfigEntryWidget;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.io.ConfigIO;
import dev.toma.configuration.config.validate.NotificationSeverity;
import dev.toma.configuration.config.value.ConfigValue;
import dev.toma.configuration.config.value.ObjectValue;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.List;

public abstract class AbstractConfigScreen extends Screen {

    public static final int HEADER_HEIGHT = 35;
    public static final int FOOTER_HEIGHT = 30;
    public static final Marker MARKER = MarkerManager.getMarker("Screen");
    protected final Screen last;
    protected final String configId;

    protected int index;
    protected int pageSize;

    public AbstractConfigScreen(Component title, Screen previous, String configId) {
        super(title);
        this.last = previous;
        this.configId = configId;
    }

    @Override
    public void onClose() {
        super.onClose();
        this.saveConfig(true);
    }

    public static void renderScrollbar(GuiGraphics graphics, int x, int y, int width, int height, int index, int valueCount, int paging) {
        if (valueCount <= paging)
            return;
        double step = height / (double) valueCount;
        int min = Mth.floor(index * step);
        int max = Mth.ceil((index + paging) * step);
        int y1 = y + min;
        int y2 = y + max;
        graphics.fill(x, y, x + width, y + height, 0xFF << 24);

        graphics.fill(x, y1, x + width, y2, 0xFF888888);
        graphics.fill(x, y1, x + width - 1, y2 - 1, 0xFFEEEEEE);
        graphics.fill(x + 1, y1 + 1, x + width - 1, y2 - 1, 0xFFCCCCCC);
    }

    protected void addFooter() {
        int centerY = this.height - FOOTER_HEIGHT + (FOOTER_HEIGHT - 20) / 2;
        addRenderableWidget(Button.builder(ConfigEntryWidget.BACK, this::buttonBackClicked).pos(20, centerY).size(50, 20).build());
        addRenderableWidget(Button.builder(ConfigEntryWidget.REVERT_DEFAULTS, this::buttonRevertToDefaultClicked).pos(75, centerY).size(120, 20).build());
        addRenderableWidget(Button.builder(ConfigEntryWidget.REVERT_CHANGES, this::buttonRevertChangesClicked).pos(200, centerY).size(120, 20).build());
    }

    protected void correctScrollingIndex(int count) {
        if (index + pageSize > count) {
            index = Math.max(count - pageSize, 0);
        }
    }

    protected Screen getFirstNonConfigScreen() {
        Screen screen = last;
        while (screen instanceof ConfigScreen configScreen) {
            screen = configScreen.last;
        }
        return screen;
    }

    private void buttonBackClicked(Button button) {
        this.minecraft.setScreen(this.last);
        this.saveConfig();
    }

    private void buttonRevertToDefaultClicked(Button button) {
        DialogScreen dialog = new DialogScreen(ConfigEntryWidget.REVERT_DEFAULTS, new Component[] {ConfigEntryWidget.REVERT_DEFAULTS_DIALOG_TEXT}, this);
        dialog.onConfirmed(screen -> {
            Configuration.LOGGER.info(MARKER, "Reverting config {} to default values", this.configId);
            ConfigHolder.getConfig(this.configId).ifPresent(holder -> {
                revertToDefault(holder.values());
                ConfigIO.saveClientValues(holder);
            });
            this.backToConfigList();
        });
        minecraft.setScreen(dialog);
    }

    private void buttonRevertChangesClicked(Button button) {
        DialogScreen dialog = new DialogScreen(ConfigEntryWidget.REVERT_CHANGES, new Component[] {ConfigEntryWidget.REVERT_CHANGES_DIALOG_TEXT}, this);
        dialog.onConfirmed(screen -> {
            ConfigHolder.getConfig(this.configId).ifPresent(ConfigIO::reloadClientValues);
            this.backToConfigList();
        });
        minecraft.setScreen(dialog);
    }

    private void revertToDefault(Collection<ConfigValue<?>> configValues) {
        configValues.forEach(val -> {
            if (val instanceof ObjectValue objVal) {
                this.revertToDefault(objVal.get().values());
            } else {
                val.useDefaultValue();
            }
        });
    }

    private void backToConfigList() {
        this.minecraft.setScreen(this.getFirstNonConfigScreen());
        this.saveConfig();
    }

    private void saveConfig() {
        saveConfig(false);
    }

    private void saveConfig(boolean force) {
        if (force || !(last instanceof AbstractConfigScreen)) {
            ConfigHolder.getConfig(this.configId).ifPresent(ConfigIO::saveClientValues);
        }
    }

    private void fillGradient(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k, int l, int m, int n, int o) {
        float f = (float) FastColor.ARGB32.alpha(n) / 255.0F;
        float g = (float) FastColor.ARGB32.red(n) / 255.0F;
        float h = (float) FastColor.ARGB32.green(n) / 255.0F;
        float p = (float) FastColor.ARGB32.blue(n) / 255.0F;
        float q = (float) FastColor.ARGB32.alpha(o) / 255.0F;
        float r = (float) FastColor.ARGB32.red(o) / 255.0F;
        float s = (float) FastColor.ARGB32.green(o) / 255.0F;
        float t = (float) FastColor.ARGB32.blue(o) / 255.0F;
        Matrix4f matrix4f = poseStack.last().pose();
        vertexConsumer.vertex(matrix4f, (float)i, (float)j, (float)m).color(g, h, p, f).endVertex();
        vertexConsumer.vertex(matrix4f, (float)i, (float)l, (float)m).color(r, s, t, q).endVertex();
        vertexConsumer.vertex(matrix4f, (float)k, (float)l, (float)m).color(r, s, t, q).endVertex();
        vertexConsumer.vertex(matrix4f, (float)k, (float)j, (float)m).color(g, h, p, f).endVertex();
    }

    public void renderNotification(NotificationSeverity severity, GuiGraphics graphics, List<FormattedCharSequence> texts, int mouseX, int mouseY) {
        if (!texts.isEmpty()) {
            int maxTextWidth = 0;
            int iconOffset = 13;
            for(FormattedCharSequence textComponent : texts) {
                int textWidth = this.font.width(textComponent);
                if (!severity.isOkStatus()) {
                    textWidth += iconOffset;
                }
                if (textWidth > maxTextWidth) {
                    maxTextWidth = textWidth;
                }
            }

            int startX = mouseX + 12;
            int startY = mouseY - 12;
            int heightOffset = 8;
            if (texts.size() > 1) {
                heightOffset += 2 + (texts.size() - 1) * 10;
            }

            if (startX + maxTextWidth > this.width) {
                startX -= 28 + maxTextWidth;
            }

            if (startY + heightOffset + 6 > this.height) {
                startY = this.height - heightOffset - 6;
            }

            graphics.pose().pushPose();
            int background = severity.background;
            int fadeMin = severity.fadeMin;
            int fadeMax = severity.fadeMax;
            int zIndex = 400;
            Tesselator tessellator = Tesselator.getInstance();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder bufferbuilder = tessellator.getBuilder();
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            fillGradient(graphics.pose(), bufferbuilder, startX - 3, startY + heightOffset + 3, startX + maxTextWidth + 3, startY + heightOffset + 4, zIndex, background, background);
            fillGradient(graphics.pose(), bufferbuilder, startX - 3, startY - 3, startX + maxTextWidth + 3, startY + heightOffset + 3, zIndex, background, background);
            fillGradient(graphics.pose(), bufferbuilder, startX - 4, startY - 3, startX - 3, startY + heightOffset + 3, zIndex, background, background);
            fillGradient(graphics.pose(), bufferbuilder, startX + maxTextWidth + 3, startY - 3, startX + maxTextWidth + 4, startY + heightOffset + 3, zIndex, background, background);
            fillGradient(graphics.pose(), bufferbuilder, startX - 3, startY - 3 + 1, startX - 3 + 1, startY + heightOffset + 3 - 1, zIndex, fadeMin, fadeMax);
            fillGradient(graphics.pose(), bufferbuilder, startX + maxTextWidth + 2, startY - 3 + 1, startX + maxTextWidth + 3, startY + heightOffset + 3 - 1, zIndex, fadeMin, fadeMax);
            fillGradient(graphics.pose(), bufferbuilder, startX - 3, startY - 3, startX + maxTextWidth + 3, startY - 3 + 1, zIndex, fadeMin, fadeMin);
            fillGradient(graphics.pose(), bufferbuilder, startX - 3, startY + heightOffset + 2, startX + maxTextWidth + 3, startY + heightOffset + 3, zIndex, fadeMax, fadeMax);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            BufferUploader.drawWithShader(bufferbuilder.end());

            if (!severity.isOkStatus()) {
                ResourceLocation icon = severity.getIcon();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, icon);
                bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                float min = -0.5f;
                float max = 8.5f;
                var matrix4f = graphics.pose().last().pose();
                bufferbuilder.vertex(matrix4f, startX + min, startY + min, zIndex).uv(0.0F, 0.0F).endVertex();
                bufferbuilder.vertex(matrix4f, startX + min, startY + max, zIndex).uv(0.0F, 1.0F).endVertex();
                bufferbuilder.vertex(matrix4f, startX + max, startY + max, zIndex).uv(1.0F, 1.0F).endVertex();
                bufferbuilder.vertex(matrix4f, startX + max, startY + min, zIndex).uv(1.0F, 0.0F).endVertex();
                BufferUploader.drawWithShader(bufferbuilder.end());
            }


            RenderSystem.disableBlend();
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            graphics.pose().translate(0.0D, 0.0D, zIndex);

            int textOffset = severity.isOkStatus() ? 0 : iconOffset;
            for(int i = 0; i < texts.size(); i++) {
                FormattedCharSequence textComponent = texts.get(i);
                if (textComponent != null) {
                    this.font.drawInBatch(textComponent, (float)startX + textOffset, (float)startY, -1, true, graphics.pose().last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 0xf000f0);
                }

                if (i == 0) {
                    startY += 2;
                }

                startY += 10;
            }

            bufferSource.endBatch();
            graphics.pose().popPose();
        }
    }

    protected <T> void initializeGuiValue(ConfigValue<T> value, IValidationHandler handler) {
        T t = value.get();
        value.setWithValidationHandler(t, handler);
    }
}
