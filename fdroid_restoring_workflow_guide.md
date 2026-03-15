# 🚀 Guide de Restauration et Configuration de F-Droid

Il semble que le flux de travail (`workflow`) pour la mise à jour automatique du magasin F-Droid (`Dynag1-Fdroid`) ait été **tronqué ou supprimé** lors des dernières manipulations hier. De plus, il manquait la clé de signature pour l'index du dépôt.

J'ai vérifié la clé Base64 que tu as laissée dans `keystore_base64.txt`. Elle correspond bien à tes clés de signature APK (`scrybook-key`). 

Voici la marche à suivre pour rétablir et faire fonctionner le déploiement sur F-Droid.

---

## 1. 🔑 Ajouter le Secret dans GitHub (`Dynag1-Fdroid`)

Pour que le dépôt F-Droid puisse signer son index (ce qui est obligatoire), ajoute le secret suivant :

1. Va sur [https://github.com/Dynag1/Dynag1-Fdroid](https://github.com/Dynag1/Dynag1-Fdroid)
2. Va dans **Settings** > **Secrets and variables** > **Actions**
3. Clique sur **New repository secret**
4. **Name** : `REPO_KEYSTORE`
5. **Secret** : *(Copie-colle TOUT le contenu du fichier `keystore_base64.txt`)*

---

## 2. 🛠️ Restaurer le workflow `build-store.yml`

Le fichier `.github/workflows/build-store.yml` dans ton dépôt `Dynag1-Fdroid` est actuellement vide/tronqué. 

Voici le fichier **complet et corrigé** que tu dois placer à cet endroit. J'ai déjà intégré tes informations de mot de passe et de clé pour qu'elle s'applique automatiquement.

### Contenu à coller dans `.github/workflows/build-store.yml` :

```yaml
name: Build Central F-Droid Store Pages

on:
  push:
    branches:
      - main
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

jobs:
  build_repo:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Setup F-Droid Tools
        run: |
          sudo apt-get update
          sudo apt-get install -y fdroidserver

      - name: Prepare F-Droid Repo
        env:
          KEYSTORE: ${{ secrets.REPO_KEYSTORE }}
        run: |
          mkdir -p fdroid/repo
          mkdir -p public/fdroid
          
          # Copie des APKs et métadonnées poussés par vos différentes applications
          cp -r apks/*/* fdroid/repo/ 2>/dev/null || true
          cp -r metadata/* fdroid/repo/ 2>/dev/null || true
          
          cd fdroid
          
          # Initialisation uniquement si config.yml n'existe pas
          if [ ! -f config.yml ]; then
            fdroid init -d repo
          fi
          
          # Configuration de la clé de signature si elle est présente
          if [ -n "$KEYSTORE" ]; then
            echo "$KEYSTORE" | base64 -d > keystore.p12
            # Mise à jour de config.yml avec les informations de ta clé
            echo "repo_keyalias: scrybook-key" >> config.yml
            echo "keystorepass: G22rtp12" >> config.yml
            echo "keypass: G22rtp12" >> config.yml
          fi
          
          # Génération de l'index
          fdroid update -c
          
          # Copie vers le répertoire de déploiement des Pages
          cp -r repo ../public/fdroid/
          
          # 📜 Création d'une page d'accueil (index.html) pour éviter les erreurs 404
          cd ..
          cat <<EOF > public/index.html
          <!DOCTYPE html>
          <html lang="fr">
          <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Dynag F-Droid Store</title>
              <style>
                  body { font-family: sans-serif; text-align: center; padding: 50px; background: #121212; color: #e0e0e0; }
                  h1 { color: #bb86fc; }
                  a { color: #03dac6; text-decoration: none; font-weight: bold; }
                  .url-box { background: #1e1e1e; padding: 15px; border-radius: 8px; display: inline-block; margin: 20px 0; border: 1px solid #333; }
              </style>
          </head>
          <body>
              <h1>🛒 Dynag F-Droid Repository</h1>
              <p>Ceci est le dépôt F-Droid officiel pour Dynag.</p>
              <p>Pour l'utiliser dans ton client F-Droid, ajoute l'adresse suivante :</p>
              <div class="url-box">
                  <code>https://dynag1.github.io/Dynag1-Fdroid/fdroid/repo</code>
              </div>
              <p><a href="https://dynag1.github.io/Dynag1-Fdroid/fdroid/repo/index.xml">Voir l'index (XML)</a></p>
          </body>
          </html>
          EOF

      - name: Setup Pages
        uses: actions/configure-pages@v4

      - name: Upload Pages Artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: './public'

  deploy:
    needs: build_repo
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

---

## 3. 🧪 Comment cela va fonctionner :

1. Dès que tu auras poussé ce fichier dans `Dynag1-Fdroid`, GitHub Actions va s'exécuter.
2. Il va récupérer ton `.p12`, configurer `config.yml` à la volée, et signer l'index F-Droid avec la **même clé** que tes APKs.
3. Les GitHub Pages se déploieront ensuite correctement sans erreur d'index non signé ou de 404 sur des configurations vides.

---

## 4. 📲 Ajouter Valoria au même magasin F-Droid

Pour que **Valoria** envoie ses APKs dans le même magasin `Dynag1-Fdroid`, tu dois créer un flux de travail (`workflow`) similaire dans le dépôt de Valoria (par exemple `.github/workflows/push-to-store.yml`).

### Workflow pour le dépôt de Valoria :

Il te suffit d'utiliser cette structure. Remplace simplement `VOTRE_PACKAGE_ID_VALORIA` par l'identifiant de l'application Valoria (ex: `co.dynag.valoria`).

```yaml
name: Push APK to Central F-Droid Store

on:
  push:
    branches:
      - main
    tags:
      - 'v*'
  workflow_dispatch:

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Decode Keystore
        shell: python
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE }}
        run: |
          import base64
          import os
          keystore_b64 = os.environ.get('KEYSTORE_BASE64', '')
          if keystore_b64:
              with open('release.keystore', 'wb') as f:
                  f.write(base64.b64decode(keystore_b64.strip()))
              print("✅ Keystore decoded successfully")
          else:
              print("❌ KEYSTORE_BASE64 secret is empty")
              exit(1)

      - name: Build Release APK
        run: ./gradlew assembleRelease

      - name: Checkout Central F-Droid Store
        uses: actions/checkout@v4
        with:
          repository: Dynag1/Dynag1-Fdroid
          token: ${{ secrets.STORE_PAT }}
          path: store-repo

      - name: Push APK and Metadata to Store
        run: |
          # ⚠️ REMPLACEZ co.dynag.valoria par le vrai Package ID (ex: co.dynag.valoria)
          PKG_ID="co.dynag.valoria"
          
          mkdir -p store-repo/apks/\$PKG_ID
          mkdir -p store-repo/metadata/\$PKG_ID/en-US
          mkdir -p store-repo/metadata/\$PKG_ID/fr-FR
          
          # Copie des APK (en le renommant pour éviter les écrasements dans le dossier plat de F-Droid)
          cp app/build/outputs/apk/release/*.apk store-repo/apks/\$PKG_ID/valoria-release.apk
          
          # Copie des métadonnées Fastlane (si disponibles)
          cp -r fastlane/metadata/android/fr-FR/* store-repo/metadata/\$PKG_ID/fr-FR/ 2>/dev/null || true
          cp -r fastlane/metadata/android/en-US/* store-repo/metadata/\$PKG_ID/en-US/ 2>/dev/null || true
          
          # Push sur le magasin central
          cd store-repo
          git config user.name "App Github Action"
          git config user.email "actions@github.com"
          git add apks/ metadata/
          git commit -m "Mise à jour automatique de l'APK Valoria" || echo "Rien à commiter"
          git push
```

### ⚠️ IMPORTANT :
N'oublie pas d'ajouter les secrets **`STORE_PAT`** (ton Token GitHub) ainsi que **`KEYSTORE`** dans le dépôt de **Valoria**, de la même manière que pour ScryBook !
[diff_block_end]
