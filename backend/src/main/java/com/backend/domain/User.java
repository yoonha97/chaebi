package com.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name= "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone; //전화번호

    private String name; //이름

    private LocalDateTime createdAt; //생성날짜
    private LocalDateTime lastLogin; //마지막 로그인
    private boolean status; //살아있는가 아닌가
    private int loginAttemptPeriod; //로그인 시도주기
    private boolean admin; //관리자 인지 아닌지
    private String fcmToken;
    private boolean push; // 푸쉬알림

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
