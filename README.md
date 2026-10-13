# Cobblemon Shasse

Mod **Kotlin**, exclusivement destiné aux serveurs dédiés, pour Minecraft 1.21.1.
Il compte les Pokémon sauvages vaincus en combat Cobblemon et capturés, conserve
les statistiques par UUID et espèce, et ajoute des tirages shiny selon la méthode
Épée/Bouclier décrite dans le cahier des charges.

Deux JAR indépendants sont produits : Fabric et NeoForge. Les clients ont besoin
de Cobblemon et de ses dépendances habituelles, **pas de Cobblemon Shasse**.
Aucun bloc, paquet réseau ou écran client n'est ajouté.

## Versions verrouillées

| Composant | Version |
|---|---|
| Minecraft / JDK | 1.21.1 / Java 21 |
| Kotlin / Gradle | 2.2.20 / 8.14.3 |
| Cobblemon | 1.8.1, strictement |
| Fabric Loader / API | 0.17.2 / 0.116.6+1.21.1 |
| Fabric Language Kotlin | 1.13.6+kotlin.2.2.20 |
| NeoForge | 21.1.249 |
| Kotlin for Forge | 5.10.0 |
| Fabric Loom / ModDevGradle | 1.11.8 / 2.0.147 |

Les artefacts Cobblemon 1.8.1 sont vérifiés sur Modrinth :
[Fabric gBW3vLC7](https://modrinth.com/mod/cobblemon/version/gBW3vLC7) et
[NeoForge 7otgw3aH](https://modrinth.com/mod/cobblemon/version/7otgw3aH).
Une autre version de Cobblemon nécessite une nouvelle vérification des API et
une mise à jour des dépendances Gradle **et** des métadonnées des deux loaders.
Java 21 est aussi imposé par Cobblemon ; ne pas lancer avec Java 25.

## Développement et compilation

Installer un JDK 21, définir `JAVA_HOME`, puis ouvrir la racine comme projet Gradle
dans IntelliJ IDEA. Le wrapper télécharge Gradle ; aucune installation globale n'est requise.

Sur Windows PowerShell, avec le JDK déjà présent sur cette machine :

```powershell
$env:JAVA_HOME = 'C:\Users\Perodeau\.jdks\ms-21.0.12.1'
.\gradlew.bat build
.\gradlew.bat :common:test
.\gradlew.bat :fabric:build
.\gradlew.bat :neoforge:build
.\gradlew.bat :fabric:runServer
.\gradlew.bat :neoforge:runServer
```

Sur Linux/macOS, utiliser `./gradlew`. Le chemin local du JDK n'est pas enregistré
dans les paramètres Gradle du projet. La CI utilise Java 21 et compile les deux variantes.

Livrables installables :

- `fabric/build/libs/cobblemon-shasse-fabric-0.1.0.jar`
- `neoforge/build/libs/cobblemon-shasse-neoforge-0.1.0.jar`

Les fichiers `-sources.jar` et le module `common` ne sont pas des mods à installer.
Les bibliothèques Graal/ICU/Mongo ajoutées au lancement Fabric sont nécessaires
uniquement au développement : Loom n'ajoute pas automatiquement toutes les
bibliothèques imbriquées dans Cobblemon. Elles ne sont pas incluses dans nos JAR.

## Installation

**Fabric** : serveur Minecraft 1.21.1, Fabric Loader >= 0.17.2, Fabric API compatible,
Fabric Language Kotlin >= 1.13.6, Cobblemon **1.8.1**, puis notre JAR Fabric dans `mods/`.

**NeoForge** : serveur Minecraft 1.21.1, NeoForge 21.1.249, Kotlin for Forge >= 5.10.0,
Cobblemon **1.8.1**, puis notre JAR NeoForge dans `mods/`.

Ne pas mélanger les JAR des loaders. Cobblemon est une dépendance obligatoire :
un loader doit refuser le chargement lorsqu'il est absent ou incompatible.
Les environnements de développement sont séparés dans `fabric/run` et `neoforge/run`.
Lire et accepter soi-même le contrat Minecraft si le serveur le demande.

## Organisation

```text
common/src/main/kotlin/rerebleue/cobblemon_shasse/
  config/       configuration JSON et validation
  data/         statistiques, chaînes et protection contre les doublons
  method/       interface, registre, méthode Épée/Bouclier et probabilités
  storage/      abstraction de stockage et données persistantes Minecraft
  integration/  événements et influence de génération Cobblemon
  command/      commandes Brigadier communes
fabric/src/main/kotlin/     initialisation et cycle serveur Fabric
neoforge/src/main/kotlin/   initialisation et cycle serveur NeoForge
common/src/test/kotlin/    tests de logique et sauvegarde NBT
```

Les sources communes sont compilées directement par chaque loader, avec les
mappings Mojang correspondants. Il n'y a pas de copie de logique métier et pas
de dépendance à Architectury à installer. `common` fournit aussi le point de
compilation et les tests communs. Pour ajouter une méthode, implémenter
`HuntingMethod`, l'enregistrer et lui fournir sa configuration et ses événements.

## Fonctionnement de la méthode

Les compteurs sont individuels, identifiés par UUID Minecraft et identifiant
d'espèce (ex. `cobblemon:pikachu`). Les formes d'une même espèce partagent les compteurs.

| Total éligible | Tirages supplémentaires |
|---|---|
| 0 | 0 |
| 1 | +1 |
| 20 | +1 |
| 50 | +2 |
| 100 | +3 |
| 200 | +4 |
| 300 | +5 |
| 500+ | +6 |

Ce tableau suit **les valeurs demandées dans le cahier des charges**.
`FAITHFUL`, par défaut, sélectionne une apparition spéciale avec une probabilité
de 0%, 1,5%, 2%, 2,5% et 3% aux paliers 0, 1, 20, 50 et 100. Seules ces
apparitions reçoivent les tirages supplémentaires. L'aura visuelle, les IV
garantis, les Watts et les capacités Œuf des jeux officiels ne sont pas reproduits.
Il s'agit donc d'une adaptation de leur mécanisme shiny, pas d'une reproduction
complète de toutes les propriétés d'un Pokémon brillant.

`SIMPLIFIED` applique les tirages supplémentaires à toutes les apparitions
naturelles éligibles. Les paliers et les probabilités spéciales sont configurables.

Cobblemon exprime `shinyRate` comme un **dénominateur** : 4096 signifie 1/4096.
Le mod conserve le taux déjà modifié par les autres abonnés à l'événement, puis
ajoute des tirages indépendants au taux de base configuré par Cobblemon.
Pour un taux courant `r`, une base `b` et `n` tirages :
`P = 1 - (1 - 1/r) * (1 - 1/b)^n`.
Aucun taux fixe 1/4096 n'est imposé. Les taux désactivés/invalides ne sont pas
transformés. Le mod n'écrit jamais lui-même dans `pokemon.shiny`.

## API Cobblemon vérifiées et attribution

Les signatures sont vérifiées dans les
[sources officielles 1.8.1](https://gitlab.com/cable-mc/cobblemon/-/tree/1.8.1).

- `BATTLE_FAINTED` fournit le Pokémon original, les acteurs et le contexte de dégâts.
  Le responsable du contexte reçoit le KO. À défaut, un unique joueur opposé
  reçoit le KO ; en combat multiple ambigu, personne n'est choisi arbitrairement.
- `POKE_BALL_CAPTURE_CALCULATED` mémorise l'éligibilité avant le changement de propriétaire.
  Seul `POKEMON_CAPTURED` enregistre une capture réellement terminée. Une capture
  calculée réussie mais interrompue ne compte pas ; un échec ne change rien.
- `PlayerSpawnerFactory.influenceBuilders` et `SpawningInfluence.affectAction`
  marquent les apparitions naturelles **avant** `PokemonProperties.roll`, via une
  propriété personnalisée sérialisable. Cela couvre aussi les troupeaux.
- `SHINY_CHANCE_CALCULATION.addModificationFunction` reçoit le joueur du contexte
  et le dénominateur à modifier. L'état shiny appartient au Pokémon partagé,
  sans différence selon le client qui l'observe.

`COBBLEMON_CONTEXT` exige le joueur fourni par Cobblemon et vérifie qu'il correspond
à l'origine naturelle. `CONTEXT_OR_SPAWNER` autorise aussi le joueur en ligne enregistré
par le spawner. Sans attribution fiable, le taux normal est conservé. Aucune
recherche du joueur le plus proche n'est effectuée.

Les joueurs propriétaires, le PvP et les KO hors combat Cobblemon sont exclus.
Les combats comportant des PNJ sont exclus par défaut. `allowTrainerBattles`
autorise les combats mixtes comportant des PNJ, mais seuls les Pokémon sauvages
restent comptabilisables. Les Pokémon appartenant à un dresseur ne deviennent
pas éligibles.

Une provenance naturelle persistante permet de continuer à compter après un
déchargement de chunk. Les Pokémon apparus avant l'installation, les commandes,
les fossiles, habitats et spawners tiers sans cette provenance sont exclus par
défaut. `allowArtificialSpawns` autorise leur **comptabilisation**, sous réserve
qu'ils soient sauvages ; leur génération shiny ne reçoit aucun bonus sans contexte
naturel vérifié. Un administrateur peut explicitement écrire la propriété interne
`shasse_natural_origin` : elle ne constitue pas une barrière contre les opérateurs.

Des marqueurs persistants sur chaque Pokémon empêchent les doublons de KO et de
capture. Une éventuelle résurrection suivie d'une capture peut augmenter les
deux compteurs bruts, mais ne donne qu'une interaction éligible à un même joueur.
La chaîne de captures reste indépendante, désactivée par défaut, et **ne donne
aucun bonus shiny** dans cette version. Capturer une autre espèce remet la chaîne
précédente à zéro ; les échecs et déconnexions ne la modifient pas.

## Configuration

`config/cobblemon-shasse.json` est créé au démarrage complet du serveur.
Voici une configuration partielle valide ; les champs omis conservent leurs valeurs par défaut :

```json
{
  "enabled": true,
  "swordShield": {
    "enabled": true,
    "mode": "FAITHFUL",
    "maxCountForBonus": 500,
    "countWildDefeats": true,
    "countWildCaptures": true,
    "allowTrainerBattles": false,
    "allowArtificialSpawns": false,
    "attribution": "COBBLEMON_CONTEXT"
  },
  "captureChain": { "enabled": false },
  "persistence": { "autosaveEnabled": true }
}
```

Le fichier généré contient aussi `rollTiers` (`count`, `extraRolls`) et
`brilliantTiers` (`count`, `probability`, entre 0 et 1).
Les listes doivent commencer à 0 et être strictement ordonnées ; au plus 64 tirages.
Les deux options `countWild...` désactivent le comptage du résultat concerné.
Elles n'effacent pas l'historique. Une capture peut toujours faire progresser
une chaîne activée indépendamment.

Configuration invalide au démarrage : shasse désactivée, fichier conservé et
erreur journalisée. Rechargement invalide : paramètres précédents conservés.
Les modifications ne réévaluent pas les Pokémon existants.

Le taux de base reste dans `config/cobblemon/main.json`, paramètre `shinyRate`.
Conserver les autres paramètres Cobblemon souhaités ; Shasse ne réécrit pas ce fichier.

## Sauvegarde

Fichier `<monde>/data/cobblemon_shasse.dat`, NBT compressé, format version 1.
Un composé distinct par UUID contient les espèces et la dernière chaîne.
Chaque monde conserve son propre historique. Pas de base de données externe.
Minecraft assure les écritures aux sauvegardes de monde et à l'arrêt propre.
Toutes les mutations sont effectuées sur le thread serveur ; les événements
asynchrones y sont reportés. Aucun fichier n'est écrit lors d'un KO ou d'une capture.

`autosaveEnabled=false` retarde le marquage des nouvelles modifications jusqu'à
l'arrêt propre ; des données déjà marquées peuvent encore être sauvegardées par
Minecraft. Un arrêt brutal peut perdre la progression depuis la dernière sauvegarde.
Un fichier corrompu ou d'une version inconnue provoque une erreur de démarrage
du service : il n'est pas remplacé silencieusement par des statistiques vides.
Conserver une sauvegarde du monde avant toute intervention manuelle.

## Commandes

| Commande | Accès |
|---|---|
| `/shinyhunt stats` | Joueur ; affiche jusqu'à 20 espèces |
| `/shinyhunt stats <pokemon>` | Joueur ; espèce exacte |
| `/shinyhunt methods` | Joueur ou console |
| `/shinyhunt reload` | Opérateur niveau 2 / console |
| `/shinyhunt admin reset <player> <pokemon>` | Niveau 2 ; réinitialise l'espèce et sa chaîne |
| `/shinyhunt admin set <player> <pokemon> <count>` | Niveau 2 ; modifie uniquement le total éligible |

`<player>` accepte le nom d'un joueur connecté ou un UUID, y compris hors ligne.
`<pokemon>` accepte `pikachu` ou `cobblemon:pikachu`, avec suggestions des espèces
chargées par Cobblemon. `set` conserve les compteurs bruts KO/captures et sert à
tester les paliers. Il ne transforme pas les Pokémon existants.

## Tests

Voir [la procédure de validation](docs/TESTING.md), notamment les tests en jeu qui
nécessitent un client Cobblemon. Les tests unitaires ne prouvent pas à eux seuls
la compatibilité réseau ou les combats réels. Le rapport d'exécution se trouve
dans [VALIDATION.md](docs/VALIDATION.md).
