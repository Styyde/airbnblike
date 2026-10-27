package com.example.demo.config;
import com.example.demo.entities.Amenity;
import com.example.demo.repositories.AmenityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class ReferenceData implements ApplicationRunner {
    private final AmenityRepository repository;
    @Override public void run(ApplicationArguments args) {
        String[][] values={{"wifi","Wi-Fi","wifi","Essentiels"},{"kitchen","Cuisine équipée","kitchen","Essentiels"},
          {"parking","Parking gratuit","parking","Confort"},{"pool","Piscine","pool","Confort"},
          {"air-conditioning","Climatisation","air","Confort"},{"workspace","Espace de travail","desk","Essentiels"},
          {"washing-machine","Lave-linge","laundry","Essentiels"},{"terrace","Terrasse","sun","Extérieur"},
          {"garden","Jardin","garden","Extérieur"},{"heating","Chauffage","heat","Confort"}};
        for(var item:values) if(repository.findByName(item[1]).isEmpty() && !repository.existsById(item[0]))
            repository.save(new Amenity(item[0],item[1],item[2],item[3]));
    }
}
