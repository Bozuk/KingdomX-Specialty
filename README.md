# Kingdoms Addon — Specialties

An addon for [KingdomsX](https://github.com/CryptoMorin/KingdomsX).

Every kingdom picks **one of three specialties, at the moment it is founded**: `/k create <name>`
opens the specialty menu, and the kingdom is only founded once the choice is made. A specialty
grants a **unique resource**, produced by the **KingdomsX extractors**, which is what the exclusive
items of the **Specialty Forge** — a kingdom structure this addon adds — are made of. The choice is
**final**: only an administrator can change it.

| Specialty                | Unique resource | Unlocks |
|--------------------------|-----------------|---------|
| `weaponsmith` — Warsmith | Carbon          | netherite sword, axe and spear, **+2 damage** |
| `armorer` — Royal Armorer| Mithril         | the 4 netherite armor pieces, **+2 armor and +1 toughness** |
| `alchemist` — Alchemist  | Goo             | wither potion, strength III potion, sublimated golden apple |

The forged items keep the **look and the stats of the original item** — a carbon sword is still a
netherite sword — plus the enchanted shimmer and the bonus above.

---

## Installation

1. Build: `mvn package` → `target/Kingdoms-Addon-Specialties-1.0.0.jar`
2. Drop the jar into the server's `plugins/` folder, next to KingdomsX.
3. Restart. The addon installs:
   - `plugins/Kingdoms/specialties.yml` — the configuration;
   - `plugins/Kingdoms/specialties/languages/<code>.yml` — one message file per language;
   - `plugins/Kingdoms/Structures/specialty-forge.yml` — the structure;
   - its menus in the KingdomsX GUI folders;
   - a `specialty-forge` entry in `guis/<language>/structures/nexus/structures.yml`, the structure
     shop — without it the forge does not show up there (see the technical notes).

Requirements: Minecraft **1.16+**, KingdomsX **1.17.27.3**. Folia is supported.

**Rebuild on every KingdomsX version bump, before deploying.** Signatures really do move: 1.17.27.3
renamed `KingdomItemInteractEvent` to `KingdomBuildingInteractEvent` and `getKingdomItem()` to
`getBuilding()`, which gives a `NoSuchMethodError` when the forge is opened if the addon was built
against an earlier one. Compiling is the fastest way to find those breaks. Maven Central stops at
1.17.18.1-BETA, so install your own server's jar — which is also the more faithful thing to build
against:

```bash
mvn install:install-file -Dfile=<server>/plugins/KingdomsX-1.17.27.3.jar -DgroupId=com.github.cryptomorin -DartifactId=kingdoms -Dversion=1.17.27.3 -Dpackaging=jar
```

**Spears** (`NETHERITE_SPEAR`) only exist since **1.21.11**. On an older server that one recipe is
skipped with a message in the console; the other two weapons work as usual.

## Commands

| Command | What it does |
|---|---|
| `/k create <name>` | Opens the specialty menu, then founds the kingdom |
| `/k specialty info` | Specialty, extractors, yield and pending remainder |
| `/k specialty choose [specialty]` | **Catch-up** — only for a kingdom that has none |
| `/k admin specialty <kingdom> <specialty>` | **Admin** — forces a specialty |
| `/k admin specialty <kingdom> reset` | **Admin** — wipes the kingdom's specialty |

Aliases: `/k specialities`, `/k spec`.

## The game loop

1. **Found** — `/k create <name>` creates nothing right away: the menu of the three specialties
   opens, a confirmation is asked for, and only then is the kingdom founded. Closing the menu
   without picking cancels the creation.
2. **Produce** — the resource comes out of the KingdomsX extractors (`/k structures`). On top of
   the usual resource points, the collector receives the specialty's resource. An extractor with no
   fuel generates nothing, hence no resource either.
3. **Build the forge** — `/k structures` → *Specialty Forge*, then place it and build it like any
   other kingdom structure.
4. **Craft** — right-click the forge. The menu lists the recipes of the kingdom's specialty, each
   with the ingredients owned / required. Ingredients are taken from the player's inventory.

The forge is the **only** place these items can be made: there is no crafting table recipe. A
kingdom without the right specialty simply does not see the matching recipes.

### What each specialty makes

**Warsmith — Carbon.** A netherite weapon + 4 carbon gives the same weapon, enchanted shimmer and
2 more damage: sword 10 instead of 8, axe 12 instead of 10, spear 7 instead of 5. Attack speed and
reach unchanged.

**Royal Armorer — Mithril.** A netherite armor piece + 4 mithril gives the same piece, enchanted
shimmer, +2 armor and +1 armor toughness: helmet 5/4, chestplate 10/4, leggings 8/4, boots 5/4
(armor / toughness). Knockback resistance unchanged.

**Alchemist — Goo.** Three transformations, one goo each:

| Ingredient | Result |
|---|---|
| Poison potion | Wither potion, **same duration and level** as the bottle used |
| Strength II potion | Strength III potion, **same duration** |
| Golden apple | Sublimated golden apple: Absorption **II** and Regeneration **IV**, against I and II |

The wither and strength III potions take their figures from the bottle that was consumed, not from
the configuration: a long poison gives a long wither, a poison II gives a wither II.

## Languages

Messages live in **one file per language**, `plugins/Kingdoms/specialties/languages/<code>.yml`,
with exactly the same layout as the KingdomsX language files (the `command:` and `specialties:`
trees, the same keys). A missing key falls back to the English text built into the addon, so a
partial translation breaks nothing.

A file is created on startup for every language installed on the server: French ships translated,
the others are generated from the English texts, ready to be translated.

**Pointing a language at another file.** A language normally reads the file of its own code, and
nothing has to be configured. The exception is a language KingdomsX does not have — it ships
`en, de, es, it, pt, pl, ru, cs, hu, tr, uk, vi, zh`, so a French server runs in English. Pointing
`EN` at `fr.yml` gives it French messages:

```yaml
messages:
  language-files:
    EN: fr        # the EN language reads fr.yml
```

The rest of the in-game text follows the KingdomsX mechanisms:

| What | Where | Per language? |
|---|---|---|
| The addon's messages | `specialties/languages/<code>.yml` | yes |
| Menu titles and labels | `languages/<language>/guis/…` of KingdomsX | yes |
| Specialty, resource and recipe names | `specialties.yml` | no — server content |

## Configuration — `specialties.yml`

```yaml
selection:
  require-confirmation: true   # founding and catch-up alike

  # Catch-up only (/k specialty choose):
  king-only: true              # otherwise, the UPGRADE kingdom permission
  required-level: 1
  cost: { resource-points: 0, money: 0 }

extraction:
  resource-points-per-unit: 100   # resource points per unit produced
  multiplier: '1'                 # math expression, kingdom context
  max-per-collect: 0              # 0 = no limit
  carry-over-remainder: true      # carries the remainder to the next collect
  drop-if-inventory-full: true

# messages:                       # see the Languages section
#   language-files: { EN: fr }
```

Fuel, rate and yield of the extractors are configured on the KingdomsX side, in
`Structures/extractor.yml`. The forge's cost, limits and blocks in
`Structures/specialty-forge.yml`.

### Adding a recipe

Under `specialties.<specialty>.recipes`. Three types: `SHAPED`, `SHAPELESS` and `TRANSMUTE`.
The forge has no crafting grid: a `shape` is only read to count the ingredients.

```yaml
      my-recipe:
        type: SHAPELESS           # SHAPED | SHAPELESS | TRANSMUTE
        display-name: '&cMy recipe'
        description: [ '&7...' ]
        ingredients:              # repeating an entry asks for more
          - NETHERITE_SWORD
          - '@resource'           # the unique resource of the specialty
          - '@resource'
        result:
          material: NETHERITE_SWORD
          name: '&cBlade'
          glint: true                        # the enchanted shimmer
          attributes:                        # what genuinely outclasses netherite
            ATTACK_DAMAGE:
              name: specialties:my-recipe-attack-damage
              amount: 9
              operation: ADD_NUMBER
              slot: HAND
```

A transmutation recipe — a `source` potion is consumed, and the result **carries over the duration
and the level** of its effect:

```yaml
      wither-potion:
        type: TRANSMUTE
        icon: POTION
        source:
          material: POTION
          effect: POISON          # the potion has to carry this effect
          amplifier: 1            # optional: that level exactly
          name: '&2Potion of Poison'
        ingredients: [ '@resource' ]
        transmute:
          effect: WITHER          # the result's effect
          amplifier-shift: 0      # +1 to go one level up
        result:
          material: POTION
          name: '&8Potion of Withering'
          glint: true
          color: '#3b3b3b'
```

An item (`result` or `resource`) is described **with the KingdomsX item syntax**: it is the main
plugin's own deserializer that reads it, itself built on XSeries' `XItemStack`. So `material`,
`amount`, `name`, `lore`, `enchants`, `stored-enchants`, `flags`, `attributes`, `effects`, `color`,
`custom-model-data`, `unbreakable`, `glow`, `damage`, `skull`, `trim`, `item-model`, `nbt`… — the
main plugin's own files are the reference. Two options are added on top, belonging to this addon:
`glint` and `consume-effects`.

**Mind the attributes**: names may be written either `ARMOR` or `GENERIC_ARMOR`, XSeries maps them
onto what the running server uses. Do give every modifier a `name` though, lower case and in
`namespace:key` form: without one XSeries makes up a random UUID, and the item stops being the same
from one restart to the next. Finally, defining any attribute replaces the item's base stats. On a
weapon or a piece of armor, spell out every stat you want — a spear's reach included. Vanilla
figures (the tooltip value in brackets) — netherite sword: 7 (8) damage / -2.4 (1.6) attack speed;
netherite axe: 9 (10) / -3.0 (1.0); netherite spear: 4 (5) / -3.13 (0.87); netherite chestplate:
8 armor / 3 toughness / 0.1 knockback resistance; helmet and boots: 3 armor; leggings: 6 armor.

**`glint`** gives the enchanted shimmer through the component made for it (1.20.5+). Prefer it over
`glow`, which gets the same shimmer out of a hidden enchantment — and so hands the item a very real
Unbreaking I, which breaks "the same stats as netherite".

**`consume-effects`** gives effects to a **food** item when it is eaten. A golden apple's effects
are hardcoded in the game and cannot be edited: the addon applies them itself on consumption, just
before the game applies its own, which can then no longer weaken them. Same shape as `effects` —
`EFFECT, seconds, level`, the level starting at 1:

```yaml
          consume-effects:
            - 'ABSORPTION, 120, 2'    # Absorption II, two minutes
            - 'REGENERATION, 5, 4'    # Regeneration IV, five seconds
```

## Technical notes

- **Storage** — two kingdom metadata entries: `Specialties:SPECIALTY` and
  `Specialties:PRODUCTION_REMAINDER` (resource points not yet converted into a whole unit). They
  follow the KingdomsX save.
- **Production** — an `ExtractorCollectEvent` listener converts the resource points the extractor is
  about to pay out. No repeating task, no added block.
- **Creation** — `KingdomCreateEvent` cannot be cancelled and fires once the kingdom exists. The
  interception happens one notch earlier, on `KingdomsPreCommandEvent`: the `/k create` command is
  held back, the menu opens, and the original command is replayed as-is once the specialty is
  picked. The KingdomsX permission and cooldown checks have already run by then, nothing is
  short-circuited.
- **Catch-up** — `/k specialty choose` only serves the kingdoms without a specialty: those founded
  before the addon was installed, those created by `/k admin create`, and those an admin has reset.
  `selection.king-only`, `required-level` and `cost` apply to that case only.
- **Structure** — a `StructureType` registered in the KingdomsX registry. The
  `Structures/specialty-forge.yml` file is only written after the default structures have been
  extracted: creating that folder too early would stop KingdomsX from putting its own in it on a
  brand new server.
- **Shop menu** — registering the structure is not enough to make it buyable. KingdomsX walks the
  registered structures and, for each of them, looks for an option **of the same name** in
  `guis/<language>/structures/nexus/structures.yml`; a structure with no entry there is skipped
  without a single message in the console. So the addon adds its own on startup, once, to the menu
  of every installed language, and never touches it again: the entry becomes an ordinary line of
  the server's configuration, free to be moved, restyled or deleted. The slot is picked among those
  the menu does not use yet.
- **Languages** — the addon registers its own compiled messages for every installed language, by
  reading its own folder. Missing entries fall back to the English texts in the code, so no message
  can be left unresolved, even with an empty or partial file.
- **Items** — built by `KingdomsItemDeserializer`, the KingdomsX deserializer, which wraps XSeries'
  `XItemStack`. Materials, effects and attributes go through `XMaterial`, `XPotion` and
  `XAttribute`: that is where the knowledge of the renames between versions lives, and rewriting it
  by hand only means owning the drift. The addon only adds what has no equivalent — `glint` and
  `consume-effects`.
- **XSeries** — KingdomsX downloads the library, relocates it to `org.kingdoms.libs.xseries` and
  injects it into **its own** plugin classloader, so an addon that depends on Kingdoms can see it.
  Hence the `provided` dependency plus the shade relocation: we compile against it and rewrite our
  references to the copy KingdomsX loaded, without shipping a single one of its classes. Shipping
  our own copy would only mean two XSeries on the server.
- **Reading potions** — the one direction XSeries does not cover: `XItemStack` turns configuration
  into an item, nothing turns an item back into the effects it carries, which is exactly what a
  transmutation needs in order to copy the duration and the level of a bottle. The `source` is
  recognised by its effect and not by its material, every potion in the game sharing `POTION`. The
  rest goes through reflection, 1.20.5 having replaced `getBasePotionData()` — a type plus the
  *extended* / *upgraded* flags — with `getBasePotionType()`, where every variant knows its own
  effects.
- **Ingredients** — a plain material ingredient never consumes a specialty item of the same
  material: the resource and anything the forge produced carry a persistent tag. Upgrading an
  already upgraded carbon sword is therefore impossible, rather than expensive.

## API

```java
Specialty specialty = KingdomSpecialties.getSpecialty(kingdom);      // null if none was picked
List<Extractor> extractors = KingdomSpecialties.getExtractors(kingdom);
int fueled = KingdomSpecialties.countFueledExtractors(kingdom);
long pending = KingdomSpecialties.getProductionRemainder(kingdom);
List<SpecialtyRecipe> recipes = specialty.getRecipes();
```

## Project layout

```
src/main/java/org/kingdoms/specialties/
├── SpecialtiesAddon.java          the addon's entry point
├── commands/                      /k specialty … and /k admin specialty
├── config/                        config, language, GUI paths
├── data/                          Specialty, recipes, metadata
├── gui/                           selection menu, forge menu
├── items/                         item factory, potion reading, tags
├── managers/                      extraction, forge, selection, consumption
└── structure/                     structure type and file installation
src/main/resources/
├── specialties.yml
├── Structures/specialty-forge.yml
├── menu/nexus-structures-option.yml
└── guis/
    ├── specialties/selection.yml
    └── structures/specialty-forge.yml
```
