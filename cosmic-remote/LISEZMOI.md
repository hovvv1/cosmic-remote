# Cosmic Remote (Fabric, Minecraft 26.3)

Mod indépendant (il n'a pas besoin de The Shooting Star). Il ajoute **une télécommande** avec 4 compétences :

| Code | Nom | Effet |
|---|---|---|
| SA-03 | Singularity | Trou noir : marque au sol, disque d'accrétion, aspiration des mobs et objets, dévoration des blocs, effondrement puis explosion et cratère à cœur de crying obsidian |
| SA-04 | Perseid | Météorite : arrive en biais avec une longue traînée, impact, cratère avec un noyau de magma |
| SA-05 | Helios | Soleil : se lève, descend, brûle tout, fait fondre la glace, évapore l'eau, vitrifie le sable, puis nova et cœur de glowstone |
| SA-06 | Nova | Supernova : étoile qui se contracte, puis coquilles de lumière qui balaient la zone |

## Utilisation
- Clic droit : lance la compétence sur ton viseur.
- Accroupi + clic droit : change de compétence.
- L'objet est dans l'onglet Outils et utilitaires du mode créatif, ou : `/give @s cosmic_remote:cosmic_remote`

## Réglages
Le fichier `config/cosmic_remote.json` est créé au premier lancement : puissance, dégâts et vitesse par compétence, délai de recharge, option `noCooldown`, option `breakBlocks` (false = aucun bloc détruit), option `hurtCaster`.

## Compiler (je n'ai pas pu le faire à ta place)
1. Va sur https://fabricmc.net/develop , choisis Minecraft 26.3 et note les versions de Loom, Fabric Loader et Fabric API.
2. Mets-les dans `gradle.properties` (`loom_version`, `loader_version`, `fabric_api_version`). Si le template généré utilise un autre nom de plugin ou une autre structure dans `build.gradle`, recopie la sienne et garde mon dossier `src`.
3. Il faut Java 25. Lance `gradle build` (ou `./gradlew build` si tu as le wrapper du template).
4. Le jar est dans `build/libs/cosmic-remote-1.0.0.jar` (prends celui sans `-sources`). Mets-le dans `mods/` avec Fabric API.

Sans PC puissant ou sans Java : crée un dépôt GitHub avec ce dossier, le workflow `.github/workflows/build.yml` compile le jar et tu le télécharges dans l'onglet Actions.

## Si la compilation échoue
Ce code n'a jamais été compilé contre Minecraft 26.3. Envoie-moi les erreurs telles quelles : les appels fragiles (particules, sons) sont concentrés dans `Fx.java`, et la poussée des entités dans `Effect.push()`.
