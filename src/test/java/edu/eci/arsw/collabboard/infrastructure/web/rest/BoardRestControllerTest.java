package edu.eci.arsw.collabboard.infrastructure.web.rest;

import edu.eci.arsw.collabboard.application.exception.BoardNotFoundException;
import edu.eci.arsw.collabboard.application.service.BoardApplicationService;
import edu.eci.arsw.collabboard.domain.model.Board;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST contract tests. Only the web layer is loaded; the application service is mocked.
 */
@WebMvcTest(BoardRestController.class)
class BoardRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardApplicationService service;

    @Test
    void postCreatesBoardAndReturns201WithLocation() throws Exception {
        when(service.createBoard("Architecture Session"))
                .thenReturn(new Board("board-1", "Architecture Session", List.of()));

        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Architecture Session\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/boards/board-1"))
                .andExpect(jsonPath("$.id").value("board-1"))
                .andExpect(jsonPath("$.name").value("Architecture Session"));
    }

    @Test
    void postWithBlankNameReturns400ApiError() throws Exception {
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/boards"));
    }

    @Test
    void getExistingBoardReturns200() throws Exception {
        when(service.getBoard("board-1"))
                .thenReturn(new Board("board-1", "Architecture Session", List.of()));

        mockMvc.perform(get("/api/boards/board-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("board-1"));
    }

    @Test
    void getMissingBoardReturns404UniformApiError() throws Exception {
        when(service.getBoard("missing"))
                .thenThrow(new BoardNotFoundException("missing"));

        mockMvc.perform(get("/api/boards/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/boards/missing"));
    }

    @Test
    void putReplacesExistingBoardReturns200() throws Exception {
        when(service.replaceBoard(eq("board-1"), eq("Final"), any()))
                .thenReturn(new Board("board-1", "Final", List.of()));

        mockMvc.perform(put("/api/boards/board-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Final\",\"elements\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Final"));
    }

    @Test
    void putOnMissingBoardReturns404() throws Exception {
        when(service.replaceBoard(eq("missing"), any(), any()))
                .thenThrow(new BoardNotFoundException("missing"));

        mockMvc.perform(put("/api/boards/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Final\",\"elements\":[]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_NOT_FOUND"));
    }
    @Test
    void putWithSeveralInvalidFieldsReportsAllOfThemInStableOrder() throws Exception {
        mockMvc.perform(put("/api/boards/board-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("elements: elements are required; name: name is required"));
    }

    @Test
    void unknownRouteReturns404ApiError() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/unknown"));
    }

    @Test
    void unsupportedMethodReturns405ApiError() throws Exception {
        mockMvc.perform(delete("/api/boards/board-1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedMediaTypeReturns415ApiError() throws Exception {
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Architecture Session"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void putWithNullElementReturns400ApiError() throws Exception {
        mockMvc.perform(put("/api/boards/board-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Final\",\"elements\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void putWithInvalidElementReportsTheBrokenDomainRule() throws Exception {
        mockMvc.perform(put("/api/boards/board-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Final\",\"elements\":[{\"id\":\"e1\",\"type\":\"RECTANGLE\",\"width\":-1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("Element dimensions cannot be negative"));
    }

    @Test
    void putWithUnknownElementTypeIsStillMalformed() throws Exception {
        mockMvc.perform(put("/api/boards/board-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Final\",\"elements\":[{\"id\":\"e1\",\"type\":\"CIRCLE\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void unexpectedFailureReturns500WithoutInternalDetails() throws Exception {
        when(service.getBoard("board-1")).thenThrow(new IllegalStateException("internal detail"));

        mockMvc.perform(get("/api/boards/board-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}
