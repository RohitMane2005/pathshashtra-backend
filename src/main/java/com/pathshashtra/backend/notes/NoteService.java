package com.pathshashtra.backend.notes;

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

    public NoteService(NoteRepository noteRepo, UserRepository userRepo) {
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
    }

    public List<Note> getNotes(String email, String category, String search) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (search != null && !search.isBlank()) {
            return noteRepo.search(user.getId(), search.trim());
        }
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            return noteRepo.findByUserIdAndCategory(user.getId(), category.toUpperCase());
        }
        return noteRepo.findByUserIdOrderByIsPinnedDescUpdatedAtDesc(user.getId());
    }

    @Transactional
    public Note createNote(String email, Note note) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        note.setUserId(user.getId());
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepo.save(note);
    }

    @Transactional
    public Note updateNote(String email, Long noteId, Note updates) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Note note = noteRepo.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        if (!note.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        note.setTitle(updates.getTitle());
        note.setContent(updates.getContent());
        note.setCategory(updates.getCategory());
        note.setTags(updates.getTags());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepo.save(note);
    }

    @Transactional
    public void deleteNote(String email, Long noteId) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Note note = noteRepo.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        if (!note.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        noteRepo.delete(note);
    }

    @Transactional
    public Note togglePin(String email, Long noteId) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Note note = noteRepo.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        if (!note.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        note.setPinned(!note.isPinned());
        return noteRepo.save(note);
    }
}
