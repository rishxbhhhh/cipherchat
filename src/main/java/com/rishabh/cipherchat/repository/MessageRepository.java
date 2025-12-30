package com.rishabh.cipherchat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rishabh.cipherchat.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

}
