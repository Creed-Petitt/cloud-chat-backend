package com.creedpetitt.aiservicesbackend.repositories;

import com.creedpetitt.aiservicesbackend.models.Conversation;
import com.creedpetitt.aiservicesbackend.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    @Query("SELECT m FROM Message m WHERE m.user = :user AND m.messageType = :messageType ORDER BY m.createdAt DESC")
    List<Message> findAllByUserAndMessageType(@Param("user") com.creedpetitt.aiservicesbackend.models.AppUser user, @Param("messageType") com.creedpetitt.aiservicesbackend.models.Message.MessageType messageType);

    void deleteAllByConversation(Conversation conversation);
}