package edu.eci.arsw.collabboard.domain.model;

public record BoardElement(
        String id,
        ElementType type,
        double x,
        double y,
        double width,
        double height,
        String text
) {
    public BoardElement {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Element id is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Element type is required");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Element dimensions cannot be negative");
        }
        text = text == null ? "" : text;
    }
}
