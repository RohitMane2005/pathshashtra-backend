package com.pathshashtra.backend.discussion;

import com.pathshashtra.backend.common.HtmlSanitizer;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DiscussionService {

    private final DiscussionPostRepository postRepo;
    private final DiscussionReplyRepository replyRepo;
    private final DiscussionVoteRepository voteRepo;
    private final UserRepository userRepo;
    private final HtmlSanitizer htmlSanitizer;

    public DiscussionService(DiscussionPostRepository postRepo,
                             DiscussionReplyRepository replyRepo,
                             DiscussionVoteRepository voteRepo,
                             UserRepository userRepo,
                             HtmlSanitizer htmlSanitizer) {
        this.postRepo = postRepo;
        this.replyRepo = replyRepo;
        this.voteRepo = voteRepo;
        this.userRepo = userRepo;
        this.htmlSanitizer = htmlSanitizer;
    }

    public Page<DiscussionPost> listPosts(String tag, String search, String sort, int page) {
        Pageable pageable = PageRequest.of(page, 20);
        if (search != null && !search.isBlank()) {
            return postRepo.search(search.trim(), pageable);
        }
        if (tag != null && !tag.isBlank()) {
            return postRepo.findByTag(tag.trim(), pageable);
        }
        if ("top".equals(sort)) {
            return postRepo.findAllByOrderByUpvotesDesc(pageable);
        }
        return postRepo.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Map<String, Object> getPost(Long id) {
        DiscussionPost post = postRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        List<DiscussionReply> replies = replyRepo.findByPostIdOrderByCreatedAtAsc(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("post", post);
        result.put("replies", replies);
        return result;
    }

    @Transactional
    public DiscussionPost createPost(String email, String title, String content, String tags) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DiscussionPost post = new DiscussionPost();
        post.setUserId(user.getId());
        post.setAuthorName(user.getName());
        post.setTitle(htmlSanitizer.sanitize(title.trim(), 200));
        post.setContent(htmlSanitizer.sanitize(content.trim(), 10000));
        post.setTags(tags != null ? tags.trim().toLowerCase() : "");
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return postRepo.save(post);
    }

    @Transactional
    public DiscussionReply addReply(String email, Long postId, String content) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DiscussionPost post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        DiscussionReply reply = new DiscussionReply();
        reply.setPostId(postId);
        reply.setUserId(user.getId());
        reply.setAuthorName(user.getName());
        reply.setContent(htmlSanitizer.sanitize(content.trim(), 10000));
        reply.setCreatedAt(LocalDateTime.now());
        DiscussionReply saved = replyRepo.save(reply);

        // BE-06 fix: atomic increment — no race condition with concurrent replies
        postRepo.incrementReplyCount(postId);
        return saved;
    }

    @Transactional
    public Map<String, Object> toggleVote(String email, String targetType, Long targetId) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<DiscussionVote> existing = voteRepo.findByUserIdAndTargetTypeAndTargetId(
                user.getId(), targetType, targetId);

        boolean voted;
        if (existing.isPresent()) {
            voteRepo.delete(existing.get());
            updateVoteCount(targetType, targetId, -1);
            voted = false;
        } else {
            DiscussionVote vote = new DiscussionVote();
            vote.setUserId(user.getId());
            vote.setTargetType(targetType);
            vote.setTargetId(targetId);
            voteRepo.save(vote);
            updateVoteCount(targetType, targetId, 1);
            voted = true;
        }
        return Map.of("voted", voted);
    }

    /**
     * HIGH-03 FIX: Use atomic SQL updates instead of read-modify-write.
     * OLD: findById → setUpvotes(current + delta) → save — race condition.
     * NEW: UPDATE SET upvotes = GREATEST(0, upvotes ± 1) — atomic, no lost updates.
     */
    private void updateVoteCount(String type, Long id, int delta) {
        if ("POST".equals(type)) {
            if (delta > 0) postRepo.incrementUpvotes(id);
            else postRepo.decrementUpvotes(id);
        } else {
            if (delta > 0) replyRepo.incrementUpvotes(id);
            else replyRepo.decrementUpvotes(id);
        }
    }
}
