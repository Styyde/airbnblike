package com.example.demo.controllers;
import com.example.demo.services.ImageStore;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController @RequiredArgsConstructor
public class ImageController {
    private final ImageStore store;
    @PostMapping(value="/api/uploads/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ImageStore.Image> upload(@RequestPart("files") List<MultipartFile> files){ return store.upload(files); }
    @GetMapping("/media/{filename}")
    public ResponseEntity<Resource> read(@PathVariable String filename) {
        return ResponseEntity.ok().contentType(filename.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(1,TimeUnit.DAYS)).body(store.read(filename));
    }
}
