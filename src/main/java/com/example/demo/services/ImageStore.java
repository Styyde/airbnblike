package com.example.demo.services;

import com.example.demo.entities.*;
import com.example.demo.repositories.*;
import com.example.demo.security.ResourceAccess;
import com.example.demo.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.*;
import org.springframework.transaction.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.*;
import java.io.*;
import java.time.*;
import java.util.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class ImageStore {
    private final UploadAssetRepository assets;
    private final ResourceAccess access;
    private final MongoTemplate mongo;
    private final Path directory;
    public ImageStore(UploadAssetRepository assets, ResourceAccess access, MongoTemplate mongo,
            @Value("${app.upload-dir:./data/uploads}") String directory) {
        this.assets=assets; this.access=access; this.mongo=mongo; this.directory=Path.of(directory).toAbsolutePath().normalize();
    }
    public record Image(String id,String url) {}
    public List<Image> upload(List<MultipartFile> files) {
        var user=access.current();
        if (user.getAuthorities().stream().noneMatch(a -> List.of("ROLE_OWNER","ROLE_ADMIN").contains(a.getAuthority())))
            throw new AccessDeniedException("Devenez hôte pour ajouter des photos.");
        if (files.isEmpty() || files.size()>10) throw ApiException.badRequest("Choisissez entre 1 et 10 photos.");
        List<Image> uploaded=new ArrayList<>();
        try {
            Files.createDirectories(directory);
            for (var file : files) {
                if (file.isEmpty() || file.getSize()>5L*1024*1024) throw ApiException.badRequest("Chaque photo doit peser au maximum 5 Mo.");
                try (ImageInputStream input=ImageIO.createImageInputStream(file.getInputStream())) {
                    var readers=ImageIO.getImageReaders(input);
                    if (!readers.hasNext()) throw ApiException.badRequest("Format invalide : utilisez des photos JPEG ou PNG.");
                    var reader=readers.next();
                    try {
                        String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                        if (!List.of("jpeg","jpg","png").contains(format)) throw ApiException.badRequest("Seuls JPEG et PNG sont acceptés.");
                        reader.setInput(input);
                        if ((long)reader.getWidth(0)*reader.getHeight(0)>24_000_000) throw ApiException.badRequest("Photo trop grande : maximum 24 mégapixels.");
                        var image=reader.read(0);
                        String id=UUID.randomUUID().toString(), extension=format.equals("png")?"png":"jpg";
                        String filename=id+"."+extension;
                        Path target=path(filename);
                        ImageIO.write(image,extension,target.toFile());
                        try { assets.save(new UploadAsset(id,user.getId(),filename,null,Instant.now(),false)); }
                        catch (RuntimeException ex) { Files.deleteIfExists(target); throw ex; }
                        uploaded.add(new Image(id,"/media/"+filename));
                    } finally { reader.dispose(); }
                }
            }
            return uploaded;
        } catch (IOException | RuntimeException ex) {
            for (var image : uploaded) {
                try { var asset=assets.findById(image.id()).orElse(null); if(asset!=null){ Files.deleteIfExists(path(asset.getFilename())); assets.deleteById(asset.getId()); } }
                catch (IOException ignored) { }
            }
            if(ex instanceof RuntimeException runtime) throw runtime;
            throw ApiException.badRequest("Impossible de lire cette photo. Choisissez un fichier JPEG ou PNG valide.");
        }
    }
    private Path path(String filename) {
        if(filename==null || !filename.matches("[a-f0-9-]{36}\\.(jpg|png)")) throw ApiException.notFound("Photo introuvable.");
        Path resolved=directory.resolve(filename).normalize();
        if(!resolved.startsWith(directory)) throw ApiException.notFound("Photo introuvable.");
        return resolved;
    }
    public Resource read(String filename) {
        var resource=new FileSystemResource(path(filename));
        if(!resource.exists()) throw ApiException.notFound("Photo introuvable.");
        return resource;
    }

    @Transactional(propagation=Propagation.MANDATORY)
    public void attach(Property property) {
        var urls=property.getImageUrls()==null?List.<String>of():property.getImageUrls();
        if(urls.size()>10) throw ApiException.badRequest("Une annonce peut contenir au maximum 10 photos.");
        for(String url:urls) {
            if(url.startsWith("/media/")) {
                String filename=url.substring(7); path(filename);
                var query=Query.query(where("filename").is(filename).and("ownerId").is(property.getOwnerId())
                        .and("deleting").ne(true).orOperator(where("propertyId").is(null),where("propertyId").is(property.getId())));
                var asset=mongo.findAndModify(query,new Update().set("propertyId",property.getId()),UploadAsset.class);
                if(asset==null) throw ApiException.badRequest("Une photo ne vous appartient pas ou n’est plus disponible.");
            } else if(!url.startsWith("https://")) throw ApiException.badRequest("URL de photo invalide.");
        }
        for(var asset:assets.findByPropertyId(property.getId())) {
            if(!urls.contains("/media/"+asset.getFilename())) {
                mongo.updateFirst(Query.query(where("_id").is(asset.getId())),new Update().set("propertyId",null).set("createdAt",Instant.now()),UploadAsset.class);
            }
        }
    }

    @Scheduled(fixedDelay=3600000,initialDelay=3600000)
    public void cleanup() throws IOException {
        var before=Instant.now().minus(Duration.ofHours(24));
        for(var candidate:assets.findByPropertyIdIsNullAndCreatedAtBefore(before)) {
            var claimed=mongo.findAndModify(Query.query(where("_id").is(candidate.getId()).and("propertyId").is(null)
                    .and("createdAt").lt(before).and("deleting").ne(true)),new Update().set("deleting",true),UploadAsset.class);
            if(claimed!=null){ Files.deleteIfExists(path(claimed.getFilename())); assets.deleteById(claimed.getId()); }
        }
    }
}
