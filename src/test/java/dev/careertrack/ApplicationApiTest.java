package dev.careertrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:tests;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class ApplicationApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    private static final String PAYLOAD = """
        {"company":" Northstar Labs ","role":"Java Developer","location":"Houston",
         "stage":"APPLIED","appliedOn":"2025-01-01","interviewOn":null,"notes":"Prepare SQL examples"}
        """;
    @BeforeEach void clear() { jdbc.update("DELETE FROM applications"); }
    private JsonNode create() throws Exception {
        var result=mvc.perform(post("/api/applications").header("X-Requested-With","CareerTrack")
            .contentType("application/json").content(PAYLOAD))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.company").value("Northstar Labs")).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }
    @Test void completeLifecycle() throws Exception {
        var app=create(); String id=app.get("id").asText();
        mvc.perform(get("/api/applications")).andExpect(jsonPath("$",hasSize(1)));
        String update=PAYLOAD.replace("APPLIED","INTERVIEW").replace("\"interviewOn\":null","\"interviewOn\":\"2025-01-10\"").replace("{","{\"version\":0,");
        mvc.perform(put("/api/applications/"+id).header("X-Requested-With","CareerTrack").contentType("application/json").content(update))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.stage").value("INTERVIEW"));
        mvc.perform(get("/api/applications/"+id)).andExpect(jsonPath("$.interviewOn").value("2025-01-10"));
        mvc.perform(delete("/api/applications/"+id+"?version=1").header("X-Requested-With","CareerTrack")).andExpect(status().isNoContent());
        mvc.perform(get("/api/applications/"+id)).andExpect(status().isNotFound());
    }
    @Test void rejectsInvalidInputs() throws Exception {
        for(String body : new String[]{PAYLOAD.replace(" Northstar Labs "," "),PAYLOAD.replace("APPLIED","INVALID"),PAYLOAD.replace("2025-01-01","2999-01-01"),PAYLOAD.replace("2025-01-01","not-a-date"),"{}"}) {
            mvc.perform(post("/api/applications").header("X-Requested-With","CareerTrack").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM applications",Integer.class));
    }
    @Test void rejectsInterviewBeforeApplication() throws Exception {
        mvc.perform(post("/api/applications").header("X-Requested-With","CareerTrack").contentType("application/json")
            .content(PAYLOAD.replace("\"interviewOn\":null","\"interviewOn\":\"2024-01-01\""))).andExpect(status().isBadRequest());
    }
    @Test void staleEditsAndDeletesCannotOverwriteNewerData() throws Exception {
        String id=create().get("id").asText(); String update=PAYLOAD.replace("{","{\"version\":0,");
        for(int i=0;i<2;i++) mvc.perform(put("/api/applications/"+id).header("X-Requested-With","CareerTrack").contentType("application/json").content(update)).andExpect(status().is(i==0?200:409));
        mvc.perform(delete("/api/applications/"+id+"?version=0").header("X-Requested-With","CareerTrack")).andExpect(status().isConflict());
        mvc.perform(get("/api/applications/"+id)).andExpect(jsonPath("$.version").value(1));
    }
    @Test void missingVersionAndUnknownIdsAreHandled() throws Exception {
        String id=create().get("id").asText();
        mvc.perform(put("/api/applications/"+id).header("X-Requested-With","CareerTrack").contentType("application/json").content(PAYLOAD)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/applications/"+UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/applications/bad-id")).andExpect(status().isBadRequest());
    }
    @Test void exportsCsvWithAttachmentHeader() throws Exception {
        create();mvc.perform(get("/api/applications/export")).andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",containsString("careertrack-applications.csv")))
            .andExpect(content().string(containsString("\"Northstar Labs\",\"Java Developer\"")));
    }
    @Test void csvEscapesQuotesNewlinesAndSpreadsheetFormulas() {
        assertEquals("\"a,\"\"b\"\"\nc\"",ApplicationService.cell("a,\"b\"\nc"));
        assertEquals("\"'=1+1\"",ApplicationService.cell("=1+1"));
        assertEquals("\"'  @SUM(1)\"",ApplicationService.cell("  @SUM(1)"));
        assertEquals("\"\"",ApplicationService.cell(null));
    }
    @Test void browserWriteGuardAndSecurityHeaders() throws Exception {
        mvc.perform(post("/api/applications").contentType("application/json").content(PAYLOAD)).andExpect(status().isForbidden());
        mvc.perform(get("/api/applications").header("Sec-Fetch-Site","cross-site")).andExpect(status().isForbidden());
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options","nosniff"));
    }
    @Test void acceptsOptionalFieldsAndKeepsNotesAsPlainText() throws Exception {
        mvc.perform(post("/api/applications").header("X-Requested-With","CareerTrack").contentType("application/json")
          .content("{\"company\":\"Example\",\"role\":\"Developer\",\"stage\":\"SAVED\",\"notes\":\"<script>alert(1)</script>\"}"))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.location").value(""))
          .andExpect(jsonPath("$.notes").value("<script>alert(1)</script>"));
    }
}
