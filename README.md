# Clod-MC Paper Plugin

A bespoke Paper plugin for the clod.glob.au Minecraft server.

### Homes

- minimal implementation of custom homes
- `/home [name]` : teleport to a saved home
- `/back` : teleport back to your previous position
- `/homes` : list homes
- `/sethome [name]` : save current location as a home
- `/delhome [mame]` : delete the specified home
- `/spawn` : teleport to server spawn

### Invite

After reaching a playtime threshold, any player can invite others; they will be
added to the server's whitelist.

- `/invite {java|bedrock} {player-name}`

### Mobs

- mobs will not spawn in admin claims (eg. spawn)
- creeper explosions no longer destroy blocks
- endermen no longer pick up blocks
- shulkers always drop two shells when killed by a player
- wither skeletons have a slightly increased chance of dropping heads

### Other

- new players receive a book welcoming them to the server
- shift+right-click to sort a container
- the server announces the name of the player who skipped the night
- indicates when players are afk, use `/afk` to toggle
- leaves decay quickly
- `/adminmode` to toggle between player and admin inventories