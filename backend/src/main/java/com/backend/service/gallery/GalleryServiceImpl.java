package com.backend.service.gallery;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.backend.domain.Gallery;
import com.backend.domain.GalleryRecipient;
import com.backend.domain.Recipient;
import com.backend.domain.User;
import com.backend.dto.*;
import com.backend.exception.NotFoundException;
import com.backend.exception.UnauthorizedException;
import com.backend.repository.GalleryRecipientRepository;
import com.backend.repository.GalleryRepository;
import com.backend.repository.RecipientRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GalleryServiceImpl implements GalleryService {
    private final GalleryRepository galleryRepository;
    private final RecipientRepository recipientRepository;
    private final GalleryRecipientRepository galleryRecipientRepository;
    private final AmazonS3 s3Client;
    private final WebClient webClient;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${fastApi.url}")
    String fastApiUrl;

    @Transactional
    public GalleryResDTO uploadFile(MultipartFile file, UploadDTO uploadDTO, User user) {
        try {
            Set<Long> recipientIds = uploadDTO.getRecipientIds();

            System.out.println("1. Starting file upload process");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());

            // 파일 유효성 검사
            validateFile(file);
            System.out.println("2. File validation passed");

            // 유니크 키(파일명) 생성
            String fileName = file.getOriginalFilename();
            String key = generateUniqueKey(fileName);

            // 1. 먼저 파일을 S3에 업로드
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            try (var inputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        bucket,
                        key,
                        inputStream,
                        metadata
                );
                s3Client.putObject(putObjectRequest);
            }

            // 2. 파일 업로드 완료 후 조회용 Presigned URL 생성
            GeneratePresignedUrlRequest presignedUrlRequest = new GeneratePresignedUrlRequest(bucket, key)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(getPresignedUrlExpiration());

            URL presignedUrl = s3Client.generatePresignedUrl(presignedUrlRequest);

            //1. presignedUrl을 FastAPI에 전송
            //2. 메타데이터 토대로 정보 추출

            // 3. Gallery 엔티티 생성 및 저장
            Gallery gallery = new Gallery();
            gallery.setUser(user);
            gallery.setFileUrl(presignedUrl.toString());
            gallery.setFileType(determineFileType(file.getContentType()));
            gallery.setFileName(fileName);

            if (recipientIds != null && !recipientIds.isEmpty()) {
                List<Recipient> recipients = recipientRepository.findAllById(recipientIds);
                for (Recipient recipient : recipients) {
                    gallery.addRecipient(recipient);
                }
            }

            gallery = galleryRepository.save(gallery);
            return new GalleryResDTO(gallery);

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error during file upload: " + e.getMessage(), e);
        }
    }

    @Override //열람인의 프로필 업로드
    public String uploadProfile(MultipartFile file, Long id, User user) {
       
        try {
            if(id != null) {
                Recipient recipient = recipientRepository.findById(id).get();
                if (recipient.getImgurl() != null) { // 열람인의 프로필이 이미 있을 때
                    String key = extractFileKeyFromUrl(recipient.getImgurl());
                    deleteFileFromS3(key); // S3의 프로필 삭제
                }
            }
            // 유니크 키(파일명) 생성
            String fileName = file.getOriginalFilename();
            String uniqueKey = generateUniqueKey(fileName);
            // 1. 먼저 파일을 S3에 업로드
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            try (var inputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        bucket,
                        uniqueKey,
                        inputStream,
                        metadata
                );
                s3Client.putObject(putObjectRequest); //프로필 S3에 업로드
                GeneratePresignedUrlRequest presignedUrlRequest = new GeneratePresignedUrlRequest(bucket, uniqueKey)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(getPresignedUrlExpiration());
                //preSignedURL 반환
                return s3Client.generatePresignedUrl(presignedUrlRequest).toString();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error during file upload: " + e.getMessage(), e);
        }
        
    }


    //Presigned URL 반환
    @Transactional
    public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request, User user, String key) {

        // unique key 생성
        System.out.println("generate : " + key);
        // presigned URL 요청(aws)
        GeneratePresignedUrlRequest presignedUrlRequest = new GeneratePresignedUrlRequest(bucket, key)
                .withMethod(HttpMethod.GET)
                .withExpiration(getPresignedUrlExpiration())
                .withContentType(request.getContentType());

        // Generate the presigned URL
        URL presignedUrl = s3Client.generatePresignedUrl(presignedUrlRequest);
        return new PresignedUrlResponse(presignedUrl.toString());
    }

    //열람자 업데이트
    @Transactional
    public GalleryResDTO updateRecipients(Long galleryId, UpdateRecipientsReqDTO request) {
        Gallery gallery = galleryRepository.findById(galleryId)
                .orElseThrow(() -> new NotFoundException("Gallery not found"));

        // Clear existing recipients
        gallery.clearRecipients();

        // Set new recipients
        if (request.getRecipientIds() != null) {
            List<Recipient> recipients = recipientRepository.findAllById(request.getRecipientIds());
            for (Recipient recipient : recipients) {
                gallery.addRecipient(recipient);
            }
        }

        galleryRepository.save(gallery);
        return new GalleryResDTO(gallery);
    }

    //읽었는지 확인
    @Transactional
    public void markAsRead(Long galleryId, Long recipientId) {
        GalleryRecipient galleryRecipient = galleryRecipientRepository
                .findByGalleryIdAndRecipientId(galleryId, recipientId)
                .orElseThrow(() -> new NotFoundException("Gallery access not found"));

        galleryRecipient.markAsRead();
        galleryRecipientRepository.save(galleryRecipient);
    }




    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // 파일 크기 제한 (예: 10MB)
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit (10MB)");
        }

        // 허용된 파일 타입 검사
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            throw new IllegalArgumentException("Invalid file type. Only images and videos are allowed");
        }
    }

    //유니크 키 생성
    private String generateUniqueKey(String fileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomString = UUID.randomUUID().toString().substring(0, 8);

        String extension = "";
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex != -1) {
            extension = fileName.substring(lastDotIndex);
        }
        return String.format("uploads/%s_%s%s", timestamp, randomString, extension);
    }

    
    //presignedUrl 만료시간 설정
    private Date getPresignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60 * 24 * 6; // 6일
        expiration.setTime(expTimeMillis);
        return expiration;
    }

    //타입 확인
    private String determineFileType(String contentType) {
        if (contentType.startsWith("image/")) {
            return "IMAGE";
        } else if (contentType.startsWith("video/")) {
            return "VIDEO";
        } else {
            return "OTHER";
        }
    }

    //유저의 파일 들을 반환
    public List<GalleryResDTO> getFileUrlByUser(User user) {
        List<Gallery> urls = galleryRepository.findAllByUser(user);
        List<GalleryResDTO> list = toGalleryResDTOList(urls);
        return list;
    }

    @Transactional
    public void deleteFiles(List<Long> ids, User user) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Gallery IDs list cannot be empty");
        }

        List<Gallery> galleries = galleryRepository.findAllById(ids); //id에 해당하는 객체 모두 불러옴

        // 조회된 앨범 개수 확인
        if (galleries.size() != ids.size()) {
            throw new NotFoundException(ids.size() - galleries.size() + "개의 앨범을 찾지 못했습니다.");
        }

        galleries.forEach(gallery -> {
            if(!gallery.getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("소유가 아닌 앨범이 있습니다.");
            }
        });

        // 삭제 실패 키 값 저장
        List<String> failedDeletions = new ArrayList<>();

        for(Gallery g : galleries) {
            try{
                String key = extractFileKeyFromUrl(g.getFileUrl());
                deleteFileFromS3(key);
            }catch (Exception e) {
                failedDeletions.add(g.getFileName());
                continue;
            }
        }
        galleryRepository.deleteAll(galleries); // DB 삭제
        if (!failedDeletions.isEmpty()) {
            throw new RuntimeException("삭제 실패 목록 : " + String.join(", ", failedDeletions));
        }

    }

    // S3에서 실제로 파일을 삭제하는 private 메소드
    private void deleteFileFromS3(String fileKey) {
        String dir = "uploads/";
        try {
            System.out.println("key : " + fileKey );
            // S3에서 파일 확인
            if (s3Client.doesObjectExist(bucket, dir + fileKey)) {

                s3Client.deleteObject(bucket, dir + fileKey);
            } else {
                throw new NotFoundException("S3에서 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException("S3에서 삭제 실패: " + e.getMessage(), e);
        }
    }

    // URL에서 파일 키 추출하는 private 메소드
    private String extractFileKeyFromUrl(String fileUrl) {
        try {
            // Presigned URL에서 파일 키 추출
            URL url = new URL(fileUrl);
            String path = url.getPath();

            return path.substring(path.indexOf("/", 1) + 1);
        } catch (Exception e) {
            throw new RuntimeException("키값 추출에 실패했습니다.: " + e.getMessage(), e);
        }
    }

    //유저와 열람자의 파일명을 반환(마지막 송신했을 때 용)
    public List<GalleryResDTO> getFileUrlByUserAndRecipient(User user, Long recipientId) {

        List<Gallery> urls = galleryRepository.findByRecipientUserAndRecipientId(user, recipientId);
        // 갤러리 소유자 체크
        List<GalleryResDTO> list = toGalleryResDTOList(urls);
        return list;
    }

    private static List<GalleryResDTO> toGalleryResDTOList(List<Gallery> galleries) {
        return galleries.stream()
                .map(gallery -> new GalleryResDTO(gallery))
                .collect(Collectors.toList());
    }
    
    //Fast API 통신
    private List<String> sendToFastApi(URL presignedUrl){
        Map<String, Object> body = new HashMap<>();
        body.put("presignedUrl", presignedUrl);

        try {
            return webClient.post()
                    .uri(fastApiUrl)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            error -> Mono.error(new RuntimeException("Client Error")))
                    .onStatus(status -> status.is5xxServerError(),
                            error -> Mono.error(new RuntimeException("Server Error")))
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                    .block(Duration.ofSeconds(10)); // 10초 timeout 설정
        } catch (Exception e) {
            throw new RuntimeException("FastAPI 호출 중 에러 발생: " + e.getMessage());
        }
    }
}