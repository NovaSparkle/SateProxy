package org.novasparkle.sateproxy.utilities;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.mindrot.jbcrypt.BCrypt;
import org.novasparkle.sateproxy.configuration.ConfigManager;
import org.simpleyaml.utils.Validate;

@UtilityClass
public class Utils {

    public String applyReplacements(String starterLine, String... replacements) {
        byte index = 0;

        String line = starterLine;
        for (String replacement : replacements) {
            if (replacement.contains("-%-")) {
                String[] mass = replacement.split("-%-");
                if (mass.length >= 2) {
                    line = line.replace("[" + mass[0] + "]", mass[1]);
                    continue;
                }
            }

            line = line.replace("[" + index + "]", replacement);
            index++;
        }
        return line;
    }

    public String getHash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(ConfigManager.getInt("auth.saltRound")));
    }

    public boolean checkPassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }

    public Component toComponent(String text) {
        ComponentSerializer<Component, Component, String> serializer = ConfigManager.serializer.getSerializer();
        Validate.notNull(serializer);
        return serializer.deserialize(text);
    }
}
