# Kingdoms Addon — Specialties

Extension (addon) pour [KingdomsX](https://github.com/CryptoMorin/KingdomsX).

Chaque royaume choisit **une spécialité parmi trois, au moment de sa création** : `/k create <nom>`
ouvre le menu des spécialités et le royaume n'est fondé qu'une fois le choix fait. La spécialité
donne accès à une **ressource unique**, produite par les **extracteurs de KingdomsX**, qui sert à
fabriquer des objets exclusifs à la **Forge de Spécialité**, une structure de royaume ajoutée par
l'extension. Le choix est **définitif** : seul un administrateur peut le modifier.

| Spécialité                | Ressource unique | Débloque |
|---------------------------|------------------|----------|
| `weaponsmith` — Forgeron  | Carbone          | épée, hache et lance de netherite, **+2 dégâts** |
| `armorer` — Armurier      | Mitril           | les 4 pièces d'armure netherite, **+2 armure et +1 résistance** |
| `alchemist` — Alchimiste  | Goo              | potion de wither, potion de force III, pomme d'or sublimée |

Les objets fabriqués gardent le **visuel et les caractéristiques de l'objet d'origine** — une épée
de carbone reste une épée de netherite — avec en plus le reflet enchanté et le bonus ci-dessus.

---

## Installation

1. Compiler : `mvn package` → `target/Kingdoms-Addon-Specialties-1.0.0.jar`
2. Déposer le jar dans le dossier `plugins/` du serveur (à côté de KingdomsX).
3. Redémarrer. L'extension installe :
   - `plugins/Kingdoms/specialties.yml` — la configuration ;
   - `plugins/Kingdoms/specialties/languages/<code>.yml` — un fichier de messages par langue ;
   - `plugins/Kingdoms/Structures/specialty-forge.yml` — la structure ;
   - les menus dans les dossiers GUI de KingdomsX ;
   - une entrée `specialty-forge` dans `guis/<langue>/structures/nexus/structures.yml`, le menu
     d'achat des structures — sans elle la forge n'y apparaît pas (voir les notes techniques).

Prérequis : Minecraft **1.16+**, KingdomsX **1.17.27.3**. Folia est supporté.

**Recompilez à chaque montée de version de KingdomsX, avant de déployer.** Les signatures bougent
réellement : 1.17.27.3 a renommé `KingdomItemInteractEvent` en `KingdomBuildingInteractEvent` et
`getKingdomItem()` en `getBuilding()`, ce qui donne un `NoSuchMethodError` à l'ouverture de la forge
si l'extension a été compilée contre une version antérieure. La compilation est le moyen le plus
rapide de trouver ces ruptures. Maven Central s'arrête à 1.17.18.1-BETA, donc installez le jar de
votre serveur — c'est aussi la référence la plus fidèle :

```bash
mvn install:install-file -Dfile=<serveur>/plugins/KingdomsX-1.17.27.3.jar -DgroupId=com.github.cryptomorin -DartifactId=kingdoms -Dversion=1.17.27.3 -Dpackaging=jar
```

La **lance** (`NETHERITE_SPEAR`) n'existe que depuis la **1.21.11**. Sur un serveur plus ancien, sa
recette est ignorée avec un avertissement dans la console ; les deux autres armes fonctionnent
normalement.

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

1. **Fonder** — `/k create <nom>` ne crée rien tout de suite : le menu des trois spécialités
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

### Ce que fabrique chaque spécialité

**Forgeron — Carbone.** Une arme de netherite + 4 carbone donne la même arme, reflet enchanté et
2 dégâts de plus : épée 10 au lieu de 8, hache 12 au lieu de 10, lance 7 au lieu de 5. Vitesse
d'attaque et portée inchangées.

**Armurier — Mitril.** Une pièce d'armure netherite + 4 mitril donne la même pièce, reflet
enchanté, +2 d'armure et +1 de résistance aux armures : casque 5/4, plastron 10/4, pantalon 8/4,
bottes 5/4 (armure / résistance). Résistance au recul inchangée.

**Alchimiste — Goo.** Trois transformations, toutes à un seul goo :

| Ingrédient | Résultat |
|---|---|
| Potion de poison | Potion de wither, **même durée et même niveau** que la fiole utilisée |
| Potion de force II | Potion de force III, **même durée** |
| Pomme d'or | Pomme d'or sublimée : Absorption **II** et Régénération **IV**, contre I et II |

Les potions de wither et de force III reprennent leurs valeurs de la fiole consommée, pas de la
configuration : un poison allongé donne un wither allongé, un poison II donne un wither II.

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

Sous `specialties.<spécialité>.recipes`. Trois types : `SHAPED`, `SHAPELESS` et `TRANSMUTE`.
La forge n'a pas de grille de craft : une `shape` sert uniquement à compter les ingrédients.

```yaml
      ma-recette:
        type: SHAPELESS           # SHAPED | SHAPELESS | TRANSMUTE
        display-name: '&cMa recette'
        description: [ '&7...' ]
        ingredients:              # repeter une entree en demande plusieurs
          - NETHERITE_SWORD
          - '@resource'           # la ressource unique de la specialite
          - '@resource'
        result:
          material: NETHERITE_SWORD
          name: '&cLame'
          glint: true                        # le reflet enchante
          attributes:                        # ce qui dépasse vraiment la netherite
            ATTACK_DAMAGE: { amount: 9, operation: ADD_NUMBER, slot: HAND }
            ATTACK_SPEED:  { amount: -2.4, operation: ADD_NUMBER, slot: HAND }
```

Recette de transmutation — une potion `source` est consommée, et le résultat **reprend la durée et
le niveau** de son effet :

```yaml
      potion-de-wither:
        type: TRANSMUTE
        icon: POTION
        source:
          material: POTION
          effect: POISON          # la potion doit porter cet effet
          amplifier: 1            # optionnel : ce niveau exactement
          name: '&2Potion de Poison'
        ingredients: [ '@resource' ]
        transmute:
          effect: WITHER          # l'effet du resultat
          amplifier-shift: 0      # +1 pour monter d'un niveau
        result:
          material: POTION
          name: '&8Potion de Wither'
          glint: true
          color: '#3b3b3b'
```

Un objet (`result` ou `resource`) se décrit **avec la syntaxe d'objet de KingdomsX** : c'est son
désérialiseur qui est appelé, lui-même bâti sur `XItemStack` de XSeries. Donc `material`, `amount`,
`name`, `lore`, `enchants`, `stored-enchants`, `flags`, `attributes`, `effects`, `color`,
`custom-model-data`, `unbreakable`, `glow`, `damage`, `skull`, `trim`, `item-model`, `nbt`… — les
fichiers du plugin principal font office de référence. Deux options s'y ajoutent, propres à
l'extension : `glint` et `consume-effects`.

**Attention aux attributs** : les noms s'écrivent indifféremment `ARMOR` ou `GENERIC_ARMOR`,
XSeries fait la correspondance avec ce que la version du serveur utilise. En revanche, les définir
remplace les statistiques de base de l'objet. Sur une
arme ou une armure, précisez donc toutes celles que vous voulez — la portée d'une lance comprise.
Repères vanilla (valeur affichée entre parenthèses) — épée netherite : 7 (8) dégâts / -2.4 (1.6)
vitesse ; hache netherite : 9 (10) / -3.0 (1.0) ; lance netherite : 4 (5) / -3.13 (0.87) ;
plastron netherite : 8 armure / 3 résistance / 0.1 résistance au recul ; casque et bottes : 3
armure ; pantalon : 6 armure.

**`glint`** donne le reflet enchanté par le composant prévu pour ça (1.20.5+). À préférer à `glow`,
qui obtient le même reflet avec un enchantement masqué — et donne donc une Solidité I bien réelle à
l'objet, ce qui casse le « mêmes caractéristiques que la netherite ».

**`consume-effects`** donne des effets à un **aliment** quand il est mangé. Les effets d'une pomme
d'or sont codés en dur dans le jeu et ne s'éditent pas : l'extension les applique elle-même à la
consommation, juste avant ceux du jeu, qui ne peuvent alors plus les affaiblir. Même forme que
`effects` — `EFFET, secondes, niveau`, le niveau partant de 1 :

```yaml
          consume-effects:
            - 'ABSORPTION, 120, 2'    # Absorption II, deux minutes
            - 'REGENERATION, 5, 4'    # Regeneration IV, cinq secondes
```

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
- **Menu d'achat** — enregistrer la structure ne suffit pas à la rendre achetable. KingdomsX
  parcourt les structures du registre et, pour chacune, cherche une option **du même nom** dans
  `guis/<langue>/structures/nexus/structures.yml` ; une structure sans entrée y est ignorée sans le
  moindre message en console. L'extension ajoute donc la sienne au démarrage, une seule fois, dans
  le menu de chaque langue installée, puis n'y retouche jamais : l'entrée devient une ligne de
  configuration du serveur comme une autre, libre d'être déplacée, restylée ou supprimée. Le slot
  est choisi parmi ceux que le menu n'utilise pas encore.
- **Langues** — l'extension enregistre elle-même ses messages compilés pour chaque langue
  installée, en lisant son propre dossier. Les entrées manquantes retombent sur l'anglais du code,
  donc aucun message ne peut rester non résolu, même avec un fichier vide ou incomplet.
- **Objets** — construits par `KingdomsItemDeserializer`, le désérialiseur de KingdomsX, qui
  enveloppe `XItemStack` de XSeries. Matériaux, effets et attributs passent par `XMaterial`,
  `XPotion` et `XAttribute` : c'est là que vit la connaissance des renommages entre versions, et la
  réécrire à la main revient à s'approprier la dérive. L'extension n'ajoute que ce qui n'a pas
  d'équivalent — `glint` et `consume-effects`.
- **XSeries** — KingdomsX télécharge la bibliothèque, la relocalise en `org.kingdoms.libs.xseries`
  et l'injecte dans **son propre** classloader de plugin, donc un addon qui dépend de Kingdoms la
  voit. D'où la dépendance `provided` plus la relocalisation du shade plugin : on compile contre
  elle et on réécrit nos références vers le paquet que KingdomsX a chargé, sans embarquer une seule
  de ses classes. En embarquer une copie ne donnerait que deux XSeries sur le serveur.
- **Lecture des potions** — la seule direction que XSeries ne couvre pas : `XItemStack` transforme
  une configuration en objet, rien ne transforme un objet en la liste des effets qu'il porte, ce
  dont une transmutation a précisément besoin pour recopier la durée et le niveau de la fiole. La
  `source` est d'ailleurs reconnue à son effet et non à son matériau, toutes les potions du jeu
  partageant `POTION`. Le reste passe par réflexion, 1.20.5 ayant remplacé `getBasePotionData()` —
  un type plus les drapeaux *extended* / *upgraded* — par `getBasePotionType()`, où chaque variante
  connaît ses propres effets.
- **Ingrédients** — un ingrédient « matériau brut » ne consomme jamais un objet de spécialité du
  même matériau : la ressource et les objets sortis de la forge portent un tag persistant. Améliorer
  une épée de carbone déjà améliorée est donc impossible, plutôt que coûteux.

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
├── items/                         fabrique d'objets, lecture des potions, tags
├── managers/                      extraction, forge, sélection, consommation
└── structure/                     type de structure et installation du fichier
src/main/resources/
├── specialties.yml
├── Structures/specialty-forge.yml
└── guis/
    ├── specialties/selection.yml
    └── structures/specialty-forge.yml
```
