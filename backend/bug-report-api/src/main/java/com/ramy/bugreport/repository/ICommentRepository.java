package com.ramy.bugreport.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ramy.bugreport.domain.Comment;

public interface ICommentRepository extends JpaRepository<Comment, UUID> {
    // No further implementation needed for now
}
