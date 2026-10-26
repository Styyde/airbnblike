# Airbnblike

Une application de location de logements pour découvrir un hébergement, préparer un séjour et accueillir des voyageurs.

## Fonctionnalités

- Catalogue de logements et recherche par destination, dates, capacité et budget.
- Comptes voyageurs et hôtes avec authentification JWT.
- Publication des annonces, équipements, calendrier et prix par nuit.
- Réservations, paiement de démonstration, annulation et remboursement.
- Messagerie privée entre voyageurs et hôtes et avis après un séjour terminé.

## Technologies

Java 17 · Spring Boot 3.4 · Spring Security · MongoDB · Maven · Docker

## Démarrage du backend

Prérequis : JDK 17+, Maven et Docker Desktop démarré. Depuis la racine du dépôt, dans PowerShell :

```powershell
docker compose up -d --wait
$secretBytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($secretBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)
$env:MONGODB_URI = 'mongodb://localhost:27018/airbnb_clone?replicaSet=rs0&directConnection=true'
$env:PAYMENT_MODE = 'demo'
mvn spring-boot:run
```

L’API est accessible sur `http://localhost:8082`. MongoDB utilise un replica set fourni par Docker Compose, sur le port local `27018`. Les données sont conservées dans un volume Docker.

## Configuration

| Variable | Description | Valeur par défaut |
| --- | --- | --- |
| JWT_SECRET | Secret aléatoire d’au moins 64 octets | Obligatoire |
| MONGODB_URI | Connexion au replica set MongoDB | Instance locale sur 27017 |
| PORT | Port de l’API | 8082 |
| JWT_EXPIRATION_MS | Durée de validité du JWT | 86400000 |
| CORS_ALLOWED_ORIGINS | Origines web autorisées | http://localhost:4200 |
| PAYMENT_MODE | demo pour activer la simulation | disabled |

Le fichier [.env.example](.env.example) décrit la configuration sans identifiants privés. Spring Boot lit les variables du terminal et ne charge pas automatiquement un fichier `.env`.

## Paiement de démonstration

Le mode `demo` simule un paiement : aucun argent n’est débité et aucune carte bancaire réelle n’est nécessaire. Les paiements sont désactivés tant que ce mode n’est pas explicitement activé.

## Tests

```powershell
mvn test
mvn -Pintegration verify
```

Les tests d’intégration utilisent MongoDB dans un conteneur Docker jetable, distinct de la base de développement.

## Documentation

Le [contrat API](docs/API.md) présente les routes, les paramètres et les règles d’accès.
