package com.stream.app.spring_stream_backend.controller;

import com.stream.app.spring_stream_backend.AppConstants;
import com.stream.app.spring_stream_backend.entities.Video;
import com.stream.app.spring_stream_backend.playload.CustomMessage;
import com.stream.app.spring_stream_backend.services.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
@CrossOrigin("*")
public class VideoController {

    private VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    // Upload Video
    @PostMapping()
    public ResponseEntity<?> create(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description)
    {
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoId(UUID.randomUUID().toString());

        Video savedVideo = videoService.save(video,file);

        if(savedVideo != null){
            return ResponseEntity.status(HttpStatus.OK).body(video);
        }
        else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CustomMessage.builder().message("Video Not Uploaded").success(false).build()
            );
        }

    }

    // Stream Video -> URL= http://localhost:8080/api/v1/videos/stream/312144
    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> stream(@PathVariable String videoId){
        Video video = videoService.get(videoId);

        String contentType = video.getContentType();
        String filePath = video.getFilePath();

        if(contentType== null){
            contentType = "application/octet-stream";
        }

        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);

    }

    // Get All Videos List
    @GetMapping()
    public List<Video> getAll(){
        return videoService.getAll();
    }

    // Stream Video in Chunks

    @GetMapping("/stream/range/{videoId}")
    public ResponseEntity<Resource> streamVideoRange(@PathVariable String videoId, @RequestHeader(value = "Range", required = false) String range){
        System.out.println(range);

        Video video= videoService.get(videoId);
        Path path = Paths.get(video.getFilePath());

        Resource resource = new FileSystemResource(path);
        String contentType = video.getContentType();

        if(contentType == null){
            contentType = "application/octet-stream";
        }

        // Total Length of the video
        long fileLength = path.toFile().length();

        // if range header is null
        if(range == null){
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }

        // else- range header is not null
        long rangeStart;
        long rangeEnd;

        // example=> bytes=1001-2002
        String[] ranges = range.replace("bytes=","").split("-");
        rangeStart = Long.parseLong(ranges[0]);

        rangeEnd = rangeStart + AppConstants.CHUNK_SIZE -1;
        if(rangeEnd >= fileLength){
            rangeEnd = fileLength-1;
        }

//        if(ranges.length > 1){
//            rangeEnd = Long.parseLong(ranges[1]);
//        }
//        else{
//            rangeEnd= fileLength-1;
//        }
//
//        if(rangeEnd> fileLength-1){
//            rangeEnd = fileLength-1;
//        }

        System.out.println("Start Range: " + rangeStart);
        System.out.println("END Range: " + rangeEnd);

        InputStream inputStream;

        try{
            inputStream = Files.newInputStream(path);
            inputStream.skip(rangeStart);
            long contentLength = rangeEnd-rangeStart+1;

            byte[] data = new byte[(int) contentLength];
            int  read = inputStream.read(data,0,data.length);
            System.out.println("read (number of bytes) : " + read );

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range","bytes "+ rangeStart + "-" + rangeEnd + "/" + fileLength);
            headers.add("Cache-Control","no-cache, no-store, must-revalidate");
            headers.add("Pragma","no-cache");
            headers.add("Expires","0");
            headers.add("X-Content-Type-Options","nosniff");
            headers.setContentLength(contentLength);

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(data));

        }catch(IOException ex){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }


    }


    //Serve HLS playlist

    // master.m3u8 file
    @Value("${files.video.hls}")
    private String HLS_DIR;

    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<Resource> ServerMasterFile(@PathVariable String videoId){
        //Creating Path
        Path path = Paths.get(HLS_DIR,videoId,"master.m3u8");
        System.out.println(path);

        if(!Files.exists(path)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_TYPE,"application/vnd.apple.mpegurl"
                ).body(resource);
    }

    // Serve the segments
    @GetMapping("/{videoId}/{segment}.ts")
    public ResponseEntity<Resource> ServeSegment(@PathVariable String videoId, @PathVariable String segment){
        // Create path for segment
        Path path = Paths.get(HLS_DIR,videoId,segment+ ".ts");

        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "video/mp2t"
                ).body(resource);
    }

}
