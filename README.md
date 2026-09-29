# BHENX FINDER

Application Android permettant de retrouver un téléphone à proximité grâce au Bluetooth / BLE (Bluetooth Low Energy).

---

## Fonctionnement

BHENX FINDER fonctionne en communication directe de proximité entre deux téléphones Android équipés de l'application :

### Téléphone A (Chercheur)
- Recherche les téléphones BHENX FINDER à proximité via BLE.
- Sélectionne la cible dans la liste des appareils détectés.
- Suit l'évolution du signal Bluetooth (RSSI) en temps réel avec un radar de proximité dynamique.
- Peut envoyer une commande sécurisée pour faire sonner le téléphone cible à distance, puis arrêter la sonnerie une fois retrouvé.

### Téléphone B (Cible / À retrouver)
- Active le mode **« Téléphone à retrouver »**.
- Devient détectable localement par les autres installations BHENX FINDER via une balise BLE non intrusive.
- Reçoit la commande sécurisée et déclenche une alarme sonore et vibratoire immédiate avec un écran d'alerte très visible.
- Permet d'arrêter la sonnerie d'un simple toucher sur son écran ou à distance depuis le Téléphone A.

---

## Fonctionnalités

- **Bluetooth / BLE réel :** Aucun faux signal, aucune fausse distance ni simulation. Toutes les données proviennent du matériel physique.
- **Recherche de téléphones BHENX :** Détection prioritaire avec nom personnalisable et identifiant local aléatoire (`BHX-...`).
- **Suivi RSSI temps réel :** Filtrage et lissage du signal Bluetooth reçu.
- **Indicateur de proximité dynamique :** Animation radar et pulsation dont la vitesse s'adapte instantanément à la proximité réelle (Très proche, Proche, Moyen, Éloigné, Signal perdu).
- **Communication GATT sécurisée :** Échange de commandes `RING` et `STOP` par service et caractéristiques GATT dédiés avec jeton de session.
- **Fonction Faire sonner :** Déclenchement d'une sonnerie sur le flux d'alarme Android (`RingtoneManager`) et vibrations répétées pour retrouver l'appareil même sous un coussin ou dans une autre pièce.
- **Arrêt de la sonnerie :** Arrêt manuel depuis le Téléphone A, arrêt direct sur le Téléphone B, ou arrêt automatique de sécurité après 30 secondes pour préserver la batterie.
- **Fonctionnement 100 % local :** Aucun compte utilisateur, aucun serveur externe, aucun cloud.
- **Historique local :** Sauvegarde Room SQLite des sessions de recherche réussies, effaçable à tout moment.
- **Paramètres :** Personnalisation du nom d'appareil, thème (Sombre, Clair, Système), sélection de langue (Français, English, Kreyòl), test audio.
- **Prise en charge du mode arrière-plan :** Service de premier plan (`Foreground Service`) avec notification d'état persistante pour maintenir la détectabilité selon les capacités du terminal.

---

## Confidentialité

- **BHENX FINDER ne nécessite aucun compte utilisateur.**
- **Aucune donnée personnelle n'est collectée, stockée sur un serveur ou envoyée sur Internet.**
- Les identifiants utilisés pour la découverte locale sont générés de manière aléatoire sur l'appareil et ne contiennent ni numéro de téléphone, ni IMEI, ni adresse email, ni identifiant publicitaire.

---

## Installation

### Installation de l'APK de test (Debug)

1. Transférer le fichier `app-debug.apk` généré sur les téléphones Android (par câble USB, partage direct ou téléchargement).
2. Ouvrir le fichier `.apk` sur chaque téléphone.
3. Si Android affiche une alerte de sécurité, autoriser l'installation depuis cette source (*Paramètres > Applications > Autoriser l'installation d'applications inconnues*).
4. Terminer l'installation et ouvrir **BHENX FINDER**.

---

## Test avec deux téléphones

Pour tester la recherche en conditions réelles :

1. **Installer BHENX FINDER** sur les deux téléphones Android (Téléphone A et Téléphone B).
2. **Activer le Bluetooth** et la localisation (si demandée par la version d'Android pour le scan BLE) sur les deux téléphones.
3. **Sur le Téléphone B :** appuyer sur **« TÉLÉPHONE À RETROUVER »** (ou via les Paramètres) et activer l'interrupteur. Le téléphone affiche *« ACTIVÉ : Disponible pour être retrouvé »*.
4. **Sur le Téléphone A :** ouvrir l'application et appuyer sur **« CHERCHER MON TÉLÉPHONE »**, vérifier les conditions, puis lancer la recherche.
5. **Sélectionner le Téléphone B** dans la liste des appareils détectés (il apparaît en tête de liste avec son nom et son badge BHENX).
6. **Se déplacer** en observant l'évolution du radar et de la jauge de signal (le radar clignote plus rapidement au fur et à mesure que vous vous rapprochez).
7. Appuyer sur **« 🔊 FAIRE SONNER MON TÉLÉPHONE »** : le Téléphone B sonne immédiatement et affiche l'alerte plein écran **« BHENX FINDER TE CHERCHE ! »**. Appuyer sur **« ARRÊTER LA SONNERIE »** pour couper l'alarme.

---

## Limitations

- **Matériel BLE Advertising :** La fonction « Téléphone à retrouver » nécessite que la puce Bluetooth du téléphone prenne en charge le mode émetteur périphérique BLE (`BluetoothLeAdvertiser`). Si un téléphone très ancien ne supporte pas ce mode matériel, l'application prévient l'utilisateur et ce téléphone peut uniquement servir de chercheur (Téléphone A).
- **Restrictions constructeurs et économie d'énergie :** Sur certaines surcouches Android (ex. Xiaomi MIUI, Huawei EMUI, Samsung One UI), la mise en veille agressive de l'OS peut couper les signaux BLE lorsque l'écran est verrouillé. Il est recommandé de désactiver l'optimisation de batterie pour BHENX FINDER sur le téléphone cible afin de garantir une détection optimale en arrière-plan.
- **Portée Bluetooth :** La détection et la commande sont effectives dans le rayon de portée standard du Bluetooth physique (généralement 10 à 30 mètres selon les obstacles et les murs).

---

## Architecture technique

- **Langage :** Kotlin 100%
- **Interface :** Jetpack Compose (Material Design 3 épuré)
- **Persistance locale :** Room Database (SQLite) + SharedPreferences
- **Gestion Bluetooth :** Android BluetoothLeScanner, BluetoothLeAdvertiser, BluetoothGattServer, BluetoothGattCallback
- **Audio & Haptique :** RingtoneManager (flux `USAGE_ALARM`), VibratorManager / Vibrator
