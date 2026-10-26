# Backend de location de logements

Application Spring Boot 3.4.3, Java 17 et MongoDB. Le code métier reste dans un monolithe en couches. Voir [l'audit d'architecture](docs/ARCHITECTURE.md), le [contrat HTTP](docs/API.md) et la [reprise d'une base existante](docs/MIGRATION.md).

## Démarrage local (PowerShell)

Prérequis : JDK 17+, Maven et Docker Desktop démarré. Le port 27018 permet de conserver une éventuelle instance MongoDB locale sur 27017.

```powershell
docker compose up -d --wait
$secretBytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($secretBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)
$env:MONGODB_URI = 'mongodb://localhost:27018/airbnb_clone?replicaSet=rs0&directConnection=true'
$env:PAYMENT_MODE = 'demo'
mvn spring-boot:run
```

API : `http://localhost:8082`. CORS : `http://localhost:4200`, configurable via `CORS_ALLOWED_ORIGINS` (origines séparées par des virgules). Les autres variables sont décrites dans [.env.example](.env.example). Spring Boot ne charge pas automatiquement un fichier `.env`.

Le paiement `demo` ne débite aucun argent. Sans configuration explicite, les paiements sont désactivés et renvoient HTTP 503. Une passerelle réelle nécessite une intégration distincte avec idempotence côté prestataire et notifications signées.

La base doit être un replica set pour les transactions. Le Compose fourni démarre un replica set de développement à un membre ; ce déploiement local n'est pas une configuration de production. Les données restent dans un volume Docker lorsque les conteneurs sont arrêtés.

Si une base contient déjà des données de l'ancienne version, suivre [MIGRATION.md](docs/MIGRATION.md) avant de démarrer le backend modifié.

## Vérification

```powershell
mvn test
mvn -Pintegration verify
```

La première commande exécute les tests unitaires sans MongoDB. La seconde démarre un MongoDB de test dans Docker via Testcontainers, vérifie les parcours HTTP et les transactions, puis supprime les conteneurs de test. Elle n'utilise pas la base de développement. Les rapports se trouvent dans `target/surefire-reports` et `target/failsafe-reports`.

## Parcours principal

1. S'inscrire puis se connecter pour obtenir un JWT.
2. Appeler `POST /api/users/me/host` pour devenir hôte. Chaque hôte conserve son rôle voyageur.
3. Publier un logement avec un type parmi `ENTIRE_HOME`, `PRIVATE_ROOM`, `SHARED_ROOM`, `HOTEL_ROOM`.
4. Rechercher un logement et réserver des nuits avec une date de départ exclusive.
5. Payer en mode démonstration pour confirmer la réservation. Une réservation impayée expire après 30 minutes ; un traitement la libère au plus tard au passage suivant, toutes les minutes.
6. Avant l'arrivée, annuler une réservation impayée ou rembourser une réservation payée.
7. Après le départ, l'hôte clôture le séjour ; le voyageur peut alors déposer un avis unique.

L'inscription crée uniquement un voyageur. Le premier administrateur doit être désigné par un opérateur dans MongoDB ; aucun compte administrateur ni mot de passe fixe n'est créé automatiquement.
