package au.com.glob.clodmc.modules.welcome;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.config.Config;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WelcomeBook {
  private static final @NotNull String FILENAME = "welcome-book.yml";

  public static @Nullable ItemStack build() {
    // build an ItemStack book from welcome-book.yml
    Config config = Config.of(FILENAME);

    MiniMessage miniMessage = MiniMessage.miniMessage();
    ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();
    BookMeta.BookMetaBuilder builder = bookMeta.toBuilder();

    List<String> pages = config.getStringList("pages");
    if (pages.isEmpty()) {
      ClodMC.logWarning(config + ": no pages");
      return null;
    }
    for (String page : pages) {
      builder.addPage(miniMessage.deserialize(page));
    }
    builder
        .title(Component.text(config.getString("title", "")))
        .author(Component.text(config.getString("author", "")));

    bookItem.setItemMeta(builder.build());

    Config.unload(FILENAME);
    return bookItem;
  }
}
