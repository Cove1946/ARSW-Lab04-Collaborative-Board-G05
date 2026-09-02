package edu.eci.arsw.collabboard.application.service;

import edu.eci.arsw.collabboard.application.port.out.BoardRepository;
import edu.eci.arsw.collabboard.domain.model.Board;
import edu.eci.arsw.collabboard.domain.model.BoardElement;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardApplicationService {

    private final BoardRepository repository;

    public BoardApplicationService(BoardRepository repository) {
        this.repository = repository;
    }

    public Board createBoard(String name) {
        // TODO LAB-04: generate the id, enforce the use-case rules and persist through the port.
        throw new UnsupportedOperationException("TODO LAB-04: createBoard");
    }

    public Board getBoard(String boardId) {
        // TODO LAB-04: use a concrete application exception when the board does not exist.
        throw new UnsupportedOperationException("TODO LAB-04: getBoard");
    }

    public Board replaceBoard(String boardId, String name, List<BoardElement> elements) {
        // TODO LAB-04: keep the existing identity and replace only a board that already exists.
        throw new UnsupportedOperationException("TODO LAB-04: replaceBoard");
    }
}
