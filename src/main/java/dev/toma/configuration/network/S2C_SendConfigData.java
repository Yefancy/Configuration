package dev.toma.configuration.network;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.adapter.TypeAdapter;
import dev.toma.configuration.config.value.ConfigValue;
import dev.toma.configuration.network.api.IClientPacket;
import dev.toma.configuration.network.api.IPacketDecoder;
import dev.toma.configuration.network.api.IPacketEncoder;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class S2C_SendConfigData implements IClientPacket<S2C_SendConfigData.ConfigData> {

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(Configuration.MODID, "s2c_send_config_data");

    private final ConfigData config;

    S2C_SendConfigData() {
        this.config = null;
    }

    public S2C_SendConfigData(String config) {
        this.config = new ConfigData(config);
    }

    @Override
    public ResourceLocation getPacketId() {
        return IDENTIFIER;
    }

    @Override
    public ConfigData getPacketData() {
        return config;
    }

    @Override
    public IPacketEncoder<ConfigData> getEncoder() {
        return (configData, buffer) -> {
            buffer.writeUtf(configData.configId);
            ConfigHolder.getConfig(configData.configId).ifPresent(data -> {
                Map<String, ConfigValue<?>> serialized = data.getNetworkSerializedFields();
                buffer.writeInt(serialized.size());
                for (Map.Entry<String, ConfigValue<?>> entry : serialized.entrySet()) {
                    String id = entry.getKey();
                    ConfigValue<?> value = entry.getValue();
                    TypeAdapter adapter = value.getAdapter();
                    buffer.writeUtf(id);
                    adapter.encodeToBuffer(value, buffer);
                }
            });
        };
    }

    @Override
    public IPacketDecoder<ConfigData> getDecoder() {
        return buffer -> {
            String config = buffer.readUtf();
            int i = buffer.readInt();
            ConfigHolder.getConfig(config).ifPresent(data -> {
                Map<String, ConfigValue<?>> serialized = data.getNetworkSerializedFields();
                for (int j = 0; j < i; j++) {
                    String fieldId = buffer.readUtf();
                    ConfigValue<?> value = serialized.get(fieldId);
                    if (value == null) {
                        Configuration.LOGGER.fatal(Networking.MARKER, "Received unknown config value " + fieldId);
                        throw new RuntimeException("Unknown config field: " + fieldId);
                    }
                    setValue(value, buffer);
                }
            });
            return new ConfigData(config);
        };
    }

    @Override
    public void handleClientsidePacket(Minecraft client, ClientPacketListener listener, ConfigData packetData, PacketSender dispatcher) {
    }

    @SuppressWarnings("unchecked")
    private <V> void setValue(ConfigValue<V> value, FriendlyByteBuf buffer) {
        TypeAdapter adapter = value.getAdapter();
        V v = (V) adapter.decodeFromBuffer(value, buffer);
        value.set(v);
    }

    static final class ConfigData {

        private final String configId;

        public ConfigData(String configId) {
            this.configId = configId;
        }
    }
}
