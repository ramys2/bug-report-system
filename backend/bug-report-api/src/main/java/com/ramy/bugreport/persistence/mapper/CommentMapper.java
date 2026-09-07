package com.ramy.bugreport.persistence.mapper;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.persistence.entity.CommentEntity;

public final class CommentMapper {
    private CommentMapper() {
    }

    public static CommentEntity toEntity(Comment domain) {
        if (domain == null) {
            return null;
        }
        CommentEntity entity = new CommentEntity();
        entity.setId(domain.getId());
        entity.setBugReportId(domain.getBugReportId());
        entity.setAuthorId(domain.getAuthorId());
        entity.setContent(domain.getContent());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static Comment toDomain(CommentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Comment(
                entity.getId(), entity.getBugReportId(), entity.getAuthorId(),
                entity.getContent(), entity.getCreatedAt());
    }
}
