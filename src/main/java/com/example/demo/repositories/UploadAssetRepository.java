package com.example.demo.repositories;
import com.example.demo.entities.UploadAsset;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;
public interface UploadAssetRepository extends MongoRepository<UploadAsset,String> {
    List<UploadAsset> findByPropertyIdIsNullAndCreatedAtBefore(Instant before);
    List<UploadAsset> findByPropertyId(String id);
}
