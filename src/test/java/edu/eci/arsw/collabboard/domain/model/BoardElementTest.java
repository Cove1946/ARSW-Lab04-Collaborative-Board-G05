package edu.eci.arsw.collabboard.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoardElementTest {
    @Test
    void connectorRequiresSourceAndTarget() {
        assertThrows(IllegalArgumentException.class, () -> BoardElement.connector("c1", null, "b"));
        assertThrows(IllegalArgumentException.class, () -> BoardElement.connector("c1", "a", " "));
    }

    @Test
    void connectorMustJoinTwoDifferentElements() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> BoardElement.connector("c1", "a", "a"));
        assertEquals("Connector c1 must join two different elements", error.getMessage());
    }

    @Test
    void shapesCannotReferenceOtherElements() {
        assertThrows(IllegalArgumentException.class, () -> new BoardElement("r1", ElementType.RECTANGLE, 0, 0, 10, 10, "", "a", null));
        assertThrows(IllegalArgumentException.class, () -> new BoardElement("t1", ElementType.TEXT, 0, 0, 10, 10, "hi", null, "b"));
    }

    @Test
    void lab04ConstructorBuildsShapesWithoutReferences() {
        BoardElement text = new BoardElement("t1", ElementType.TEXT, 5, 5, 0, 0, null);
        assertNull(text.sourceId());
        assertNull(text.targetId());
        assertEquals("", text.text());
    }
}
