package dev.toma.configuration.config.format;

import dev.toma.configuration.config.value.ConfigValue;
import dev.toma.configuration.config.value.ICommentsProvider;
import dev.toma.configuration.exception.ConfigReadException;
import dev.toma.configuration.exception.ConfigValueMissingException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface IConfigFormat {

    void writeBoolean(String field, boolean value);

    boolean readBoolean(String field) throws ConfigValueMissingException;

    void writeChar(String field, char value);

    char readChar(String field) throws ConfigValueMissingException;

    void writeInt(String field, int value);

    int readInt(String field) throws ConfigValueMissingException;

    void writeString(String field, String value);

    String readString(String field) throws ConfigValueMissingException;

    void writeIntArray(String field, int[] values);

    int[] readIntArray(String field) throws ConfigValueMissingException;

    <E extends Enum<E>> void writeEnum(String field, E value);

    <E extends Enum<E>> E readEnum(String field, Class<E> enumClass) throws ConfigValueMissingException;

    void writeMap(String field, Map<String, ConfigValue<?>> value);

    void readMap(String field, Collection<ConfigValue<?>> values) throws ConfigValueMissingException;

    void readFile(File file) throws IOException, ConfigReadException;

    void writeFile(File file) throws IOException;

    void addComments(ICommentsProvider provider);
}