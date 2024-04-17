package au.com.glob.clodmc.modules.welcome;

import au.com.glob.clodmc.ClodMC;
import java.io.File;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.Nullable;

public class WelcomeBook {
  private static ItemStack welcomeBookItemStack;
  private static long configLastModified = -1;

  @Nullable public static ItemStack build() {
    // build an ItemStack book from welcome-book.yml
    // changes to the config will automatically be detected

    File configFile = new File(ClodMC.instance.getDataFolder(), "welcome-book.yml");
    if (!configFile.exists()) {
      welcomeBookItemStack = null;
      configLastModified = -1;
      return null;
    }

    long mtime = configFile.lastModified();
    if (mtime == configLastModified) {
      return welcomeBookItemStack;
    }
    welcomeBookItemStack = null;
    configLastModified = mtime;

    MiniMessage miniMessage = MiniMessage.miniMessage();
    ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();
    BookMeta.BookMetaBuilder builder = bookMeta.toBuilder();

    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

    List<String> pages = config.getStringList("pages");
    if (pages.isEmpty()) {
      ClodMC.logWarning(configFile + ": no pages");
      return null;
    }
    for (String page : pages) {
      builder.addPage(miniMessage.deserialize(page));
    }
    builder
        .title(Component.text(config.getString("title", "Welcome")))
        .author(Component.text(config.getString("author", "glob")));

    bookItem.setItemMeta(builder.build());
    welcomeBookItemStack = bookItem;

    return welcomeBookItemStack;
  }
}
