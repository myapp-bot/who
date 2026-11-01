# WorshipFlow (Android Studio, GitHub, PWA)

Application pour dirigeant de louange : chants, cultes par date, bibliothèque (inspirations, versets, leçons), lecteur audio avec paroles synchronisées, statistiques, sauvegarde. Hors ligne, un seul fichier HTML (`www/index.html`).

Paramètres respectés : Capacitor 6, Android Gradle Plugin 8.2.1, Gradle 8.2.1, JDK 17, minSdk 24 (Android 7), compileSdk et targetSdk 34, applicationId `com.worshipflow.app`.

## Ouvrir dans Android Studio
1. Décompresser le projet, puis dans un terminal : `npm install`
2. `npx cap sync android`
3. `npx cap open android` (ou Android Studio > Open > dossier `android`)
4. Gradle JDK = jbr-17, puis Run ou Build > Build APK(s).

Après chaque modification de `www/index.html` : `npx cap sync android`.

## GitHub
1. Créer un dépôt, puis : `git init && git add . && git commit -m "WorshipFlow 5.2" && git branch -M main && git remote add origin <URL> && git push -u origin main`
2. L'onglet **Actions > Build APK** compile l'APK de test : télécharger l'artefact `WorshipFlow-debug-apk`.
3. Pour une version Play Store : signer un APK/AAB release (Build > Generate Signed Bundle) et garder le keystore hors du dépôt.

## PWA (installation depuis le navigateur)
Le dossier `www/` est aussi la PWA : `index.html`, `manifest.webmanifest`, `sw.js`, `icons/`. Héberger sur HTTPS (GitHub Pages, Netlify…), ouvrir l'adresse dans Chrome, puis « Installer l'application » (bouton aussi dans Réglages > À propos).

## Sauvegarde (Download/Backup)
Dans l'APK, l'application essaie d'écrire `Download/Backup/WorshipFlow-Backup-AAAA-MM-JJ.json`, puis `Documents/WorshipFlow/Backup/`, puis ouvre la feuille de partage (Drive, WhatsApp, Fichiers…). Sur Android 11 et plus, le système peut refuser l'écriture directe dans Download : le partage prend alors le relais. Dans un navigateur ou une PWA, le fichier va dans Téléchargements.
La sauvegarde automatique hebdomadaire (APK) inclut les données. Les MP3 peuvent être ajoutés en activant l’option dédiée ; cela peut rendre le fichier de sauvegarde volumineux.

## Icônes
Icônes PNG déjà intégrées (launcher, adaptive, splash). Pour les régénérer : placer `icon-only.png`, `icon-foreground.png`, `icon-background.png` dans `resources/` et lancer `npx @capacitor/assets generate --android`.

## Nouveautés 5.2
- Impression et partage d'un culte (liste seule, ou avec inspirations/versets et notes) : imprimer (navigateur), PDF, texte (WhatsApp…), copier. Dans l'APK, « Imprimer » génère un PDF et ouvre la feuille de partage (la fenêtre d'impression du WebView n'existe pas).
- Égaliseur (basses, voix, aigus) pendant la lecture des chants : Web Audio API, réglage mémorisé.
- Verset défilant sous le logo : personnalisable (ajout, modification, choix depuis les versets de la bibliothèque, vitesse, masquer). Toucher le texte pour l'éditer.
- Bibliothèque compacte, corbeille 30 jours, annuler/rétablir, import Word/Excel/CSV, glisser-déposer des chants d'un culte.

## Nouveautés 5.3 : télécharger des chants depuis tononkira.serasera.org
- Chants > icône nuage : recherche par titre et/ou auteur, aperçu et import (avec détection des doublons). La source est enregistrée dans les notes du chant.
- Dans l'APK, `CapacitorHttp` est activé (`capacitor.config.json`) : les requêtes passent par le réseau natif, sans blocage CORS. Dans un navigateur ou une PWA, le site peut bloquer l'accès : utiliser « Coller un lien ou des paroles ».
- L'application lit le formulaire de recherche du site au moment de la recherche, puis la page de l'auteur, puis l'adresse directe auteur/titre. Si le site change sa structure, il suffit d'adapter `tsParse` et `tsLinks` dans `www/index.html`.
- Vérifier que les chants importés sont libres de droit ou utilisés avec autorisation. Importer par petits lots : une pause de 0,5 s est faite entre deux téléchargements.

## Nouveautés 5.5 : gestion des MP3
- Les chants indiquent si un MP3 est associé et la bibliothèque peut filtrer les chants avec ou sans audio.
- La fiche d’un chant permet d’écouter l’audio, de choisir un nouveau fichier pour le remplacer, ou de programmer son retrait avant d’enregistrer.
- Les sauvegardes manuelles peuvent inclure les MP3 comme auparavant. Une option permet aussi de les inclure dans les sauvegardes automatiques hebdomadaires de l’APK ; elle est désactivée par défaut car les fichiers audio peuvent rendre la sauvegarde volumineuse.
- Les informations disponibles sur le nom et la taille du fichier sont conservées avec le chant.

## Nouveautés 5.4 : design
- Polices intégrées hors ligne (Inter pour l'interface, Lora pour les titres et paroles) : rendu identique sur tous les téléphones.
- Lecteur compact : lecture/pause, barre de progression, temps, vitesse (0,75× à 1,25×), boucle, outils (A–B, aller à, égaliseur, synchro).
- Carte d'un chant dans un culte sur une seule ligne, actions dans le menu « ⋯ » avec libellés.
- Listes déroulantes, cases à cocher et curseurs personnalisés, fondu sous la barre du bas, états vides avec bouton, premier lancement guidé, mise en page deux colonnes sur grand écran.


## Version 5.6
- Cultes : recherche par date, nom, thème ou chant et tri par date récente/ancienne, nom ou nombre de chants.
- Google Drive : depuis Réglages > Sauvegardes, le bouton de partage Android permet de choisir Google Drive pour déposer le JSON. La restauration accepte un JSON sélectionné dans le fournisseur de fichiers Google Drive. Les fichiers audio restent inclus uniquement si la case correspondante est cochée.
- Ce transfert Drive est volontaire et manuel. Il n’y a pas de synchronisation automatique bidirectionnelle : celle-ci exige un client OAuth Google configuré et un backend de synchronisation déployé. Aucun secret ni jeton Google n’est intégré à l’application.
- Workflow GitHub : copier `github-workflow/android.yml` dans `.github/workflows/android.yml` du dépôt.
- Interface : aucun nouvel ensemble d’icônes ; les listes de cultes utilisent des lignes textuelles plus sobres.

## Version 5.7

- Le lecteur Android lit les pistes choisies dans un dossier via le sélecteur de fichiers système. Les fichiers MP3 restent dans leur emplacement; WorshipFlow conserve uniquement leur URI et leurs métadonnées. Si l’accès au dossier est révoqué ou si l’application est réinstallée, choisissez à nouveau le dossier.
- La lecture dispose d’un retour/avance de 15 secondes et ne passe plus automatiquement au chant suivant. L’égaliseur WebAudio ne s’attache qu’après activation d’un réglage non plat afin de limiter la charge pendant la lecture standard.
- Les cultes ont un minuteur réglé par défaut à 30 minutes, une durée audio cumulée et un filtre de chants par thème.
- Les déclarations de foi figurent dans la bibliothèque avec les inspirations et les versets.


## Version 5.7.1

- Le bouton de répétition ouvre maintenant le choix entre répéter le chant, répéter le culte, s’arrêter à la fin du chant ou enchaîner automatiquement sur le chant suivant.
- Le minuteur du culte est présenté dans un bandeau compact en haut de la fiche.


## Version 5.7.2

- Le minuteur reste visible et pilotable pendant toute la lecture d’un culte.
- Les paroles importées sont nettoyées des caractères invisibles, balises et éléments de navigation du site.
- La liste de chants peut être réduite ou développée en une action.
- Les filtres audio affichent leurs libellés dans la langue choisie.


## Version 5.7.3

- Correction de la constante du drapeau Android empêchant la compilation Java du plugin audio.


## Version 5.7.4

- Nettoyage des caractères parasites injectés dans les paroles Tononkira, y compris pour les chants déjà enregistrés.
- Fenêtre des modes de lecture recentrée, compacte et défilante, avec fermeture visible en haut.


## Version 5.7.5

- Recherche Tononkira : sélection de plusieurs résultats avec cases à cocher et commandes Tout sélectionner / Tout désélectionner, puis récupération groupée dans une seule prévisualisation.

## Version 5.7.6

- Le minuteur est intégré au défilement du lecteur ; les paroles conservent un espace au-dessus des commandes fixes.
- Le panneau des modes de lecture est centré et garde son bouton de fermeture dans l’en-tête.

## Version 5.7.7
- Le bouton de boucle passe au mode de lecture suivant à chaque appui : répéter le chant, répéter le culte, arrêt à la fin, puis chant suivant automatique. Un message indique le mode actif; le cycle recommence ensuite au premier mode.

## Version 5.7.8
- Ajout de l’accès au dossier audio du téléphone dans Android et masquage de l’import MP3 dans l’application. La version Web autonome garde son import de fichiers. La séparation complète entre audio du téléphone et liste de chants est corrigée en 5.7.9.


## Version 5.7.9
- Les fichiers audio du téléphone s’affichent dans un navigateur audio séparé et sont lus depuis leur URI d’origine. Aucun enregistrement de chant ni copie audio n’est créé. Une action confirmée permet de retirer les anciennes entrées audio seules sans supprimer les fichiers du téléphone. L’import groupé Tononkira reste consacré aux paroles.
