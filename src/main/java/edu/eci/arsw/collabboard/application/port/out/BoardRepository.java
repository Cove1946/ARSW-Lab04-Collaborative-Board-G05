package edu.eci.arsw.collabboard.application.port.out;

import java.util.Optional;

import edu.eci.arsw.collabboard.domain.model.Board;

/**
 * Output port owned by the application boundary.
 *
 * Only the operations the use cases actually need are declared here. This is a
 * domain-shaped contract, not a copy of a database framework: no query language,
 * pagination, transactions or vendor types leak through.
 *
 * <ul>
 *   <li>{@link #save(Board)} — insert a new board or replace the existing one (upsert).</li>
 *   <li>{@link #findById(String)} — load a board for the {@code get} use case.</li>
 *   <li>{@link #existsById(String)} — let {@code replace} reject a missing board
 *       without loading the whole aggregate.</li>
 * </ul>
 */

public interface BoardRepository {

    /** Inserts a new board or replaces the existing one with the same id. Returns the stored board. */
    Board save(Board board);

    /** Returns the board with the given id, or empty if none exists. */
    Optional<Board> findById(String boardId);

    /** Returns {@code true} if a board with the given id is stored. */
    boolean existsById(String boardId);
}
