package org.novasparkle.sateproxy.configuration;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.novasparkle.sateproxy.SateProxy;
import org.novasparkle.sateproxy.utilities.ColorService;
import org.novasparkle.sateproxy.utilities.Utils;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class TelegramManager {
    private final YamlConfiguration config;


    static {
        try {
            config = YamlConfiguration.loadConfiguration(new File(SateProxy.instance.getDataDirectory(), "telegram.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        return ColorService.color(config.getString(path));
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
        return config.getStringList(path).stream().map(ColorService::color).collect(Collectors.toList());
    }

    public List<Integer> getIntegerList(String path) {
        return config.getIntegerList(path);
    }

    public ConfigurationSection getConfigurationSection(String path) {
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
        List<String> stringMessage = TelegramManager.getMessage(messageId, replacements);

        return stringMessage.stream().map(Utils::toComponent).toList();
    }
}
