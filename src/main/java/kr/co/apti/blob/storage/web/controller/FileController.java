package kr.co.apti.blob.storage.web.controller;

import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@Slf4j
@RequestMapping(value = "/gcs", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=utf-8")
public class FileController {

    @PostMapping("/upload")
    public void uploadGCS(@RequestParam("fileName") MultipartFile file) throws IOException {

        log.debug("POST upload {} ", file.getName());
        // 파일명 정의
        String now = new SimpleDateFormat("yyyyMMddHHmmS").format(new Date());
        String fileName = now + "_" + file.getOriginalFilename();

        // GCS(Google Cloud Storage) 자격 인증
        Storage storage = StorageOptions.getDefaultInstance().getService();

        // 버킷 설정(환경 yml 파일 등록 처리)
        String bucketName = "apti-private-bucket";

        // 파일 경로
        String filePath = "rental-contract";

        // GCS -> Blob 객체
        BlobId blobId = BlobId.of(bucketName, filePath + "/" + fileName);

        log.info("파일 이름은 {}", fileName);

        // BlobId로 BlobInfo 객체 생성
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        // InputStream -> GCS Storage
        Blob blob = storage.createFrom(blobInfo, file.getInputStream());


    }


    @GetMapping(value = "/download")
    public ResponseEntity<Resource> downloadGCS(@RequestParam("objectName") String objectName, HttpServletRequest httpServletRequest) throws IOException {

        log.debug("GET download {} ", objectName);

        String remoteHost = httpServletRequest.getRemoteHost();

        // 버킷 설정(환경 yml 파일 등록 처리)
        String bucketName = "apti-private-bucket";

        // 파일 경로
        String filePath = "rental-contract";

        // GCS 인증
        Storage storage = StorageOptions.getDefaultInstance().getService();

        // GCS -> Blob 객체
        Blob blob = storage.get(BlobId.of(bucketName, filePath + "/" + objectName));

        // App Engine에 별도에 파일 저장은 하지 않고 Byte Array를 바로 InputStream Resource로 응답
        String fileName = blob.getName().substring(blob.getName().lastIndexOf("/") + 1);

        // Blob -> Byte 배열
        byte[] fileContent = blob.getContent();

        // Byte -> InputStream
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(fileContent));

        // 응답 객체 파일 스트림 추가
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .body(resource);
    }

}
