package com.backend.repository;

import com.backend.domain.Recipient;
import com.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    List<Recipient> findByUser(User user);
    Recipient findByUserAndPhone(User user, String phone);
    Recipient findByEnterCode(String phone);
}