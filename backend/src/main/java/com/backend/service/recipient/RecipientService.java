package com.backend.service.recipient;

import com.backend.domain.User;
import com.backend.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface RecipientService {
    Long createRecipient(RecipientDTO recipientDTO, User user, MultipartFile file); //열람인 생성
    RecipientResDTO getRecipient(long id); // 열람인 조회
    List<RecipientResDTO> getRecipients(User user); // 그 유저의 열람인들 불러오기
    void updateRecipient(RecipientDTO recipientDTO, User user, MultipartFile file, Long recipientId);
    void deleteRecipient(long id);
    EnterRes enterRecipient(EnterReq req);
}
