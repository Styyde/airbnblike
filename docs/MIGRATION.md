# Reprise d'une base existante

Les tests utilisent une base jetable. Aucune migration n'est exécutée automatiquement sur la base existante.

Avant d'utiliser le nouveau backend avec des données de l'ancienne version : arrêter ses écritures, sauvegarder la base, puis travailler sur une copie. MongoDB doit fonctionner en replica set. Le Compose de développement crée une autre instance sur le port 27018 et ne déplace pas les données d'une instance locale.

## Contrôles préalables

- Résoudre les doublons sur payments.bookingId, reviews.bookingId, availabilities.(propertyId,date) et conversations.(user1Id,user2Id). Pour les conversations, trier les participants avant de détecter les doublons et réaffecter les messages si des fils sont fusionnés.
- Vérifier les références : utilisateur et logement de chaque réservation, réservation de chaque paiement ou avis, participants et conversation de chaque message.
- Vérifier que les réservations actives ne se chevauchent pas. Les anciennes incohérences ne sont pas réparées par les nouveaux contrôles.
- Normaliser les emails en minuscules, après détection des collisions, pour correspondre au login et à la nouvelle inscription.
- Déterminer le type réel de chaque logement. L'ancien champ libre pouvait désigner un bâtiment (Appartement, Maison, Villa), alors que PropertyType décrit le mode d'occupation. Affecter explicitement ENTIRE_HOME, PRIVATE_ROOM, SHARED_ROOM ou HOTEL_ROOM selon le besoin ; ne pas convertir aveuglément toutes les anciennes valeurs.
- Réconcilier les paiements : l'ancien simulateur ne conservait pas les références de transaction. Un ancien paiement ne peut pas recevoir une référence de démonstration inventée ; son remboursement demande un traitement explicite.

## Champs à reprendre

Les commandes suivantes sont des exemples pour mongosh sur la copie sauvegardée et vérifiée de la base. Elles ne doivent être lancées qu'après les contrôles ci-dessus.

```javascript
// Initialiser les versions est nécessaire : @Version null signifie nouvel objet.
for (const name of ["users", "properties", "bookings", "payments", "conversations"]) {
  db.getCollection(name).updateMany({version: {$exists: false}}, {$set: {version: NumberLong(0)}});
}
db.users.updateMany({active: {$exists: false}}, {$set: {active: true}});
db.properties.updateMany({calendarRevision: {$exists: false}}, {$set: {calendarRevision: NumberLong(0)}});
db.properties.updateMany({imageUrls: {$exists: false}}, {$set: {imageUrls: []}});

// Préserver l'ancien archivage partagé pour les deux participants.
db.conversations.updateMany({archivedForUser1: {$exists: false}}, [
  {$set: {archivedForUser1: {$ifNull: ["$isArchived", false]}, archivedForUser2: {$ifNull: ["$isArchived", false]}}}
]);

// Ajouter le rôle hôte aux propriétaires existants sans retirer leurs autres rôles.
for (const property of db.properties.find({}, {ownerId: 1})) {
  const ownerId = property.ownerId;
  const candidates = [ownerId];
  if (typeof ownerId === "string" && /^[0-9a-fA-F]{24}$/.test(ownerId)) candidates.push(ObjectId(ownerId));
  db.users.updateOne({_id: {$in: candidates}}, {$addToSet: {roles: "ROLE_OWNER"}});
}

// Expiration des anciennes attentes : vérifier l'impact métier avant application.
db.bookings.updateMany({status: "PENDING", expiresAt: {$exists: false}}, [
  {$set: {expiresAt: {$dateAdd: {startDate: "$createdAt", unit: "minute", amount: 30}}}}
]);
```

Convertir ensuite les champs monétaires en BSON Decimal128 avec une politique d'arrondi validée : properties.pricePerNight, availabilities.priceOverride (sauf null), bookings.totalPrice et payments.amount. Pour une application à deux décimales, utiliser une conversion numérique puis un arrondi à deux décimales ; contrôler les différences avant d'enregistrer. Les coordonnées et les notes moyennes restent des nombres à virgule flottante.

Après reprise : démarrer le backend sur cette copie, laisser créer les index uniques, vérifier un login, une modification de logement, une réservation, un paiement de démonstration neuf, un remboursement et une conversation. Contrôler les statistiques et l'historique. Le passage de la base d'origine à la version reprise constitue une opération distincte ; conserver la sauvegarde pour pouvoir revenir en arrière.
