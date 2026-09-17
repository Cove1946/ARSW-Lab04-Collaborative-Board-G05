package edu.eci.arsw.collabboard.domain.model;


import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class BoardTest {
    @Test
    void shouldRejectNullElementWithDomainException() {
        List<BoardElement> elements = Arrays.asList((BoardElement) null);

        assertThrows(IllegalArgumentException.class,
                () -> new Board("board-1", "Architecture Session", elements));
    }
}
