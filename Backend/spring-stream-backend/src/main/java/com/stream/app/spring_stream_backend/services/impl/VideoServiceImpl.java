package com.stream.app.spring_stream_backend.services.impl;

import com.stream.app.spring_stream_backend.entities.Video;
import com.stream.app.spring_stream_backend.repository.VideoRepository;
import com.stream.app.spring_stream_backend.services.VideoService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class VideoServiceImpl  implements VideoService {

    @Value("${files.video}")
    String DIR;


    @Value("${files.video.hls}")
    String HLS_DIR;

    @Autowired
    private VideoRepository videoRepository;

    @PostConstruct
    public void init(){

        File file1 = new File(HLS_DIR);

        if(!file1.exists()) {
            file1.mkdir();
        }


        File file = new File(DIR);
        if(!file.exists()){
            file.mkdir();
            System.out.println("Folder Created:");
        } else{
            System.out.println("Folder Already created: ");
        }

    }

    @Override
    public Video save(Video video, MultipartFile file) {

        try{
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            // File Path
            String cleanFileName = StringUtils.cleanPath(filename);
            // folder path
            String cleanFolder =  StringUtils.cleanPath(DIR);

            // folderpath with filename
            Path path = Paths.get(cleanFolder, cleanFileName);

            System.out.println(contentType);
            System.out.println(path);

            // copy file to folder
            Files.copy(inputStream,path, StandardCopyOption.REPLACE_EXISTING);

            // video meta data
            video.setContentType(contentType);
            video.setFilePath(path.toString());

            //save video metadata
            videoRepository.save(video);

            // processing video
            processVideo(video.getVideoId());

            // TODO: delete actual video file and database entry if exception occur while processing


            return video;

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public Video get(String videoId) {
        return videoRepository.findById(videoId).orElseThrow(()->new RuntimeException("Video Not Found!"));
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }

    @Override
    public String processVideo(String videoId) {
        Video video = this.get(videoId);
        String filePath = video.getFilePath();

        //path where to store
        Path  videoPath = Paths.get(filePath);

/*
        String output360 = HLS_DIR + videoId + "/360p/";
        String output720 = HLS_DIR + videoId + "/720p/";
        String output1080 = HLS_DIR + videoId + "/1080p/";
*/

        try {
/*
            Files.createDirectories(Paths.get(output360));
            Files.createDirectories(Paths.get(output720));
            Files.createDirectories(Paths.get(output1080));
*/
            // Ffmpeg Command

            Path outputPath = Paths.get(HLS_DIR,videoId);

            Files.createDirectories(outputPath);

            String ffmpegCmd = String.format(
                "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\" \"%s/master.m3u8\" ",
                    videoPath, outputPath,outputPath
            );

/*
            StringBuilder ffmpegCmd = new StringBuilder();
            ffmpegCmd.append("ffmpeg -i").append(videoPath.toString())
                    .append(" ")
                    .append("-map 0:v -map 0:a -s:v:0 640x360 -b:v:0 800k ")
                    .append("-map 0:v -map 0:a -s:v:1 1280x720 -b:v:1 2800k ")
                    .append("-map 0:v -map 0:a -s:v:2 1920x1080 -b:v:2 5000k ")
                    .append("-var_stream_map \"v:0,a:0 v:1,a:0 v:2,a:0\" ")
                    .append("-master_pl_name ").append(HLS_DIR)
                    .append(videoId).append("/master.m3u8 ")
                    .append("-f hls -hls_time 10 -hls_list_size 0 ")
                    .append("-hls_segment_filename \"").append(HLS_DIR).append(videoId)
                    .append("/v%v/files")
                    .append("\"").append(HLS_DIR).append(videoId).append("/v%v/prog_index.m3u8\"");
*/

            System.out.println(ffmpegCmd);
            // file this command
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exit = process.waitFor();
            if(exit != 0){
                throw new RuntimeException("Video Processing failed!");
            }
            return videoId;

        }catch (IOException e) {
            throw new RuntimeException("Video Processing Fail");
        }
        catch (InterruptedException ex){
            throw  new RuntimeException(ex);
        }
    }
}
