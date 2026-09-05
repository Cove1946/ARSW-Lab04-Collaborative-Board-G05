package edu.eci.arsw.collabboard.application.service;

import edu.eci.arsw.collabboard.application.exception.BoardNotFoundException;
import edu.eci.arsw.collabboard.domain.model.Board;
import edu.eci.arsw.collabboard.domain.model.BoardElement;
import edu.eci.arsw.collabboard.domain.model.ElementType;
import edu.eci.arsw.collabboard.infrastructure.persistence.InMemoryBoardRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Use-case tests. The service is exercised without starting the web server,
 * using the in-memory adapter as the port implementation.
 */
class BoardApplicationServiceTest {

    private final BoardApplicationService service =
            new BoardApplicationService(new InMemoryBoardRepository());

    @Test
    void shouldCreateBoardWithServerGeneratedIdAndNoElements() {
        Board created = service.createBoard("Architecture Session");

        assertNotNull(created.id());
        assertTrue(!created.id().isBlank());
        assertEquals("Architecture Session", created.name());
        assertTrue(created.elements().isEmpty());
    }

    @Test
    void shouldCreateAndReadBoard() {
        Board created = service.createBoard("Architecture Session");
        Board loaded = service.getBoard(created.id());

        assertEquals(created, loaded);
    }

    @Test
    void shouldFailWithConcreteExceptionWhenBoardDoesNotExist() {
        assertThrows(BoardNotFoundException.class,
                () -> service.getBoard("missing-board"));
    }

    @Test
    void shouldReplaceFullStateKeepingIdentity() {
        Board created = service.createBoard("Draft");
        List<BoardElement> elements = List.of(
                new BoardElement("e1", ElementType.RECTANGLE, 0, 0, 100, 40, ""),
                new BoardElement("e2", ElementType.TEXT, 10, 10, 0, 0, "hello")
        );

        Board replaced = service.replaceBoard(created.id(), "Final", elements);

        assertEquals(created.id(), replaced.id());
        assertEquals("Final", replaced.name());
        assertEquals(2, replaced.elements().size());
        assertEquals(replaced, service.getBoard(created.id()));
    }

    @Test
    void shouldNotCreateWhenReplacingMissingBoard() {
        assertThrows(BoardNotFoundException.class,
                () -> service.replaceBoard("missing-board", "X", List.of()));
    }

    @Test
    void shouldRejectBlankNameOnCreate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createBoard("  "));
    }
}
