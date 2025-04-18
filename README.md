# Clod-MC Paper Plugin

A bespoke Paper plugin for the https://clod.glob.au/ Minecraft server.

While Clod-MC is open-source, pull requests are not accepted.

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

### Gateways

End game player-craftable point-to-point portals.

Recipe:

```
PWP    P=EnderPearl  W=Wool
EWE    E=EndStone    R=Respawn Anchor
ERE
```

The two wool blocks set the anchor block's unique colour. Place two blocks in the world with the
same two colours in the same positions to establish a gateway.

A black-black anchor will teleport new players to a random location away from
spawn; this can only be crafted by a server operator.

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
- a `/msg` sent to an offline player will be delivered the next time they log in
- `/seen` to show when a player last logged out
- leaves decay quickly
- `/admininv` to toggle between player and admin inventories
- waxing an item frame prevents item rotation or removal (interact while crouching to override)
- clicking on a waxed sign or item frame attached to a container opens the container's inventory
- spore blossoms are craftable with a big dripleaf and Allium (shapeless)
- waxing a pressure plate will prevent it from functioning (useful for decorations)
- "VeinMiner" enchantment; once applied to a tool shift+mine to break all connected blocks of the
  same type
- picking an item (middle click) will also pull from inside held shulker boxes
