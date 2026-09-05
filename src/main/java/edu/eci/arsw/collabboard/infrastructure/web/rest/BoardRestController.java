package edu.eci.arsw.collabboard.infrastructure.web.rest;

import edu.eci.arsw.collabboard.application.service.BoardApplicationService;
import edu.eci.arsw.collabboard.domain.model.Board;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Thin HTTP adapter: translates requests into use-case calls and shapes the
 * response. Holds no state, contains no business rule and never touches the
 * persistence store directly.
 */
@RestController
@RequestMapping("/api/boards")
public class BoardRestController {

    private final BoardApplicationService service;

    public BoardRestController(BoardApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Board> create(@Valid @RequestBody CreateBoardRequest request) {
        Board created = service.createBoard(request.name());
        return ResponseEntity
                .created(URI.create("/api/boards/" + created.id()))
                .body(created);
    }

    @GetMapping("/{boardId}")
    public Board get(@PathVariable String boardId) {
        return service.getBoard(boardId);
    }

    @PutMapping("/{boardId}")
    public Board replace(@PathVariable String boardId,
                         @Valid @RequestBody ReplaceBoardRequest request) {
        return service.replaceBoard(boardId, request.name(), request.elements());
    }
}
