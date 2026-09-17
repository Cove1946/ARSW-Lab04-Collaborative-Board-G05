package edu.eci.arsw.collabboard.domain.model;

/**
 * A single visual element on a {@link Board}.
 *
 * Minimum domain invariants:
 * - {@code id} non-blank, {@code type} required and among the supported {@link ElementType} values.
 * - non-negative {@code width} / {@code height}.
 * - {@code text} defaults to empty string (used by TEXT elements).
 * - Lab 05: a CONNECTOR requires two different, non-blank ids in {@code sourceId} and
 *   {@code targetId}; RECTANGLE and TEXT never reference other elements (both stay {@code null}).
 *
 * Whether the referenced elements exist is an aggregate rule, checked by {@link Board}.
 */
public record BoardElement(
        String id,
        ElementType type,
        double x,
        double y,
        double width,
        double height,
        String text,
        String sourceId,
        String targetId
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
        if (type == ElementType.CONNECTOR) {
            if (isBlank(sourceId) || isBlank(targetId)) {
                throw new IllegalArgumentException("Connector " + id + " requires sourceId and targetId");
            }
            if (sourceId.equals(targetId)) {
                throw new IllegalArgumentException("Connector " + id + " must join two different elements");
            }
        } else if (sourceId != null || targetId != null) {
            throw new IllegalArgumentException("Only CONNECTOR elements can have sourceId/targetId: " + id);
        }
    }

    /** Lab 04 shape: RECTANGLE and TEXT elements do not reference other elements. */
    public BoardElement(String id, ElementType type, double x, double y, double width, double height, String text) {
        this(id, type, x, y, width, height, text, null, null);
    }

    /** A connector between two elements; its geometry comes from them when it is drawn. */
    public static BoardElement connector(String id, String sourceId, String targetId) {
        return new BoardElement(id, ElementType.CONNECTOR, 0, 0, 0, 0, "", sourceId, targetId);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}