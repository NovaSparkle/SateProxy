package org.novasparkle.sateproxy.configuration;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.novasparkle.sateproxy.SateProxy;
import org.novasparkle.sateproxy.utilities.ColorService;
import org.novasparkle.sateproxy.utilities.Utils;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlConfiguration;
import org.simpleyaml.utils.Validate;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class ConfigManager {
    public final Serializers serializer;
    private final YamlConfiguration config;

    static {
        config = SateProxy.instance.getConfig();
        serializer = Serializers.valueOf(config.getString("serializer"));
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }

    @SneakyThrows
    public void save(File file)  {
        config.save(file);
    }

    public Set<String> getKeys(boolean deep) {
        return config.getKeys(deep);
    }

    public boolean contains(String path) {
        return config.contains(path);
    }

    public String getName() {
        return config.getName();
    }

    public ConfigurationSection createSection(String path) {
        return config.createSection(path);
    }

    public String getString(String path) {
        return config.getString(path);
    }

    public int getInt(String path) {
        return config.getInt(path);
    }

    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    public double getDouble(String path) {
        return config.getDouble(path);
    }

    public long getLong(String path) {
        return config.getLong(path);
    }

    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public List<Integer> getIntegerList(String path) {
        return config.getIntegerList(path);
    }

    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }

    public void remove(String path) {
        config.remove(path);
    }

    public void sendMessage(final Audience target, String messageId, String... replacements) {

        List<Component> componentMessage = getMessageAsComponent(messageId, replacements);
        componentMessage.forEach(target::sendMessage);
    }

    public List<String> getMessage(String messageId, String... replacements) {
        String path = "messages." + messageId;

        List<String> message = Lists.newArrayList(config.getStringList(path));
        if (message.isEmpty()) {
            String stringMessage = config.getString(path);
            if (stringMessage != null && !stringMessage.isEmpty()) message.add(stringMessage);
        }

        return message.stream().map(line -> ColorService.color(Utils.applyReplacements(line, replacements))).collect(Collectors.toList());
    }

    public List<Component> getMessageAsComponent(String messageId, String... replacements) {
        ComponentSerializer<Component, Component, String> serializer = ConfigManager.serializer.getSerializer();
        Validate.notNull(serializer);
        List<String> stringMessage = ConfigManager.getMessage(messageId, replacements);

        return stringMessage.stream().map(serializer::deserialize).toList();
    }


}
