package com.example.demo.services;

import com.example.demo.entities.Property;
import com.example.demo.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/** All booking and calendar writers touch the same document before checking dates. */
@Service
@RequiredArgsConstructor
public class PropertyCalendar {
    private final MongoTemplate mongo;

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(String propertyId) {
        var result = mongo.updateFirst(Query.query(where("_id").is(propertyId)),
                new Update().inc("calendarRevision", 1), Property.class);
        if (result.getMatchedCount() == 0) throw ApiException.notFound("Logement introuvable");
    }
}
