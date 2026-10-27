package com.example.demo.entities;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor @Document("upload_assets")
public class UploadAsset {
    @Id private String id;
    @Indexed private String ownerId;
    private String filename;
    private String propertyId;
    private Instant createdAt;
    private boolean deleting;
}
