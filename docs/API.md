# Contrat HTTP pour le frontend

Base : `http://localhost:8082/api`. Corps JSON ; dates de séjour `YYYY-MM-DD`. Les montants sont des nombres JSON, en unité monétaire principale ; cette démonstration utilise une seule devise implicite. Les valeurs d'enum sont en majuscules.

Les routes privées demandent `Authorization: Bearer <token>`. L'identité des opérations est celle du JWT. Les champs historiques `userId` dans la création de réservation, `authorId` dans la création d'avis et `senderId` dans l'envoi de message sont facultatifs et ignorés.

## Authentification et profil

| Méthode | Route | Accès / résultat |
| --- | --- | --- |
| POST | /auth/register | Public ; username, email, password, confirmPassword ; 201 |
| POST | /auth/login | Public ; email et password ; 200 avec data.token, data.tokenType et data.expiresIn en millisecondes |
| POST | /auth/logout | Connecté ; effacer le JWT côté client |
| GET | /users/me | Profil connecté sans mot de passe |
| POST | /users/me/host | Ajoute ROLE_OWNER en conservant ROLE_USER |

L'inscription n'émet pas de token ; appeler login ensuite. Les autorités sont relues en base à chaque requête, donc le rôle hôte devient effectif sans attendre l'expiration du JWT.

## Catalogue et calendrier

| Méthode | Route | Accès / résultat |
| --- | --- | --- |
| GET | /properties | Public ; logements AVAILABLE |
| GET | /properties/{id} | Public ; 404 si non publié |
| GET | /properties/owner/me | Hôte ou administrateur ; ses annonces, tous statuts |
| GET | /properties/owner/{ownerId} | Utilisateur concerné ou administrateur |
| POST | /properties | Hôte ou administrateur ; 201 |
| PUT | /properties/{id} | Hôte du logement ou administrateur |
| DELETE | /properties/{id} | Hôte du logement ou administrateur ; suppression logique ; 204 |
| GET | /amenities | Public |
| POST | /amenities | Administrateur ; name, icon et category |
| GET | /search/properties | Public ; city, startDate, endDate, guests ; minPrice et maxPrice facultatifs |
| GET | /availabilities/{propertyId} | Hôte du logement ou administrateur ; startDate, endDate ; exceptions du calendrier |
| PUT | /availabilities | Hôte du logement ou administrateur ; propertyId, date, isAvailable et priceOverride |

Création et modification de logement : title, description, type, address, city, country, latitude, longitude, pricePerNight, maxGuests, bedrooms, amenityIds, imageUrls. title, type, address, city, country, pricePerNight, maxGuests et bedrooms sont obligatoires. Types : ENTIRE_HOME, PRIVATE_ROOM, SHARED_ROOM, HOTEL_ROOM. L'identifiant, le propriétaire et le statut sont contrôlés par le serveur.

Dans le calendrier, une valeur priceOverride absente ou null rétablit le prix de base ; isAvailable absent conserve l'état existant. La lecture renvoie les exceptions et non un jour matérialisé pour chaque date. Les réservations de l'hôte fournissent les nuits occupées. Les nuits déjà réservées ne peuvent pas être bloquées par une exception.

## Réservations, paiement et avis

| Méthode | Route | Accès / résultat |
| --- | --- | --- |
| POST | /bookings | Connecté ; propertyId, startDate, endDate, numberOfGuests ; 201 |
| GET | /bookings/user/me | Réservations du voyageur connecté |
| GET | /bookings/user/{userId} | Utilisateur concerné ou administrateur |
| GET | /bookings/property/{propertyId} | Hôte du logement ou administrateur |
| GET | /bookings/{id} | Voyageur, hôte du logement ou administrateur |
| PUT | /bookings/{id}/cancel | Voyageur ; réservation impayée et future ; 204 |
| PUT | /bookings/{id}/complete | Hôte du logement ou administrateur ; séjour CONFIRMED dont le départ est passé |
| POST | /payments/process/{bookingId}?method=CREDIT_CARD | Voyageur ; paiement demo et confirmation atomiques |
| GET | /payments/booking/{bookingId} | Voyageur, hôte du logement ou administrateur |
| POST | /payments/{paymentId}/refund | Voyageur, hôte du logement ou administrateur ; séjour futur ; annule aussi la réservation |
| POST | /reviews | Voyageur ; bookingId, rating de 1 à 5 et comment ; séjour COMPLETED ; 201 |
| GET | /reviews/can-review?bookingId=... | Connecté ; booléen |
| GET | /reviews/property/{propertyId} | Public ; avis avec nom d'auteur |
| DELETE | /reviews/{reviewId} | Auteur ; 204 |

Méthodes de paiement acceptées par la simulation : CREDIT_CARD, PAYPAL, STRIPE, BANK_TRANSFER. Ces libellés hérités n'impliquent aucune connexion à un prestataire. La route historique de confirmation manuelle est supprimée : le paiement confirme la réservation.

## Messagerie privée

| Méthode | Route | Paramètres |
| --- | --- | --- |
| GET | /conversations/user/{userId} | Seulement l'utilisateur connecté |
| POST | /conversations?user1Id=...&user2Id=... | user1Id doit être l'utilisateur connecté ; bookingId facultatif et vérifié |
| GET | /conversations/{id}/messages | Identité du JWT ; messages visibles pour cet utilisateur |
| POST | /conversations/{id}/messages | receiverId, content ; 5000 caractères maximum |
| PUT | /conversations/{id}/read | Marque les messages reçus comme lus |
| DELETE | /conversations/messages/{messageId} | Masque le message uniquement pour l'utilisateur connecté ; 204 |
| PUT | /conversations/{id}/archive | Archive uniquement pour cet utilisateur |
| PUT | /conversations/{id}/unarchive | Annule son archivage |

Les anciens paramètres userId de lecture et de modification ne déterminent plus les droits. Un nouveau message réactive le fil pour les deux participants.

## Administration et erreurs

Routes administrateur : GET /admin/users, PUT /admin/users/{userId}/role?role=ROLE_OWNER, DELETE /admin/users/{userId}, PUT /admin/properties/{propertyId}/status?status=MAINTENANCE, GET /admin/stats. La suppression désactive le compte, retire ses annonces du catalogue public et conserve les références métier. Les rôles autorisés sont ROLE_USER, ROLE_OWNER, ROLE_ADMIN et ROLE_MODERATOR.

Les erreurs de validation et métier contiennent status, message, timestamp et path. Codes principaux : 400 requête invalide, 401 authentification requise ou invalide, 403 accès interdit, 404 ressource absente, 409 conflit métier ou concurrent, 503 paiement désactivé. Le frontend doit afficher le message et actualiser les disponibilités après un 409.
