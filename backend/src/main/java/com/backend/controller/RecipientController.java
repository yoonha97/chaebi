package com.backend.controller;

import com.backend.domain.Recipient;
import com.backend.domain.User;
import com.backend.dto.RecipientDTO;
import com.backend.dto.RecipientResDTO;
import com.backend.service.letter.LetterService;
import com.backend.service.recipient.RecipientService;
import com.backend.service.user.UserService;
import com.backend.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recipient")
@RequiredArgsConstructor
@Tag(name = "열람자 관리", description = "사용자의 열람자를 관리")
@Validated
public class RecipientController {
    private final RecipientService recipientService;
    private final LetterService letterService;
    private final UserService userService;

    @Operation(summary = "열람자 등록")
    @PostMapping(value = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createRecipient(@RequestParam("file") MultipartFile file, @RequestBody RecipientDTO recipientDTO, HttpServletRequest request) {
        Optional<User> user = userService.getUserByToken(request);
        if (user.isPresent()) {
            recipientService.createRecipient(recipientDTO, user.get());
            return ResponseEntity.ok("create successful");
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // 유효하지 않은 토큰
        }
    }

    @Operation(summary = "열람자 상세")
    @GetMapping("/{id}")
    public ResponseEntity<?> getRecipientById(@PathVariable long id) {
        return ResponseEntity.ok(recipientService.getRecipient(id));
    }

    @Operation(summary = "열람자 목록")
    @GetMapping("/list")
    public ResponseEntity<List<RecipientResDTO>> getRecipientList(HttpServletRequest request) {
        // JWT를 통해 사용자 정보를 가져옴
        Optional<User> user = userService.getUserByToken(request);
        if (user.isPresent()) {
            Optional<List<RecipientResDTO>> recipients = recipientService.getRecipients(user);
            if (recipients.isPresent()) {
                return ResponseEntity.ok(recipients.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonList(new RecipientResDTO())); // 빈배열
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // 유효하지 않은 토큰
        }
    }

    @Operation(summary = "열람자 정보 수정")
    @PostMapping(value = "/update",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateRecipient(@RequestParam("file") MultipartFile file,@RequestBody RecipientDTO recipientDTO, HttpServletRequest request) {
        Optional<User> user = userService.getUserByToken(request);
        if (user.isPresent()) {
            recipientService.updateRecipient(recipientDTO, user.get());
            return ResponseEntity.ok("Update successful");
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // 유효하지 않은 토큰
        }
    }

    @Operation(summary = "열람자 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRecipient(@PathVariable long id) {
        recipientService.deleteRecipient(id);
        return ResponseEntity.ok("Delete successful");
    }



}
