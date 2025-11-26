package com.aiinsight.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 리포지토리
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByNaverId(String naverId);

    Optional<User> findByEmail(String email);

    boolean existsByNaverId(String naverId);
}
