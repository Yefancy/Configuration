package dev.toma.configuration.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.client.DisplayAdapter;
import dev.toma.configuration.client.DisplayAdapterManager;
import dev.toma.configuration.client.widget.ConfigEntryWidget;
import dev.toma.configuration.config.adapter.TypeAdapter;
import dev.toma.configuration.config.adapter.TypeAdapters;
import dev.toma.configuration.config.validate.NotificationSeverity;
import dev.toma.configuration.config.value.ArrayValue;
import dev.toma.configuration.config.value.ConfigValue;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ArrayConfigScreen<V, C extends ConfigValue<V> & ArrayValue> extends AbstractConfigScreen {

    public static final ITextComponent ADD_ELEMENT = new TranslationTextComponent("text.configuration.value.add_element");

    public final C array;
    private final boolean fixedSize;

    private Supplier<Integer> sizeSupplier = () -> 0;
    private DummyConfigValueFactory valueFactory;
    private ElementAddHandler addHandler;
    private ElementRemoveHandler<V> removeHandler;

    public ArrayConfigScreen(String ownerIdentifier, String configId, C array, Screen previous) {
        super(new TranslationTextComponent("config.screen." + ownerIdentifier), previous, configId);
        this.array = array;
        this.fixedSize = array.isFixedSize();
    }

    public void fetchSize(Supplier<Integer> integerSupplier) {
        this.sizeSupplier = integerSupplier;
    }

    public void valueFactory(DummyConfigValueFactory factory) {
        this.valueFactory = factory;
    }

    public void addElement(ElementAddHandler handler) {
        this.addHandler = handler;
    }

    public void removeElement(ElementRemoveHandler<V> handler) {
        this.removeHandler = handler;
    }

    @Override
    protected void init() {
        final int viewportMin = HEADER_HEIGHT;
        final int viewportHeight = this.height - viewportMin - FOOTER_HEIGHT;
        this.pageSize = (viewportHeight - 20) / 25;
        this.correctScrollingIndex(this.sizeSupplier.get());
        int errorOffset = (viewportHeight - 20) - (this.pageSize * 25 - 5);
        int offset = 0;

        Class<?> compType = array.get().getClass().getComponentType();
        DisplayAdapter adapter = DisplayAdapterManager.forType(compType);
        TypeAdapter.AdapterContext context = array.getSerializationContext();
        Field owner = context.getOwner();
        for (int i = this.index; i < this.index + this.pageSize; i++) {
            int j = i - this.index;
            if (i >= this.sizeSupplier.get())
                break;
            int correct = errorOffset / (this.pageSize - j);
            errorOffset -= correct;
            offset += correct;
            ConfigValue<?> dummy = valueFactory.create(array.getId(), i);
            dummy.processFieldData(owner);
            ConfigEntryWidget widget = addButton(new ConfigEntryWidget(30, viewportMin + 10 + j * 25 + offset, this.width - 60, 20, dummy, this.configId));
            widget.setDescriptionRenderer(this::renderEntryDescription);
            if (adapter == null) {
                Configuration.LOGGER.error(MARKER, "Missing display adapter for {} type, will not be displayed in GUI", compType.getSimpleName());
                continue;
            }
            try {
                adapter.placeWidgets(dummy, owner, widget);
                initializeGuiValue(dummy, widget);
            } catch (ClassCastException e) {
                Configuration.LOGGER.error(MARKER, "Unable to create config field for {} type due to error {}", compType.getSimpleName(), e);
            }
            if (!fixedSize) {
                final int elementIndex = i;
                addButton(new Button(this.width - 28, widget.y, 20, 20, new StringTextComponent("x"), btn -> {
                    this.removeHandler.removeElementAt(elementIndex, (index, src, dest) -> {
                        System.arraycopy(src, 0, dest, 0, index);
                        System.arraycopy(src, index + 1, dest, index, this.sizeSupplier.get() - 1 - index);
                        return dest;
                    });
                    this.init(minecraft, width, height);
                }));
            }
        }
        addFooter();
    }

    private void renderEntryDescription(MatrixStack stack, int mouseX, int mouseY, Widget widget, NotificationSeverity severity, List<IReorderingProcessor> text) {
        if (!severity.isOkStatus()) {
            this.renderNotification(severity, stack, text, widget.x + 5, widget.y + widget.getHeight() + 10);
        }
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(stack);
        // HEADER
        int titleWidth = this.font.width(this.title);
        font.draw(stack, this.title, (this.width - titleWidth) / 2.0F, (HEADER_HEIGHT - this.font.lineHeight) / 2.0F, 0xFFFFFF);
        fill(stack, 0, HEADER_HEIGHT, width, height - FOOTER_HEIGHT, 0x99 << 24);
        renderScrollbar(stack, width - 5, HEADER_HEIGHT, 5, height - FOOTER_HEIGHT - HEADER_HEIGHT, index, sizeSupplier.get(), pageSize);
        super.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void addFooter() {
        super.addFooter();
        if (!this.fixedSize) {
            int centerY = this.height - FOOTER_HEIGHT + (FOOTER_HEIGHT - 20) / 2;
            addButton(new Button(width - 20 - 80, centerY, 80, 20, ADD_ELEMENT, btn -> {
                this.addHandler.insertElement();
                this.init(minecraft, width, height);
            }));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int scale = (int) -amount;
        int next = this.index + scale;
        if (next >= 0 && next + this.pageSize <= this.sizeSupplier.get()) {
            this.index = next;
            this.init(minecraft, width, height);
            return true;
        }
        return false;
    }

    public static <V> TypeAdapter.AdapterContext callbackCtx(Field parent, Class<V> componentType, BiConsumer<V, Integer> callback, int index) {
        return new DummyCallbackAdapter<>(componentType, parent, callback, index);
    }

    @FunctionalInterface
    public interface ElementAddHandler {
        void insertElement();
    }

    @FunctionalInterface
    public interface DummyConfigValueFactory {
        ConfigValue<?> create(String id, int elementIndex);
    }

    @FunctionalInterface
    public interface ElementRemoveHandler<V> {
        void removeElementAt(int index, ArrayTrimmer<V> trimmer);

        @FunctionalInterface
        interface ArrayTrimmer<V> {
            V trim(int index, V src, V dest);
        }
    }

    private static class DummyCallbackAdapter<V> implements TypeAdapter.AdapterContext {

        private final TypeAdapter typeAdapter;
        private final Field parentField;
        private final BiConsumer<V, Integer> setCallback;
        private final int index;

        private DummyCallbackAdapter(Class<V> type, Field parentField, BiConsumer<V, Integer> setCallback, int index) {
            this.typeAdapter = TypeAdapters.forType(type);
            this.parentField = parentField;
            this.setCallback = setCallback;
            this.index = index;
        }

        @Override
        public TypeAdapter getAdapter() {
            return typeAdapter;
        }

        @Override
        public Field getOwner() {
            return parentField;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setFieldValue(Object value) {
            this.setCallback.accept((V) value, this.index);
        }
    }
}
