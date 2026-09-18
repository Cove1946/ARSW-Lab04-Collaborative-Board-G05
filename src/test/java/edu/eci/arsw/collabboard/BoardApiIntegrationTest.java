package edu.eci.arsw.collabboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end REST coverage with the real service and in-memory repository. */
@SpringBootTest
@AutoConfigureMockMvc
class BoardApiIntegrationTest {
    private static final String CONNECTED_BOARD = """
            {"name":"Connected","elements":[
              {"id":"a","type":"RECTANGLE","x":10,"y":10,"width":170,"height":70,"text":"API"},
              {"id":"b","type":"TEXT","x":300,"y":10,"width":160,"height":32,"text":"Notes"},
              {"id":"c","type":"CONNECTOR","sourceId":"a","targetId":"b"}
            ]}""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void savedConnectorSurvivesReload() throws Exception {
        String location = createBoard();
        mockMvc.perform(put(location).contentType(MediaType.APPLICATION_JSON).content(CONNECTED_BOARD))
                .andExpect(status().isOk());
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Connected"))
                .andExpect(jsonPath("$.elements.length()").value(3))
                .andExpect(jsonPath("$.elements[2].type").value("CONNECTOR"))
                .andExpect(jsonPath("$.elements[2].sourceId").value("a"))
                .andExpect(jsonPath("$.elements[2].targetId").value("b"));
    }

    @Test
    void rejectedConnectorLeavesStoredBoardUnchanged() throws Exception {
        String location = createBoard();
        mockMvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Broken\",\"elements\":[{\"id\":\"c\",\"type\":\"CONNECTOR\",\"sourceId\":\"a\",\"targetId\":\"b\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("Connector c references a missing element: a"));
        mockMvc.perform(get(location))
                .andExpect(jsonPath("$.name").value("Integration"))
                .andExpect(jsonPath("$.elements.length()").value(0));
    }

    @Test
    void webClientIsServedByTheSameApplication() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("boardCanvas")));
        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/javascript"));
    }

    private String createBoard() throws Exception {
        return mockMvc.perform(post("/api/boards").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Integration\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }
}
