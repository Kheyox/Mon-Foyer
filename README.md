# Mon Foyer

Application Android native pour organiser le quotidien d'un foyer : courses, budget, factures, argent restant, agendas, notes et membres.

## Stack

- Kotlin
- Jetpack Compose / Material 3
- Firebase Authentication avec Google
- Cloud Firestore
- Build Android App Bundle (`.aab`) pour publication Play Store

## Configuration Firebase

1. Dans Firebase, ouvre le projet `mon-foyer-65616`.
2. Ajoute une application Android avec le package `com.bibliostudio.monfoyer`.
3. Telecharge `google-services.json`.
4. Place le fichier dans `app/google-services.json`.
5. Active Authentication > Sign-in method > Google.
6. Active Firestore Database.
7. Remplace dans `app/src/main/res/values/strings.xml` la valeur `REMPLACE_PAR_LE_CLIENT_WEB_FIREBASE` par l'ID client Web OAuth visible dans Firebase/Google Cloud.

## Build AAB

Dans Android Studio : `Build > Generate Signed Bundle / APK > Android App Bundle`.

En ligne de commande, apres installation du SDK Android et de Gradle :

```powershell
gradle bundleRelease
```

Le fichier sera genere dans `app/build/outputs/bundle/release/`.

## GitHub Actions

Le workflow `.github/workflows/android-aab.yml` construit un `.aab` a chaque push sur `main`.

Secrets GitHub a ajouter dans `Settings > Secrets and variables > Actions` :

- `GOOGLE_SERVICES_JSON_BASE64` : contenu base64 du fichier `app/google-services.json`
- `ANDROID_KEYSTORE_BASE64` : keystore d'upload Play Store en base64
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## Regles Firestore

Les regles de depart sont dans `firestore.rules`.

Deploiement :

```powershell
firebase deploy --only firestore:rules
```
