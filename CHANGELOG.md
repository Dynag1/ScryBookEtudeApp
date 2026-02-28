# Changelog

Toutes les modifications notables apportées au projet **ScryBook** seront documentées dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
et ce projet adhère au [Versionnage Sémantique](https://semver.org/spec/v2.0.0.html).

## [0.1.8] - 2026-02-28

### Fix
- **Correction des Crashs** : Résolution d'un plantage lors de l'ouverture de projet et d'éventuels conflits de base de données.
- **Sauvegarde Renforcée** : Amélioration de la persistance des fichiers lors de l'utilisation d'explorateurs de fichiers tiers.

## [0.1.6] - 2026-02-28

### Ajouté
- **Modification Directe (Edit in Place)** : L'application synchronise désormais automatiquement les modifications vers le fichier d'origine, même s'il est situé dans un dossier protégé (Documents, Google Drive, etc.).
- **Accès Complet aux Fichiers** : Ajout du support de l'autorisation "Accès à tous les fichiers" (Android 11+) pour un meilleur contrôle sur vos dossiers de projets.
- **Diagnostic de Déploiement** : Amélioration des logs GitHub Actions pour faciliter le débogage de la configuration Google Play.

## [0.1.5] - 2026-02-28

### Ajouté
- **Bouton Sauvegarde Manuelle** : Ajout d'une icône de sauvegarde dans la barre de titre pour forcer l'enregistrement immédiat.
- **Support des caractères spéciaux** : Amélioration de la gestion des caractères dans le JSON de configuration du Play Store.

## [0.1.4] - 2026-02-28

### Ajouté
- **Gestionnaire de fichiers amélioré** : L'application travaille désormais sur le fichier d'origine (via résolution de chemin réel) au lieu d'utiliser des copies cache temporaires.
- **Support des fichiers externes** : Amélioration de la persistance pour les fichiers ouverts depuis Google Drive, Nextcloud, etc. via copie dans le dossier permanent `/ScryBook`.
- **Navigateur de dossiers** : Ajout d'une option "Parcourir" dans l'écran de création de projet pour choisir visuellement le dossier de destination.
- **Documentation visuelle** : Intégration de captures d'écran réelles dans le README pour un meilleur aperçu de l'interface.

### Corrigé
- **Déploiement GitHub** : Correction de l'erreur JSON dans le pipeline de déploiement Play Store.

## [0.1.3] - 2026-02-28

### Ajouté
- **Sauvegarde automatique brute** : L'éditeur enregistre maintenant toutes les 30 secondes.
- **Sauvegarde intelligente** : Enregistre automatiquement lors du changement de chapitre via le menu latéral.
- **Sauvegarde de cycle de vie** : Sauvegarde automatique quand l'application est mise en arrière-plan ou fermée.
- **Gestion des permissions** : Demande explicite des permissions de stockage pour les versions d'Android < 13.
- **Dialogue d'explication** : Ajout d'une popup expliquant pourquoi la permission de stockage est nécessaire.
- **Localisation** : Traduction des messages système en Français et Anglais.

### Corrigé
- **Lien SDK** : Correction du chemin incorrect vers le SDK Android dans les propriétés locales.
- **Stabilité** : Utilisation de verrous (Mutex) pour éviter les corruptions de fichiers lors de sauvegardes simultanées.
- **Robustesse** : Les sauvegardes se terminent même si l'interface est détruite (contexte non-annulable).

## [0.1.2] - 2026-02-27

### Ajouté
- **Fichiers récents** : Liste des 4 derniers projets ouverts sur l'écran d'accueil.
- **Automatisation** : Mise en place des GitHub Actions pour le déploiement automatique sur le Google Play Store.

## [0.1.1] - 2026-02-26

### Ajouté
- **Mode Paysage** : Support complet de l'affichage permanent du menu et du résumé sur tablettes et écrans larges.
- **Insertion d'images** : Amélioration du redimensionnement et de l'alignement des images insérées dans le texte.

## [0.1.0] - 2026-02-24

### Ajouté
- **Éditeur WYSIWYG** : Premier éditeur riche basé sur WebView avec barre de formatage.
- **Gestion de projet** : Création, ouverture et suppression de projets au format `.sb` (SQLite).
- **Structure de livre** : Gestion des chapitres, personnages et lieux.
- **Export PDF** : Première version de l'exportation du manuscrit au format PDF.

---
*Dernière mise à jour : 28 février 2026*
