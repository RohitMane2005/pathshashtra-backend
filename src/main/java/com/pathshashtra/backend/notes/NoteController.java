package com.pathshashtra.backend.notes;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FIX C3: Uses NoteRequest DTO instead of binding directly to the Note entity.
 */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Note>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Authentication auth) {
        return ResponseEntity.ok(service.getNotes(auth.getName(), category, search));
    }

    @PostMapping
    public ResponseEntity<Note> create(@Valid @RequestBody NoteRequest request, Authentication auth) {
        return ResponseEntity.ok(service.createNote(auth.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Note> update(@PathVariable Long id, @Valid @RequestBody NoteRequest request, Authentication auth) {
        return ResponseEntity.ok(service.updateNote(auth.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id, Authentication auth) {
        service.deleteNote(auth.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Note deleted"));
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<Note> togglePin(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.togglePin(auth.getName(), id));
    }
}

