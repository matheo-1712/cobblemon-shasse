# Validation exécutée — 13 octobre 2026

Environnement : Windows, JDK Microsoft 21.0.12.1, Minecraft 1.21.1,
Cobblemon 1.8.1, Fabric Loader 0.17.2 et NeoForge 21.1.249.

## Résultats réellement observés

| Vérification | Résultat |
|---|---|
| Compilation Kotlin commune, Fabric et NeoForge | Réussie |
| Production des deux JAR installables | Réussie |
| Tests JUnit | 20 exécutés, 0 échec, 0 ignoré |
| Démarrage dédié Fabric avec Cobblemon | `Done` et service Shasse actif |
| Démarrage dédié NeoForge avec Cobblemon | `Done` et service Shasse actif |
| Commandes console `methods`, `reload`, `admin set` | Réussies sur les deux loaders |
| `admin reset` après redémarrage | Réussi sur les deux loaders |
| Sauvegarde après `stop` | Fichier NBT créé sur les deux loaders |
| Lecture des NBT réellement sauvegardés | UUID de test, total Pikachu = 500 sur les deux loaders |
| Rechargement et nouvelle sauvegarde Fabric | Pikachu = 500 conservé après modification d'une autre espèce |
| Rechargement et nouvelle sauvegarde NeoForge | Pikachu = 500 conservé après modification d'une autre espèce |
| Lancement sans Cobblemon sur Fabric | Refus explicite du loader avant démarrage |
| Lancement sans Cobblemon sur NeoForge | Refus explicite du loader avant démarrage |
| Vérification des espaces/erreurs de diff | `git diff --check` sans erreur de contenu |

Le test de persistance utilise le joueur fictif
`00000000-0000-0000-0000-000000000001` et le compteur éligible de
`cobblemon:pikachu`. Une commande administrative fixe le compteur à 500.
Après arrêt propre, le NBT est lu et vérifié. Après redémarrage, une commande
réinitialise uniquement Évoli puis un nouvel arrêt sauvegarde l'état.
La conservation de Pikachu à 500 dans cette nouvelle sauvegarde vérifie le
chargement du fichier dans le service, au-delà d'une simple lecture du fichier.

Les tests JUnit couvrent les paliers et leurs limites, les modes, la composition
des taux, les paramètres invalides, les doublons persistants, les chaînes, les
joueurs/espèces indépendants, les débordements, le format NBT, l'exclusion PvP
et PNJ et l'attribution des KO en combat multiple.

Les artefacts de développement ont été vérifiés ; une connexion à un serveur
installé à partir des JAR distribuables n'a pas été réalisée.

## Vérifications restant à faire en jeu

Aucune des vérifications suivantes n'est présentée comme exécutée :

- Connexion réelle d'un client Cobblemon sans Cobblemon Shasse.
- KO et capture via un vrai combat joué ; capture ratée/interrompue.
- Non-opérateur et permissions d'un joueur connecté.
- État shiny partagé entre plusieurs clients.
- Combats multiples/simultanés réels et Pokémon possédés.
- Effet statistique du taux sur une grande population de Pokémon naturels.
- Coexistence avec un autre addon qui modifie lui aussi les chances shiny.
- Refus avec un JAR d'une version de Cobblemon différente de 1.8.1.

La procédure reproductible est détaillée dans [TESTING.md](TESTING.md).

## Traces locales

Rapports JUnit : `common/build/reports/tests/test/index.html`.
Logs conservés, ignorés par Git : `.work/validation/` :

- `fabric-loaded.log` : démarrage, commandes, sauvegarde.
- `neoforge-loaded.log` : démarrage, commandes, sauvegarde.
- `fabric-restarted.log` : rechargement et arrêt propre.
- `neoforge-restarted.log` : rechargement et arrêt propre.
- `missing-cobblemon.log` : refus des deux loaders sans dépendance.

Les messages Cobblemon sur des Mixins d'intégrations optionnelles absentes et
certaines traductions sont visibles dans ces environnements de développement.
Ils n'ont pas empêché les démarrages constatés. Les premiers essais ont révélé
des dépendances Graal/ICU de développement manquantes et une interface Fabric
absente lors du remappage commun ; ces problèmes ont été corrigés avant les
démarrages réussis.
