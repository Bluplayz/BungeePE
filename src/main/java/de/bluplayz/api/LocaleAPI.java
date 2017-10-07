package de.bluplayz.api;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import de.bluplayz.BungeePE;
import de.bluplayz.locale.Locale;

public class LocaleAPI {

    public static void log(String key, Object... args) {
        BungeePE.getInstance().getServer().getLogger().info(translate(BungeePE.getInstance().getConsoleLocale(), key, args));
    }

    public static String translate(String languageCode, String key, Object... args) {
        return translate(BungeePE.getInstance().getLocaleManager().getLocale(languageCode), key, args);
    }

    public static String translate( CommandSender target, String key, Object... args) {
        if (target instanceof Player ) {
            String languageCode = ((Player) target).getLoginChainData().getLanguageCode();
            return translate(BungeePE.getInstance().getLocaleManager().getLocale(languageCode), key, args);
        }

        return translate(BungeePE.getInstance().getConsoleLocale(), key, args);
    }

    public static void sendTranslatedMessage( CommandSender target, String key, Object... args) {
        target.sendMessage(translate(target, key, args));
    }

    public static String translate(Locale locale, String key, Object... args) {
        String message = BungeePE.getInstance().getLocaleManager().getTranslatedMessage(locale, key, args);
        message = message.replaceAll("\\{PREFIX}", BungeePE.getInstance().getLocaleManager().getTranslatedMessage(locale, "prefix"));

        return message;
    }
}
