import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {

    Page<DiscussionPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM DiscussionPost p WHERE p.tags LIKE %:tag% ORDER BY p.createdAt DESC")
    Page<DiscussionPost> findByTag(String tag, Pageable pageable);

    @Query("SELECT p FROM DiscussionPost p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%')) ORDER BY p.createdAt DESC")
    Page<DiscussionPost> search(String q, Pageable pageable);

    Page<DiscussionPost> findAllByOrderByUpvotesDesc(Pageable pageable);

    void deleteByUserId(Long userId);

    // BE-06 fix: atomic increment — prevents race condition on concurrent replies
    @Modifying
    @Query("UPDATE DiscussionPost p SET p.replyCount = p.replyCount + 1 WHERE p.id = :id")
    void incrementReplyCount(Long id);

    // BE-06 fix: atomic upvote increment/decrement
    @Modifying
    @Query("UPDATE DiscussionPost p SET p.upvotes = GREATEST(0, p.upvotes + 1) WHERE p.id = :id")
    void incrementUpvotes(Long id);

    @Modifying
    @Query("UPDATE DiscussionPost p SET p.upvotes = GREATEST(0, p.upvotes - 1) WHERE p.id = :id")
    void decrementUpvotes(Long id);
}
