# Configuration

Project Bench has two settings files. Both live in the `config` folder of your Minecraft instance (or of your server).

- **`projectbench-common.toml`** holds the game's rules: features, crafting, buttons, hoppers, blueprints and archives. On a server, the server's copy counts for everyone. Players get its settings when they join.
- **`projectbench-client.toml`** is just yours: the colours you see, what the showcase shows you, and which other mods the bench works with. The server never reads it. See [The client file](#the-client-file).

## How to edit them

1. Close the game (or stop the server).
2. Open the file with any text editor.
3. Change a value and save.
4. Start the game again.

You can also save `projectbench-common.toml` while a world or a server is running: the change applies at once, and the server sends it to every player who is online. Two things wait: the recipes of a feature you turned on or off change only after `/reload` or a restart, and a pipe already connected to a bench keeps the bench's old automation sides until the bench is turned, broken or its chunk loads again.

The file is split into sections with headings in square brackets. A setting written here as `bench.slab.enabled` is the `enabled` line under the `[bench.slab]` heading. If you delete a line, it goes back to its default. If you type a value that is not allowed, the game puts the default back when it starts.

**Turning something off never deletes it.** Turned-off blocks and items lose their recipe and leave the creative tab and JEI. They cannot be placed or used. The ones already in your world stop working: their window does not open and hoppers cannot reach them. Turn the setting back on and they work again, with everything they held.

## Turning features on and off

| Setting | Default | What you notice |
|---|---|---|
| `enabled.bench` | `true` | The [Project Bench](Project-Bench.md) itself. When it is off, blueprints and archives are off too. |
| `enabled.blueprint` | `true` | [Blueprints](Blueprints.md): the item, recording, ghosts, the blueprint slot, **Record** and **Fill**, and the archives. When it is off, the bench is a crafting bench with storage, which is nice if you lay recipes out with JEI or EMI. |
| `enabled.archive` | `true` | The [Blueprint Archive](Blueprint-Archive.md) and its panel in the bench's window. |
| `enabled.autocraft` | `true` | The [Auto Project Bench](Auto-Project-Bench.md). When it is off, you cannot craft or place new ones, and the ones already placed work as normal benches. |

## Crafting at the bench

| Setting | Default | What you notice |
|---|---|---|
| `bench.keepLayout` | `true` | The bench refills a grid slot from its storage when a craft would empty it, so the recipe stays laid out; when off, the grid empties like a crafting table's. |
| `bench.autoDefault` | `"ITEM"` | How a newly placed bench's **Auto** button starts: `"OFF"` (no refill), `"ITEM"` (the same item) or `"TAGS"` (the same item or another the recipe accepts). |
| `bench.remaindersToStorage` | `true` | Crafting leftovers like empty buckets go into the bench's storage first; when off, they go straight to you. |

## Buttons

| Setting | Default | What you notice |
|---|---|---|
| `bench.buttons.auto` | `true` | Shows the **Auto** button; when off, every bench refills the way `bench.autoDefault` says. |
| `bench.buttons.balance` | `true` | Shows the **Balance** button. |
| `bench.buttons.clear` | `true` | Shows the **Clear** button. |
| `bench.buttons.fill` | `true` | Shows the **Fill** button. |

## Chests next to the bench

| Setting | Default | What you notice |
|---|---|---|
| `bench.external.enabled` | `true` | Shows the chest panel and the **Share** button; when off, the bench ignores the chests next to it. |
| `bench.external.shareDefault` | `"ALL"` | How a newly placed bench's **Share** button starts: `"NONE"`, `"OPEN"` (only the open tab) or `"ALL"`. |
| `bench.external.panelSlots` | `108` | The most slots of one chest that its tab shows, from 1 to 600; sharing still uses all of them. |
| `bench.external.protectVaults` | `true` | Create's Item Vault shows its items in its tab. As in Create, you cannot take from it or put into it by hand. The bench still uses it as **Share** says. When off, it works like any other chest. |
| `bench.external.processors` | `true` | A furnace, blast furnace, smoker or brewing stand next to the bench gets a tab that works like its own window; the bench never takes from it or fills it by itself. When off, they are treated like the machines in the next line. |
| `bench.external.partialTabs` | `false` | A machine from another mod that shows different slots on different sides gets no tab and the bench leaves it alone. When on, it gets a tab with only the slots of the side that touches the bench, and the bench uses it like a chest. |
| `bench.external.alwaysShow` | `["storagedrawers:controller", "storagedrawers:controller_io", "storagedrawers:framed_controller", "storagedrawers:framed_controller_io"]` | Blocks that always get a tab, even when they show different slots on different sides: the tab has the slots of the side that touches the bench, and the bench uses it like a chest. An id from a mod that is not installed does nothing. |

## Showcase

| Setting | Default | What you notice |
|---|---|---|
| `bench.showcase.enabled` | `true` | Shows the **Showcase** button; when off, nothing is drawn on top of any bench. |
| `bench.showcase.onByDefault` | `false` | Newly placed benches start with the showcase on. |

## Locked slots

| Setting | Default | What you notice |
|---|---|---|
| `bench.slotLocks.enabled` | `true` | Middle-click locks a storage slot to one item; when off, locks are hidden and do nothing, but benches remember them for when you turn this back on. |

## Slab and Project Station

| Setting | Default | What you notice |
|---|---|---|
| `bench.slab.enabled` | `true` | The [Project Bench Slab](Project-Bench-Slab.md) and the [Project Station](Project-Station.md); when off, every bench works alone. |
| `bench.slab.shareStorage` | `true` | The two halves of a Project Station share their storage and their chests; when off, each half only uses its own. |

## Automatic benches

| Setting | Default | What you notice |
|---|---|---|
| `bench.autocraft.onByDefault` | `true` | Newly placed automatic benches start with their automation button on. |

## Hoppers and pipes

These settings choose what a hopper or pipe reaches on each side. Sides turn with the block. The front is the side that faced you when you placed it. Left and right are as you look at the front. See [Hoppers and Pipes](Hoppers-and-Pipes.md) for how each side behaves in play.

Each side takes one of these values:

- `"INVENTORY"`: the bench's storage. Items come out of the storage first, then the grid.
- `"GRID"`: the crafting grid. Items come out of the grid first, then the storage.
- `"BLUEPRINT"`: only the blueprint slot and its dye slot. With blueprints turned off, it works like `"INVENTORY"`.
- `"NONE"`: closed.
- `"RESULT"`: only on automatic benches. Nothing goes in, and taking an item out crafts. On a normal bench, it is closed.

The result of a normal bench is never reachable, whatever these settings say. On a Project Station, the side where the two halves touch is always closed.

### Project Bench and slab sides

| Setting | Default | What you notice |
|---|---|---|
| `bench.automation.front` | `"BLUEPRINT"` | What the front reaches. |
| `bench.automation.back` | `"GRID"` | What the back reaches. |
| `bench.automation.left` | `"INVENTORY"` | What the left side reaches. |
| `bench.automation.right` | `"INVENTORY"` | What the right side reaches. |
| `bench.automation.up` | `"GRID"` | What the top reaches. |
| `bench.automation.down` | `"INVENTORY"` | What the bottom reaches. |

### Auto Project Bench sides

| Setting | Default | What you notice |
|---|---|---|
| `bench.autocraft.sides.front` | `"BLUEPRINT"` | What the front reaches. |
| `bench.autocraft.sides.back` | `"INVENTORY"` | What the back reaches. |
| `bench.autocraft.sides.left` | `"INVENTORY"` | What the left side reaches. |
| `bench.autocraft.sides.right` | `"INVENTORY"` | What the right side reaches. |
| `bench.autocraft.sides.up` | `"GRID"` | What the top reaches. |
| `bench.autocraft.sides.down` | `"RESULT"` | What the bottom reaches. |

When an automatic bench's automation button is off, a `"RESULT"` side is closed. When `enabled.autocraft` is off, automatic benches use the normal bench sides.

### Blueprint Archive sides

| Setting | Default | What you notice |
|---|---|---|
| `archive.automation.insertSides` | `["front", "back", "left", "right", "up", "down"]` | The sides where hoppers and pipes can put blueprints in; an empty list `[]` closes them all. |
| `archive.automation.extractSides` | `["down"]` | The sides where hoppers and pipes can take blueprints out. |

## Breaking

| Setting | Default | What you notice |
|---|---|---|
| `bench.onBreak` | `"SILK_TOUCH"` | What a broken bench does with its contents: `"DROP_ALL"` (always spill), `"SILK_TOUCH"` (a Silk Touch pickaxe keeps them inside the bench item) or `"ALWAYS_KEEP"` (any pickaxe keeps them). |
| `archive.onBreak` | `"SILK_TOUCH"` | The same for an archive and its blueprints, with the same three values. |

Explosions, pistons, commands, creative mode and breaking without the right tool always spill the contents.

## Blueprints

| Setting | Default | What you notice |
|---|---|---|
| `blueprint.shiftClickFillsGhosts` | `true` | Shift-click sends items onto a blueprint's ghosts; when off, the ghosts still show but shift-click ignores them. |
| `blueprint.tooltipIngredients` | `true` | A recorded blueprint's tooltip lists its ingredients, not just what it makes. |
| `blueprint.recordCost.enabled` | `true` | Recording costs an item, paid from a dye slot by the blueprint slot; when off, recording is free and the slot disappears. |
| `blueprint.recordCost.item` | `"minecraft:white_dye"` | The item recording costs, written as its id (any item from any mod; an id that does not exist makes recording free). |
| `blueprint.recordCost.count` | `1` | How many of that item each recording costs, from 1 to 64. |

## Blueprint Archive

| Setting | Default | What you notice |
|---|---|---|
| `archive.stackSize` | `64` | How many blank blueprints fit in one archive slot, from 1 to 64 (a recorded blueprint always takes a whole slot). |
| `archive.columnHeight` | `3` | How many archives can stack into one column, from 1 to 6. |
| `archive.benchPanel` | `true` | The bench's window lists the blueprints of the archives next to it; when off, an archive is just a shelf. |

## The client file

`projectbench-client.toml` is yours alone. Change it and only your game changes. On a server, every player keeps their own.

### Other mods

| Setting | Default | What you notice |
|---|---|---|
| `compat.jei` | `true` | The bench works with JEI: listed as a crafting station, recipes laid out with **+**, turned-off items hidden. |
| `compat.emi` | `true` | The bench works with EMI: listed as a crafting workstation, recipes laid out with the fill button. |
| `compat.jade` | `true` | Looking at a bench with Jade shows the recipe it is set to make. |
| `compat.polymorph` | `true` | Polymorph's button picks between matching recipes instead of the bench's own picker (restart the game after changing it). |
| `compat.mouseTweaks` | `true` | Mouse Tweaks' scroll wheel moves items the way the bench's shift-click does. |

See [Works With Other Mods](Works-With-Other-Mods.md) for the details.

### Showcase display

| Setting | Default | What you notice |
|---|---|---|
| `showcase.enabled` | `true` | You see the showcase on benches that have it on; when off, you never see it. |
| `showcase.show` | `"BOTH"` | What you see: `"BOTH"`, `"RESULT"` (only the floating result) or `"ITEMS"` (only the grid's items on top of the bench). |

### Project Station colours

Colours are written as `"#rrggbb"`, like in web pages. `"#ffffff"` or `""` means no tint.

| Setting | Default | What you notice |
|---|---|---|
| `station.leftTint` | `"#d4a017"` | The tint of one half of the Project Station, soft gold by default. |
| `station.rightTint` | `"#800020"` | The tint of the other half, soft burgundy by default. |

### Chest window colours

| Setting | Default | What you notice |
|---|---|---|
| `tints.windows` | `true` | The panels and tabs of the chests next to the bench are tinted; when off, they are all plain. |
| `tints.list` | every shulker box and the [Iron Chests](https://www.curseforge.com/minecraft/mc-mods/iron-chests) chests | Your own colour for a block's panel and tab, one entry per block (see below). |
| `tints.fromTexture` | `true` | A block that is not in the list gets a soft tint taken from its own texture, so a wooden drawer looks like its wood. |

Each entry of `tints.list` is the block's id, an equals sign and a colour, like `"quark:spruce_chest=#735531"`. Add a second colour for the title text, like `"quark:dark_oak_chest=#4f3218,#f0f0f0"`, which helps on dark colours. The default list gives each shulker box the colour of its dye. Each Iron Chests chest, trapped or not, gets the colour of its material. The dark ones get light titles. A block in the list always wins over its texture colour. A line with a mistake is skipped.

## See also

- [Project Bench](Project-Bench.md)
- [Blueprints](Blueprints.md)
- [Blueprint Archive](Blueprint-Archive.md)

[Back to Home](Home.md)
