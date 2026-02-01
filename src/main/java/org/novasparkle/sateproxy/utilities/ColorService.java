package org.novasparkle.sateproxy.utilities;


import lombok.experimental.UtilityClass;
import org.novasparkle.sateproxy.configuration.ConfigManager;
import org.simpleyaml.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public final class ColorService {
    private static final List<Color> colorList = new ArrayList<>();

    static {
        reload(ConfigManager.getSection("colors"));
    }

    public void reload(ConfigurationSection section) {
        if (section == null) throw new RuntimeException("Секция с цветами не найдена, нужная секция: colors");

        colorList.clear();
        for (String key : section.getKeys(false)) {
            ConfigurationSection colorSection = section.getConfigurationSection(key);
            assert colorSection != null;
            colorList.add(new Color(colorSection.getString("abbr"), colorSection.getString("variable")));
        }
    }

    public String color(String text) {
        if (text == null || text.isEmpty()) return "";
        for (Color color : colorList) {
            text = text.replaceAll(color.abbr(), color.variable());
        }
        return text;
    }
}