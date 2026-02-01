package org.novasparkle.sateproxy.utilities;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public record Color(@Getter String abbr, @Getter String variable) implements Serializable {

    @Override
    public @NotNull String toString() {
        return "Color{" +
                "abbr='" + abbr + '\'' +
                ", variable='" + variable + '\'' +
                '}';
    }

    public String toHex() {
        if (this.isLegacy())
            return this.variable.replaceAll("&", "").replace("x", "#");
        return "";
    }

    public boolean isLegacy() {
        return this.variable.matches("^&x(&[0-9A-Fa-f]){6}$");
    }

    public String toLegacy() {
        if (this.variable.length() == 7) {
            String newString = this.variable.replace("#", "");
            StringBuilder builder = new StringBuilder("&x");
            for (char i : newString.toCharArray()) {
                builder.append('&').append(i);
            }
            return builder.toString();

        }
        return "";
    }
}
