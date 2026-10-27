# API Airbnblike

Base locale : `http://localhost:8082/api`. Corps JSON, dates de séjour `YYYY-MM-DD`, échéances ISO 8601 avec fuseau UTC. Les montants sont exprimés en MAD. Les nuits appartiennent à l’intervalle [arrivée, départ).

Les routes privées nécessitent `Authorization: Bearer <token>`. Le serveur détermine l’identité depuis le JWT.

## Comptes

| Méthode | Route | Corps ou résultat |
| --- | --- | --- |
| POST | /auth/register | username, email, password, confirmPassword ; 201 |
| POST | /auth/login | email, password ; data contient token, expiresIn en millisecondes et le profil |
| POST | /auth/logout | Déconnexion côté client : effacer la session locale |
| GET | /users/me | id, username, email, roles, active |
| POST | /users/me/host | Ajoute le rôle hôte au compte connecté |

L’inscription ne crée pas de token ; le client se connecte ensuite. Les droits sont relus en base à chaque requête.

## Catalogue

| Méthode | Route | Description |
| --- | --- | --- |
| GET | /properties | Logements publiés |
| GET | /properties/{id} | Détail public ; 404 si le logement n’est pas publié |
| GET | /search/properties | Filtres facultatifs : city, startDate, endDate, guests, type, minPrice, maxPrice |
| GET | /properties/{id}/quote | startDate, endDate, guests ; prix exact du séjour et disponibilités |
| GET | /properties/{id}/calendar | startDate, endDate ; nuits, prix et disponibilités sans données de voyageurs |
| GET | /properties/owner/me | Toutes les annonces de l’hôte connecté |
| GET | /properties/owner/{ownerId} | Propriétaire concerné ou administrateur |
| POST | /properties | Publication par un hôte ; 201 |
| PUT | /properties/{id} | Modification par son hôte |
| DELETE | /properties/{id} | Retrait du catalogue ; 204 |
| GET | /amenities | Catalogue public des équipements |
| POST | /amenities | Création d’un équipement par un administrateur |

Une annonce contient title, description, type, address, city, country, latitude, longitude, pricePerNight, maxGuests, bedrooms, amenityIds et imageUrls. Le serveur gère id, ownerId et status. Les réponses ajoutent ownerName, amenities, averageRating, reviewCount et currency.

Types : ENTIRE_HOME, PRIVATE_ROOM, SHARED_ROOM, HOTEL_ROOM. Les deux dates de recherche doivent être renseignées ensemble.

Le devis renvoie `{nights,totalPrice,currency,available,days:[{date,available,price}]}`. Le calendrier ne contient ni identifiant de réservation ni identité de voyageur.

## Photos

`POST /uploads/images` accepte un formulaire multipart avec une ou plusieurs parties `files`, pour un hôte connecté. Réponse 201 : `[{id,url}]`.

- JPEG ou PNG, 5 Mo et 24 mégapixels maximum par photo.
- Au maximum 10 photos par annonce ; ordre de imageUrls = ordre de la galerie, première image = couverture.
- Les images sont vérifiées et réencodées côté serveur.
- Utiliser les URL retournées dans imageUrls lors de la publication ou de la modification.
- Les fichiers sont disponibles publiquement sur `/media/{filename}`, hors du préfixe /api.
- Une photo téléversée doit appartenir à l’hôte du logement et ne peut pas être affectée à une autre annonce en même temps.
- Les photos non associées depuis 24 h sont nettoyées périodiquement.

Stockage configurable via UPLOAD_DIR, par défaut ./data/uploads. Les URL HTTPS existantes restent lisibles.

## Calendrier hôte

| Méthode | Route | Description |
| --- | --- | --- |
| GET | /availabilities/{propertyId} | Exceptions du calendrier, avec startDate et endDate |
| PUT | /availabilities | propertyId, date, isAvailable, priceOverride |

Accès réservé à l’hôte du logement ou à un administrateur. Un prix absent ou null rétablit le tarif de base. Un isAvailable absent conserve sa valeur. Une nuit réservée ne peut pas être bloquée. Les réservations de l’hôte complètent les exceptions pour dessiner le calendrier.

## Demandes et réservations

| Statut | Signification |
| --- | --- |
| REQUESTED | Demande ouverte pendant 48 h, sans blocage des nuits |
| PENDING | Accord de l’hôte ; nuits bloquées pendant 30 min pour le paiement |
| CONFIRMED | Paiement réussi |
| REJECTED | Refus de l’hôte ou dates réservées par un autre voyageur |
| EXPIRED | Délai de décision ou de paiement écoulé |
| CANCELLED | Demande annulée ou séjour remboursé |
| COMPLETED | Séjour clôturé après le départ |

| Méthode | Route | Accès / corps |
| --- | --- | --- |
| POST | /bookings | Voyageur ; propertyId, startDate, endDate, numberOfGuests, guestMessage facultatif (1000 caractères) |
| GET | /bookings/user/me | Demandes et séjours du voyageur |
| GET | /bookings/user/{userId} | Utilisateur concerné ou administrateur |
| GET | /bookings/host/me | Demandes et séjours de tous les logements de l’hôte connecté |
| GET | /bookings/property/{propertyId} | Hôte du logement ou administrateur |
| GET | /bookings/{id} | Voyageur concerné, hôte du logement ou administrateur |
| PUT | /bookings/{id}/accept | Hôte du logement ; accepte une demande active |
| PUT | /bookings/{id}/reject | Hôte du logement ; corps facultatif {reason}, maximum 500 caractères |
| PUT | /bookings/{id}/cancel | Voyageur ; annule une demande active impayée ; 204 |
| PUT | /bookings/{id}/complete | Hôte du logement ; clôture un séjour confirmé après son départ |

Les réponses incluent les identifiants, dates, nombre de voyageurs, totalPrice, status, expiresAt, guestMessage, decisionReason, travelerName, propertyTitle, propertyCity, propertyImage, ownerId, ownerName et currency.

Plusieurs voyageurs peuvent demander les mêmes nuits. Une acceptation réserve temporairement ces nuits ; un autre accord incompatible renvoie 409. L’expiration libère les nuits sans refuser les autres demandes. Le paiement réussi refuse les demandes concurrentes qui se chevauchent.

Le prix est fixé à la création de la demande. Accepter plusieurs fois la même demande ne prolonge pas son échéance. Le traitement d’expiration passe toutes les minutes ; le serveur refuse immédiatement les opérations après l’échéance.

## Paiement de démonstration

| Méthode | Route | Accès |
| --- | --- | --- |
| POST | /payments/process/{bookingId}?method=CREDIT_CARD | Voyageur, après accord de l’hôte |
| GET | /payments/booking/{bookingId} | Participants au séjour ou administrateur |
| POST | /payments/{paymentId}/refund | Participants au séjour ou administrateur, avant le jour d’arrivée |

Méthodes simulées : CREDIT_CARD, PAYPAL, STRIPE, BANK_TRANSFER. Aucun débit réel. Activer PAYMENT_MODE=demo ; sinon 503.

Le paiement confirme la réservation dans une transaction. Les appels répétés après un succès renvoient le même paiement. Le remboursement annule aussi le séjour. Une réservation historique PENDING conserve son échéance et peut être payée.

## Avis

| Méthode | Route | Description |
| --- | --- | --- |
| POST | /reviews | bookingId, rating (1 à 5), comment ; voyageur d’un séjour COMPLETED ; 201 |
| GET | /reviews/can-review?bookingId=... | Le compte connecté peut-il déposer un avis ? |
| GET | /reviews/property/{propertyId} | Avis publics : id, authorId, authorName, rating, comment, createdAt |
| DELETE | /reviews/{reviewId} | Auteur de l’avis ; 204 |

## Messagerie

| Méthode | Route | Description |
| --- | --- | --- |
| GET | /conversations/user/{userId}?archived=false | Conversations actives du compte connecté ; archived=true pour ses archives |
| POST | /conversations?user1Id=...&user2Id=... | user1Id est le compte connecté ; bookingId facultatif et vérifié |
| GET | /conversations/{id}/messages | Messages visibles pour le participant connecté |
| POST | /conversations/{id}/messages | receiverId et content, maximum 5000 caractères |
| PUT | /conversations/{id}/read | Marque les messages reçus comme lus |
| DELETE | /conversations/messages/{id} | Masque un message pour le compte connecté uniquement ; 204 |
| PUT | /conversations/{id}/archive | Archive pour le compte connecté ; 204 |
| PUT | /conversations/{id}/unarchive | Réactive sa conversation ; 204 |

Un nouveau message réactive la conversation pour les deux participants. Les champs client historiques senderId, authorId et userId des créations ne déterminent pas l’identité.

## Administration

Les API administrateur restent accessibles aux comptes ROLE_ADMIN : GET /admin/users, PUT /admin/users/{userId}/role, DELETE /admin/users/{userId}, GET /admin/stats, PUT /admin/properties/{id}/status. Elles ne disposent pas d’écran dans l’interface voyageur/hôte.

## Erreurs

Réponse structurée : status, message, timestamp et path. Codes : 400 requête invalide, 401 authentification, 403 accès interdit, 404 ressource absente, 409 conflit métier ou concurrent, 413 photo trop volumineuse, 503 paiement désactivé. Après un 409, recharger les données avant une nouvelle tentative.
