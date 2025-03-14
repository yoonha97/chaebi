package com.backend.service.recipient;

import com.backend.domain.*;
import com.backend.dto.*;
import com.backend.exception.AlreadyExistsException;
import com.backend.exception.NotFoundException;
import com.backend.repository.GalleryRepository;
import com.backend.repository.RecipientRepository;
import com.backend.service.gallery.GalleryService;
import com.backend.service.idconvert.IdConverterServiceImpl;
import com.backend.service.letter.LetterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipientServiceImpl implements RecipientService{ //열람인 CRUD 구현

    private final RecipientRepository repository;
    private final LetterService letterService;
    private final GalleryService galleryService;
    private final IdConverterServiceImpl idConverterService;
    private final GalleryRepository galleryRepository;

    @Override
    public Long createRecipient(RecipientDTO recipientDTO, User user, MultipartFile file) {
        if(repository.findByUserAndPhone(user, recipientDTO.getPhone()) != null){
            throw new AlreadyExistsException(recipientDTO.getName() +  "님은 이미 등록되었습니다.");
        }
        String imgUrl = "";
        Long id = null; // trigger 용
        if (file != null){
            imgUrl = galleryService.uploadProfile(file, id, user);
        }
        Recipient recipient = Recipient.builder()
                .phone(recipientDTO.getPhone())
                .name(recipientDTO.getName())
                .securityQuestion(recipientDTO.getSecretQuestion())
                .securityAnswer(recipientDTO.getSecretAnswer())
                .user(user)
                .imgurl(imgUrl)
                .build();


        Recipient saved = repository.save(recipient); //열람자 저장
        //편지 생성
        Letter letter = letterService.createLetter(user, recipient);
        recipient.setLetter(letter);
        return saved.getId();
    }

    @Override
    public RecipientResDTO getRecipient(long id) {
        Recipient recipient = repository.findById(id).orElseThrow(NoSuchElementException::new);
        RecipientResDTO recipientResDTO = RecipientResDTO.builder()
                .id(recipient.getId())
                .name(recipient.getName())
                .phone(recipient.getPhone())
                //.imgUrl(recipient.getImgurl())
                .secretQuestion(recipient.getSecurityQuestion())
                .secretAnswer(recipient.getSecurityAnswer())
                .build();
        return recipientResDTO;
    }

    @Override
    public List<RecipientResDTO> getRecipients(User user) {
        // 사용자 열람자 리스트 조회
        List<Recipient> recipients = repository.findByUser(user);

        // 결과가 없을 경우 빈 Optional 반환
//        if (recipients.isEmpty()) {
//            return Optional.empty();
//        }

        // Recipient 엔티티 리스트를 RecipientDTO 리스트로 변환
        List<RecipientResDTO> recipientDTOs = recipients.stream()
                .map(recipient -> RecipientResDTO.builder()
                        .id(recipient.getId())
                        .phone(recipient.getPhone())  // 전화번호
                        .name(recipient.getName())    // 이름
                        .secretQuestion(recipient.getSecurityQuestion()) // 비밀 질문
                        .secretAnswer(recipient.getSecurityAnswer())     // 비밀 답변
                        .imgUrl(recipient.getImgurl())// 이미지 URL
                        .lastModified(letterService.getLetter(recipient.getId()).getLastModifiedDate())
                        .build())
                .collect(Collectors.toList());

        return recipientDTOs; // DTO 리스트 반환
    }

    @Override
    public void updateRecipient(RecipientDTO recipientDTO, User user, MultipartFile file, Long recipientId) { // 열람자 업데이트
        Recipient recipient = repository.findById(recipientId).get();
        if (recipient == null) {
            throw new NotFoundException("Recipient not found");
        }
        Set<GalleryRecipient> existingGalleryRecipients = recipient.getGalleryRecipients();

        // 이미지 처리
        String imgUrl = recipient.getImgurl(); // 기존 이미지 URL 유지
        if (file != null && !file.isEmpty()) {
            imgUrl = galleryService.uploadProfile(file, recipient.getId(), user);
        }

        recipient.setName(recipientDTO.getName());
        recipient.setPhone(recipientDTO.getPhone());
        recipient.setSecurityQuestion(recipientDTO.getSecretQuestion());
        recipient.setSecurityAnswer(recipientDTO.getSecretAnswer());
        recipient.setImgurl(imgUrl);
        recipient.setGalleryRecipients(existingGalleryRecipients);

        // 변경된 엔티티 저장
        repository.save(recipient);
    }

    @Override
    public void deleteRecipient(long id) {  //열람자 삭제
        repository.deleteById(id);

        List<Gallery> emptyGalleries = galleryRepository.findGalleriesWithNoRecipients();

        // 3. 열람자가 없는 갤러리들 삭제
        galleryRepository.deleteAll(emptyGalleries);
    }

    @Override
    public EnterRes enterRecipient(EnterReq req) {  //열람자 삭제
        //PairDTO pair = idConverterService.extractIds(req.getEnterCode());
        Recipient recipient = repository.findByEnterCode(req.getEnterCode());
        if(recipient == null){
            throw new NotFoundException("Not found: " + req.getEnterCode());
        }
        EnterRecipient enterR = EnterRecipient.builder()
                .id(recipient.getId())
                .phone(recipient.getPhone())
                .secretQuestion(recipient.getSecurityQuestion())
                .secretAnswer(recipient.getSecurityAnswer())
                .name(recipient.getName())
                .build();

        UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                .userName(recipient.getUser().getName())
                .userId(recipient.getUser().getId())
                .phoneNumber(recipient.getUser().getPhone())
                .build();

        EnterRes res = EnterRes.builder()
                .enterRecipient(enterR)
                .userInfo(userInfoDTO)
                .build();

        if(!repository.findById(recipient.getId()).isPresent())
            throw new NotFoundException("Recipient not found");
        return res;
    }
}
