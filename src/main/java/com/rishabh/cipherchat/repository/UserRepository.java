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

    @Query(value = "SELECT * FROM c_users u WHERE " +
           "(:search IS NULL OR LOWER(CAST(u.email AS text)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY u.date_created DESC",
           countQuery = "SELECT COUNT(*) FROM c_users u WHERE " +
           "(:search IS NULL OR LOWER(CAST(u.email AS text)) LIKE LOWER(CONCAT('%', :search, '%')))",
           nativeQuery = true)
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
}
