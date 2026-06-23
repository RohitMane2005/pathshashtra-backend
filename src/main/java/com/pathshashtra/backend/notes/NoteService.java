package com.pathshashtra.backend.notes;

import com.pathshashtra.backend.common.HtmlSanitizer;
import com.pathshashtra.backend.exception.ForbiddenException;
import com.pathshashtra.backend.exception.NotFoundException;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepo;
    private final UserRepository userRepo;
    private final HtmlSanitizer htmlSanitizer;

    public NoteService(NoteRepository noteRepo, UserRepository userRepo, HtmlSanitizer htmlSanitizer) {
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
        this.htmlSanitizer = htmlSanitizer;
    }

    public List<Note> getNotes(String email, String category, String search) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (search != null && !search.isBlank()) {
            return noteRepo.search(user.getId(), search.trim());
        }
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            return noteRepo.findByUserIdAndCategory(user.getId(), category.toUpperCase());
        }
        return noteRepo.findByUserIdOrderByIsPinnedDescUpdatedAtDesc(user.getId());
    }

    /**
     * FIX C3: Accept NoteRequest DTO instead of Note entity.
     * Prevents mass assignment of userId, id, isPinned etc.
     */
    @Transactional
    public Note createNote(String email, NoteRequest request) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Note note = new Note();
        note.setUserId(user.getId());
        note.setTitle(htmlSanitizer.sanitize(request.getTitle(), 200));
        note.setContent(htmlSanitizer.sanitize(request.getContent(), 50000));
        note.setCategory(request.getCategory() != null ? request.getCategory() : "GENERAL");
        note.setTags(htmlSanitizer.sanitize(request.getTags(), 500));
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepo.save(note);
    }

    /** FIX C3: Accept NoteRequest DTO instead of Note entity. */
    @Transactional
    public Note updateNote(String email, Long noteId, NoteRequest request) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Note note = noteRepo.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));
        if (!note.getUserId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
        note.setTitle(htmlSanitizer.sanitize(request.getTitle(), 200));
        note.setContent(htmlSanitizer.sanitize(request.getContent(), 50000));
        note.setCategory(request.getCategory());
        note.setTags(htmlSanitizer.sanitize(request.getTags(), 500));
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepo.save(note);
    }

    @Transactional
    public void deleteNote(String email, Long noteId) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Note note = noteRepo.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));
        if (!note.getUserId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
        noteRepo.delete(note);
    }

    @Transactional
    public Note togglePin(String email, Long noteId) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Note note = noteRepo.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found"));
        if (!note.getUserId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
        note.setPinned(!note.isPinned());
        return noteRepo.save(note);
    }
}
