package com.rishabh.cipherchat.repository;

import com.rishabh.cipherchat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<List<User>> findAllByEmailIn(List<String> emails);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY u.dateCreated DESC")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
}
