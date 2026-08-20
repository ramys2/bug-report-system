package com.ramy.bugreport.component;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.security.UserAccountDetails;

@Component
public class CommentAuthorizer {
	private final ICommentRepository commentRepository;

	public CommentAuthorizer(ICommentRepository commentRepository) {
		this.commentRepository = commentRepository;
	}
	
	public boolean canDelete(UUID commentId, Authentication auth) {
		Comment comment = this.commentRepository.findById(commentId)
				.orElseThrow(() ->
                		new ResourceNotFoundException(
                				"Report with id: %s".formatted(commentId)
                		)
				);
		
		UserAccountDetails account = (UserAccountDetails) auth.getPrincipal();
		
		return account.getId().equals(comment.getAuthorId());
	}
	
}
