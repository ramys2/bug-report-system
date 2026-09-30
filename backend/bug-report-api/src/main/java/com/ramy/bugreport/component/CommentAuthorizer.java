package com.ramy.bugreport.component;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.ramy.bugreport.domain.Comment;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.ICommentRepository;
import com.ramy.bugreport.security.UserAccountDetails;

/**
 * Authorization helper used in {@code @PreAuthorize} expressions of {@code CommentService}
 * (as the bean {@code commentAuthorizer}).
 */
@Component
public class CommentAuthorizer {
	private final ICommentRepository commentRepository;

	public CommentAuthorizer(ICommentRepository commentRepository) {
		this.commentRepository = commentRepository;
	}
	
	/**
	 * Checks whether the signed-in user wrote the comment. Admins are handled separately in the expression.
	 *
	 * @param commentId id of the comment
	 * @param auth the current authentication; its principal must be a {@link com.ramy.bugreport.security.UserAccountDetails}
	 * @return {@code true} if the user is the comment's author
	 * @throws ResourceNotFoundException if the comment does not exist. TODO(verify): whether this reaches the client as 404
	 */
	public boolean canDelete(UUID commentId, Authentication auth) {
		Comment comment = this.commentRepository.findById(commentId)
				.orElseThrow(() ->
                		new ResourceNotFoundException(
                				"Comment with id: %s".formatted(commentId)
                		)
				);
		
		UserAccountDetails account = (UserAccountDetails) auth.getPrincipal();
		
		return account.getId().equals(comment.getAuthorId());
	}
	
}
