# Procédure de validation

Exécuter les scénarios sur **chacun** des loaders, dans un monde de test.
Les tests unitaires ne remplacent pas une session réelle de combat et de connexion.

## Compilation et tests automatisés

```powershell
.\gradlew.bat build
.\gradlew.bat :common:test
```

Rapports : `common/build/reports/tests/test/index.html` et
`common/build/test-results/test/TEST-*.xml`.

Couverture : limites des paliers, sélection spéciale du mode fidèle, mode simplifié,
désactivation indépendante, composition mathématique des probabilités, taux invalides,
doublons et resurrection/capture, joueurs/espèces indépendants, chaînes, sauvegarde
NBT sur disque et rechargement, format inconnu, paramètres invalides, débordement
des compteurs, attribution en combat multiple, exclusion PvP et combats PNJ.

## Démarrage

1. Lancer ` :fabric:runServer ` ou ` :neoforge:runServer ` (sans espaces autour du nom).
2. Si demandé, lire le contrat indiqué dans `run/eula.txt` et l'accepter soi-même.
3. Attendre `Done` **et** `Cobblemon Shasse actif : mode FAITHFUL`.
4. Dans la console, exécuter `shinyhunt methods`, `shinyhunt reload`, puis :
   `shinyhunt admin set 00000000-0000-0000-0000-000000000001 pikachu 500`.
5. Vérifier les messages de succès et l'absence d'exception Shasse.
6. Exécuter `stop`, attendre la sauvegarde finale, puis relancer.
7. Vérifier la présence de `world/data/cobblemon_shasse.dat` ; décoder son NBT et
   vérifier que ce joueur a `cobblemon:pikachu.totalBattledCount=500`.
8. Répéter avec un second monde ; le compteur du premier ne doit pas apparaître.

En développement, NeoForge peut désactiver automatiquement certaines vérifications
vanilla et l'authentification. Utiliser les règles habituelles de votre serveur
pour un déploiement réel. Arrêter les serveurs de test après vérification.

## Dépendance obligatoire

La propriété Gradle suivante supprime Cobblemon du **lancement** de développement
tout en le conservant pour la compilation :

```powershell
.\gradlew.bat :fabric:runServer -PwithoutCobblemon
.\gradlew.bat :neoforge:runServer -PwithoutCobblemon
```

Résultat attendu : refus du loader avec un message indiquant que `cobblemon`
est requis par `cobblemon_shasse`. Le monde ne doit pas démarrer.

Pour tester une version incompatible, préparer une copie du serveur installable,
remplacer Cobblemon 1.8.1 par une autre version et vérifier le refus avant `Done`.
Ne pas modifier votre serveur principal pour ce test.

## Connexion sans Shasse sur le client

1. Installer Minecraft 1.21.1, le loader correspondant, Cobblemon 1.8.1 et ses
   dépendances sur un client.
2. Laisser **Cobblemon Shasse absent** de ses mods.
3. Rejoindre le serveur et vérifier que la connexion fonctionne.
4. Exécuter `/shinyhunt methods` et `/shinyhunt stats pikachu`.
5. Avec un compte non opérateur, vérifier que les branches `reload` et `admin`
   sont refusées. Opérateur niveau 2 : vérifier qu'elles fonctionnent.

## KO, captures et provenance

1. Garder `allowArtificialSpawns=false`. Faire apparaître naturellement une espèce
   après le démarrage du mod, près d'un joueur connecté.
2. Relever `/shinyhunt stats <espèce>`, puis la vaincre dans un combat Cobblemon.
   Attendre l'animation KO : `defeatedCount` et `totalBattledCount` doivent augmenter de 1.
3. Capturer naturellement un autre individu : `capturedCount` et le total augmentent
   de 1 après la capture terminée. Les tentatives échouées ne changent rien.
4. Vaincre un Pokémon possédé en PvP : aucun compteur ne doit changer.
5. Combattre un PNJ : aucun compteur par défaut. Même avec l'option PNJ, les Pokémon
   possédés restent exclus.
6. Utiliser la commande Cobblemon pour invoquer un Pokémon sauvage : aucun compteur
   par défaut. Activer `allowArtificialSpawns=true`, recharger et recommencer :
   la comptabilisation doit être autorisée, sans bonus shiny artificiel.
7. Décharger/recharger le chunk d'un Pokémon naturel, puis le capturer :
   sa provenance et le comptage doivent survivre.
8. Désactiver séparément `countWildDefeats`, puis `countWildCaptures` et vérifier
   que le résultat concerné n'incrémente plus les compteurs.
9. Arrêter proprement et relancer après ces interactions ; retrouver les statistiques.

## Paliers et génération

1. Pour chaque valeur 0, 1, 20, 50, 100, 200, 300 et 500, utiliser
   `/shinyhunt admin set <joueur> <espèce> <valeur>`.
2. Vérifier `stats` : bonus 0, 1, 1, 2, 3, 4, 5 et 6.
3. Mettre `mode=SIMPLIFIED` et un taux de test Cobblemon de 100 pour observer un
   effet sur un échantillon raisonnable. Recharger le taux Cobblemon avec sa
   commande propre, ou redémarrer selon les possibilités de la version utilisée.
4. Mettre une unique `brilliantTiers` à `[{"count":0,"probability":1.0}]` et repasser
   en `FAITHFUL` : le comportement shiny doit suivre le mode simplifié.
   Avec la probabilité à 0, le taux reste celui de Cobblemon.
5. Ne pas conclure à un taux à partir de quelques rencontres : instrumenter la
   fonction de calcul au débogueur ou recueillir un grand échantillon.
6. Vérifier qu'un Pokémon déjà visible conserve son état après une modification
   de compteur/configuration. Vérifier qu'un Pokémon déjà shiny le reste.
7. Répéter avec deux joueurs aux compteurs différents. L'origine du spawner doit
   déterminer le bonus ; les deux clients voient le même Pokémon shiny/non shiny.
8. Sans joueur fiable au calcul, vérifier que le dénominateur reste normal.
9. Rétablir les taux et paliers par défaut après les essais.

## Chaînes et combats multiples

1. Activer uniquement le suivi des chaînes ; capturer deux fois la même espèce,
   puis une autre. La chaîne précédente passe à 0 et la nouvelle à 1.
2. Une tentative échouée ne change pas la chaîne. Déconnexion/redémarrage :
   elle reprend à la valeur conservée. Aucun bonus shiny lié à la chaîne.
3. En combat multiple, provoquer deux KO et des changements de Pokémon :
   chaque cible compte une seule fois pour le joueur identifié par le contexte.
4. Si le contexte ne permet pas d'identifier un joueur parmi plusieurs opposants,
   aucun joueur ne reçoit de KO arbitraire.
5. Pour simuler un événement dupliqué via un outil de développement, publier
   deux fois le même KO/capture ; un seul incrément doit être enregistré.
