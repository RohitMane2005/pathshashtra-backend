package com.pathshashtra.backend.notes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<Note> create(@RequestBody Note note, Authentication auth) {
        return ResponseEntity.ok(service.createNote(auth.getName(), note));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Note> update(@PathVariable Long id, @RequestBody Note note, Authentication auth) {
        return ResponseEntity.ok(service.updateNote(auth.getName(), id, note));
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
