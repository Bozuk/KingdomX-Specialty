# Kingdoms Addon — Specialties

Extension (addon) pour [KingdomsX](https://github.com/CryptoMorin/KingdomsX).

Chaque royaume choisit **une spécialité parmi quatre, au moment de sa création** : `/k create <nom>`
ouvre le menu des spécialités et le royaume n'est fondé qu'une fois le choix fait. La spécialité
donne accès à une **ressource unique**, produite par les **extracteurs de KingdomsX**, qui sert à
fabriquer des objets exclusifs à la **Forge de Spécialité**, une structure de royaume ajoutée par
l'extension. Le choix est **définitif** : seul un administrateur peut le modifier.

| Spécialité                | Ressource unique     | Débloque |
|---------------------------|----------------------|----------|
| `weaponsmith` — Forgeron  | Acier Trempé         | des armes **au-dessus de la netherite** |
| `armorer` — Armurier      | Plaque de Mithril    | des armures **au-dessus de la netherite** |
| `alchemist` — Alchimiste  | Essence Arcanique    | des potions de bataille |
| `enchanter` — Enchanteur  | Rune Ancestrale      | des enchantements **au-delà des limites vanilla** |

---

## Installation

1. Compiler : `mvn package` → `target/Kingdoms-Addon-Specialties-1.0.0.jar`
2. Déposer le jar dans le dossier `plugins/` du serveur (à côté de KingdomsX).
3. Redémarrer. L'extension installe :
   - `plugins/Kingdoms/specialties.yml` — la configuration ;
   - `plugins/Kingdoms/specialties/languages/<code>.yml` — un fichier de messages par langue ;
   - `plugins/Kingdoms/Structures/specialty-forge.yml` — la structure ;
   - les menus dans les dossiers GUI de KingdomsX.

Prérequis : Minecraft **1.16+**, KingdomsX **1.17.18.1-BETA** ou plus récent. Folia est supporté.

## Commandes

| Commande | Effet |
|---|---|
| `/k create <nom>` | Ouvre le menu des spécialités, puis fonde le royaume |
| `/k specialty info` | Spécialité, extracteurs, rendement et reliquat |
| `/k specialty choose [spécialité]` | **Rattrapage** — seulement pour un royaume sans spécialité |
| `/k admin specialty <royaume> <spécialité>` | **Admin** — impose une spécialité |
| `/k admin specialty <royaume> reset` | **Admin** — remet le royaume à zéro |

Alias : `/k specialite`, `/k spec`.

## Boucle de jeu

1. **Fonder** — `/k create <nom>` ne crée rien tout de suite : le menu des quatre spécialités
   s'ouvre, une confirmation est demandée, et le royaume n'est fondé qu'ensuite. Fermer le menu
   sans choisir annule la création.
2. **Produire** — la ressource sort des extracteurs KingdomsX (`/k structures`). En plus de ses
   points de ressources, le collecteur reçoit la ressource de la spécialité. Un extracteur sans
   carburant ne génère rien, donc pas de ressource non plus.
3. **Construire la forge** — `/k structures` → *Forge de Spécialité*, puis la poser et la
   construire comme n'importe quelle structure du royaume.
4. **Fabriquer** — clic droit sur la forge. Le menu liste les recettes de la spécialité du
   royaume, avec pour chacune les ingrédients possédés / requis. Les ingrédients sont pris dans
   l'inventaire du joueur.

La forge est le **seul** endroit où ces objets se fabriquent : il n'y a pas de recette d'établi.
Un royaume sans la bonne spécialité ne voit tout simplement pas les recettes correspondantes.

### L'enchanteur

Les enchantements sont appliqués **directement à l'objet tenu en main**, pas via un livre. C'est
volontaire : une enclume ramène toujours un enchantement à son niveau maximum vanilla, un livre
Tranchant VIII n'aurait donc servi à rien.

## Langues

Les messages vivent dans **un fichier par langue**, `plugins/Kingdoms/specialties/languages/<code>.yml`,
avec exactement la même structure que les fichiers de langue de KingdomsX (les arbres `command:` et
`specialties:`, les mêmes clés). Une clé absente retombe sur le texte anglais intégré à l'extension,
donc une traduction partielle ne casse rien.

Un fichier est créé au démarrage pour chaque langue installée sur le serveur : le français est
fourni traduit, les autres sont générés à partir de l'anglais, prêts à traduire.

**Le cas du français** : KingdomsX ne propose pas de locale française — les langues disponibles sont
`en, de, es, it, pt, pl, ru, cs, hu, tr, uk, vi, zh`. Un serveur francophone tourne donc en anglais.
D'où la correspondance livrée par défaut :

```yaml
messages:
  language-files:
    EN: fr        # la langue EN lit fr.yml
```

Supprimez cette ligne pour repasser les messages en anglais, ou pointez une autre langue vers le
fichier de votre choix (`DE: de`, …).

Le reste du texte visible en jeu suit les mécanismes de KingdomsX :

| Quoi | Où | Par langue ? |
|---|---|---|
| Messages de l'extension | `specialties/languages/<code>.yml` | oui |
| Titres et libellés des menus | `languages/<langue>/guis/…` de KingdomsX | oui |
| Noms des spécialités, des ressources et des recettes | `specialties.yml` | non — contenu du serveur |

## Configuration — `specialties.yml`

```yaml
selection:
  require-confirmation: true   # creation et rattrapage

  # Rattrapage uniquement (/k specialty choose) :
  king-only: true              # sinon, permission de royaume UPGRADE
  required-level: 1
  cost: { resource-points: 0, money: 0 }

extraction:
  resource-points-per-unit: 100   # points de ressources par unité produite
  multiplier: '1'                 # expression math., contexte du royaume
  max-per-collect: 0              # 0 = illimité
  carry-over-remainder: true      # reporte le reliquat sur la collecte suivante
  drop-if-inventory-full: true

messages:
  language-files: { EN: fr }      # voir la section Langues
```

Le carburant, la cadence et le rendement des extracteurs se règlent côté KingdomsX dans
`Structures/extractor.yml`. Le coût, les limites et les blocs de la forge dans
`Structures/specialty-forge.yml`.

### Ajouter une recette

Sous `specialties.<spécialité>.recipes`. La forge n'a pas de grille de craft : une `shape` sert
uniquement à compter les ingrédients.

```yaml
      ma-recette:
        type: SHAPED              # SHAPED | SHAPELESS | ENCHANT
        display-name: '&cMa recette'
        description: [ '&7...' ]
        shape: [ 'RRR', 'RNR', ' S ' ]
        ingredients:              # une liste simple en SHAPELESS
          R: '@resource'          # la ressource unique de la spécialité
          N: NETHERITE_SWORD
          S: STICK
        result:
          material: NETHERITE_SWORD
          name: '&cLame'
          enchants: { SHARPNESS: 8 }        # au-delà du maximum vanilla
          attributes:                        # ce qui dépasse vraiment la netherite
            GENERIC_ATTACK_DAMAGE: { amount: 12, operation: ADD_NUMBER, slot: HAND }
            GENERIC_ATTACK_SPEED:  { amount: -2.2, operation: ADD_NUMBER, slot: HAND }
```

Recette d'enchantement :

```yaml
      sharpness-viii:
        type: ENCHANT
        enchantment: SHARPNESS
        level: 8
        applies-to: [ '*_SWORD', '*_AXE' ]   # ANY, un matériau, ou un motif avec *
        icon: DIAMOND_SWORD
        ingredients: [ '@resource', '@resource', DIAMOND ]
```

Un objet (`result` ou `resource`) accepte : `material`, `amount`, `name`, `lore`,
`custom-model-data`, `unbreakable`, `glow`, `enchants`, `stored-enchants`, `item-flags`, `potion`,
`attributes`.

**Attention aux attributs** : les définir remplace les statistiques de base de l'objet. Sur une
arme ou une armure, précisez donc toutes celles que vous voulez. Repères vanilla — épée netherite :
8 dégâts / -2.4 vitesse ; plastron netherite : 8 armure / 3 résistance / 0.1 résistance au recul.

## Notes techniques

- **Stockage** — deux métadonnées de royaume : `Specialties:SPECIALTY` et
  `Specialties:PRODUCTION_REMAINDER` (points de ressources pas encore convertis en une unité
  entière). Elles suivent la sauvegarde de KingdomsX.
- **Production** — un écouteur de `ExtractorCollectEvent` convertit les points de ressources que
  l'extracteur s'apprête à verser. Aucune tâche périodique, aucun bloc ajouté.
- **Création** — `KingdomCreateEvent` n'est pas annulable et arrive une fois le royaume existant.
  L'interception se fait donc un cran plus tôt, sur `KingdomsPreCommandEvent` : la commande
  `/k create` est retenue, le menu s'ouvre, puis la commande d'origine est rejouée telle quelle une
  fois la spécialité choisie. Les vérifications de permission et de cooldown de KingdomsX ont déjà
  eu lieu à ce stade, rien n'est court-circuité.
- **Rattrapage** — `/k specialty choose` ne sert qu'aux royaumes sans spécialité : ceux créés avant
  l'installation de l'extension, ceux créés par `/k admin create`, et ceux qu'un admin a
  réinitialisés. `selection.king-only`, `required-level` et `cost` ne s'appliquent qu'à ce cas.
- **Structure** — un `StructureType` enregistré dans le registre de KingdomsX. Le fichier
  `Structures/specialty-forge.yml` n'est écrit qu'après l'extraction des structures par défaut :
  créer ce dossier trop tôt empêcherait KingdomsX d'y déposer les siennes sur un serveur neuf.
- **Langues** — l'extension enregistre elle-même ses messages compilés pour chaque langue
  installée, en lisant son propre dossier. Les entrées manquantes retombent sur l'anglais du code,
  donc aucun message ne peut rester non résolu, même avec un fichier vide ou incomplet.
- **Attributs** — appliqués par réflexion. Entre 1.16 et 1.21, Bukkit a changé la recherche des
  attributs (énumération, puis registre, préfixe `GENERIC_` abandonné) et la construction des
  modificateurs (`UUID + EquipmentSlot`, puis `NamespacedKey + EquipmentSlotGroup`). Les deux
  chemins sont tentés, et l'option est ignorée avec un avertissement si aucun ne convient.
- **Ingrédients** — un ingrédient « matériau brut » ne consomme jamais une ressource de spécialité
  du même matériau : la ressource est reconnue à son tag persistant.

## API

```java
Specialty specialty = KingdomSpecialties.getSpecialty(kingdom);      // null si non choisie
List<Extractor> extractors = KingdomSpecialties.getExtractors(kingdom);
int fueled = KingdomSpecialties.countFueledExtractors(kingdom);
long pending = KingdomSpecialties.getProductionRemainder(kingdom);
List<SpecialtyRecipe> recipes = specialty.getRecipes();
```

## Structure du projet

```
src/main/java/org/kingdoms/specialties/
├── SpecialtiesAddon.java          point d'entrée de l'addon
├── commands/                      /k specialty … et /k admin specialty
├── config/                        config, langue, chemins de GUI
├── data/                          Specialty, recettes, métadonnées
├── gui/                           menu de sélection, menu de la forge
├── items/                         parseur d'items, attributs, tag des ressources
├── managers/                      extraction, forge, sélection
└── structure/                     type de structure et installation du fichier
src/main/resources/
├── specialties.yml
├── Structures/specialty-forge.yml
└── guis/
    ├── specialties/selection.yml
    └── structures/specialty-forge.yml
```
