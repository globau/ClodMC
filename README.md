# Clod-MC Paper Plugin

A bespoke Paper plugin for the https://clod.glob.au/ Minecraft server.

While Clod-MC is open-source, pull requests are not accepted.

## Features

### Player

- **Away From Keyboard** - Automatic and manual AFK; players are visibly AFK in the tab-list [[src](src/main/java/au/com/glob/clodmc/modules/player/afk/AFK.java)]
- **Back** - /back command to return to where you last teleported from [[src](src/main/java/au/com/glob/clodmc/modules/player/Back.java)]
- **Chorus Flower Crafting** - Adds crafting recipes for Chorus Flower [[src](src/main/java/au/com/glob/clodmc/modules/crafting/ChorusFlower.java)]
- **Click-Through Waxed Signs** - When right-clicking on a waxed sign attached to a chest, open the chest [[src](src/main/java/au/com/glob/clodmc/modules/interactions/SignedContainers.java)]
- **Deep Pockets** - When picking blocks, look inside held shulker boxes too [[src](src/main/java/au/com/glob/clodmc/modules/inventory/deeppockets/DeepPockets.java)]
- **Homes** - Set limited number of named teleport locations [[src](src/main/java/au/com/glob/clodmc/modules/player/Homes.java)]
- **Inventory Sorting** - Sort containers by shift+right-clicking in the inventory screen [[src](src/main/java/au/com/glob/clodmc/modules/inventory/inventorysort/InventorySort.java)]
- **Invite** - Allows players with enough playtime to add others to the whitelist [[src](src/main/java/au/com/glob/clodmc/modules/player/Invite.java)]
- **Named Storage** - If a container has been named in an anvil, show that name when looking at it [[src](src/main/java/au/com/glob/clodmc/modules/interactions/NamedStorage.java)]
- **Offline Messages** - Queue and deliver whispers sent to offline players [[src](src/main/java/au/com/glob/clodmc/modules/player/offlinemessages/OfflineMessages.java)]
- **Point-to-Point Gateways** - Player-built point-to-point teleportation system using coloured wool anchors [[src](src/main/java/au/com/glob/clodmc/modules/interactions/gateways/Gateways.java)]
- **Seen** - Show how long it's been since the server last saw the player [[src](src/main/java/au/com/glob/clodmc/modules/player/Seen.java)]
- **Server Status** - Add simple server status/health command [[src](src/main/java/au/com/glob/clodmc/modules/server/ServerStatus.java)]
- **Silence Mobs/Animals** - Include 🔇 when naming a mob/animal/etc to silence it [[src](src/main/java/au/com/glob/clodmc/modules/mobs/SilenceMobs.java)]
- **Sleep** - Tell players who slept and skipped the night [[src](src/main/java/au/com/glob/clodmc/modules/player/Sleep.java)]
- **Spawn Teleport** - Adds /spawn to teleport to spawn [[src](src/main/java/au/com/glob/clodmc/modules/player/Spawn.java)]
- **Spore Blossom Crafting** - Adds a crafting recipe for Spore Blossoms [[src](src/main/java/au/com/glob/clodmc/modules/crafting/SporeBlossom.java)]
- **VeinMiner Enchantment** - Mine connected blocks with one action when shift+mining [[src](src/main/java/au/com/glob/clodmc/modules/interactions/VeinMiner.java)]
- **Waxed Item Frames** - Allow waxing an item-frame to prevent item rotation/removal and allow chest click-through [[src](src/main/java/au/com/glob/clodmc/modules/interactions/WaxedItemFrames.java)]
- **Waxed Pressure Plates** - Allow waxing a pressure plate to prevent it activating [[src](src/main/java/au/com/glob/clodmc/modules/interactions/WaxedPressurePlates.java)]

### Server

- **Better Drops** - Improve the drop rates of selected entities [[src](src/main/java/au/com/glob/clodmc/modules/mobs/BetterDrops.java)]
- **BlueMap Integration** - Bridge between ClodMC modules and BlueMap [[src](src/main/java/au/com/glob/clodmc/modules/bluemap/BlueMap.java)]
- **Fast Leaf Decay** - Nearly instant decaying of leafs [[src](src/main/java/au/com/glob/clodmc/modules/interactions/FastLeafDecay.java)]
- **Heat Map** - Track minutes a chunk is occupied by at least one player [[src](src/main/java/au/com/glob/clodmc/modules/server/heapmap/HeatMap.java)]
- **Player Tracker** - Collect data about players (eg. login/logout times) [[src](src/main/java/au/com/glob/clodmc/modules/player/PlayerTracker.java)]
- **Prevent Mob Griefing** - Prevent some mobs from breaking or moving blocks [[src](src/main/java/au/com/glob/clodmc/modules/mobs/PreventMobGriefing.java)]
- **Prevent Mob Spawning** - Prevents enemy mobs from spawning within areas claimed by admin [[src](src/main/java/au/com/glob/clodmc/modules/mobs/preventmobspawn/PreventMobSpawn.java)]

### Admin

- **Admin Inventory** - Swap between player and admin inventories [[src](src/main/java/au/com/glob/clodmc/modules/inventory/AdminInv.java)]
- **Custom MOTD** - Set MOTD automatically based on server type [[src](src/main/java/au/com/glob/clodmc/modules/server/MOTD.java)]
- **Death Log** - Log player's death location in server log [[src](src/main/java/au/com/glob/clodmc/modules/player/DeathLog.java)]
- **Inventory Restore** - Automatic inventory backup, allowing admins to restore [[src](src/main/java/au/com/glob/clodmc/modules/inventory/InventoryRestore.java)]
- **Required Plugins** - Don't allow non-op players to connect unless all required plugins are loaded [[src](src/main/java/au/com/glob/clodmc/modules/server/RequiredPlugins.java)]
- **Server Links** - Set server links in Minecraft client pause screen [[src](src/main/java/au/com/glob/clodmc/modules/server/ClodServerLinks.java)]
- **Welcome Book** - Give players the Welcome Book [[src](src/main/java/au/com/glob/clodmc/modules/player/WelcomeBook.java)]

## Commands

### Player

- `/afk` - Toggle AFK status
- `/back` - Teleport to previous location
- `/delhome` - Delete home
- `/gateways` - List gateways in use
- `/home` - Teleport home
- `/homes` - List homes
- `/invite` - Add a player to the whitelist
- `/seen` - Show time since player's last login
- `/server-status` - Shows server status
- `/sethome` - Sets a home to your current location
- `/spawn` - Teleport to spawn

### Admin

- `/admininv` - Toggle admin/player inventory
- `/restore_inv` - Restore player's inventory from automatic backups
- `/welcome` - Give specified player the welcome book
