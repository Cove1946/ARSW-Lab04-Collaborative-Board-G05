package edu.eci.arsw.collabboard.domain.model;

/**
 * A single visual element on a {@link Board}.
 *
 * Minimum domain invariants (Lab 04):
 * - {@code id} non-blank, {@code type} required and among the supported {@link ElementType} values.
 * - non-negative {@code width} / {@code height}.
 * - {@code text} defaults to empty string (used by TEXT elements).
 */

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
