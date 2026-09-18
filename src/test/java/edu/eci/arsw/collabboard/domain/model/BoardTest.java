package edu.eci.arsw.collabboard.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Aggregate invariants of {@link Board}. */
class BoardTest {
    private static final BoardElement API = new BoardElement("api", ElementType.RECTANGLE, 0, 0, 170, 70, "API");
    private static final BoardElement NOTE = new BoardElement("note", ElementType.TEXT, 300, 0, 160, 32, "Note");

    @Test
    void shouldRejectNullElementWithDomainException() {
        List<BoardElement> elements = Arrays.asList((BoardElement) null);
        assertThrows(IllegalArgumentException.class, () -> new Board("board-1", "Architecture Session", elements));
    }

    @Test
    void shouldAcceptConnectorBetweenExistingElements() {
        Board board = new Board("board-1", "Demo", List.of(API, NOTE, BoardElement.connector("c1", "api", "note")));
        assertEquals(3, board.elements().size());
    }

    @Test
    void shouldAcceptConnectorListedBeforeItsEndpoints() {
        Board board = new Board("board-1", "Demo", List.of(BoardElement.connector("c1", "api", "note"), API, NOTE));
        assertEquals(3, board.elements().size());
    }

    @Test
    void shouldRejectConnectorToMissingElement() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new Board("board-1", "Demo", List.of(API, BoardElement.connector("c1", "api", "ghost"))));
        assertEquals("Connector c1 references a missing element: ghost", error.getMessage());
    }

    @Test
    void shouldAcceptConnectorWhoseEndpointIsAnotherExistingConnector() {
        Board board = new Board("board-1", "Demo", List.of(API, NOTE,
                BoardElement.connector("c1", "api", "note"), BoardElement.connector("c2", "api", "c1")));
        assertEquals(4, board.elements().size());
    }
}
