package cn.dengkemiao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.demo-login=true"})
@AutoConfigureMockMvc
class ApiTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;
    String token() throws Exception {var r=mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"demo\":true}")).andExpect(status().isOk()).andReturn();return "Bearer "+json.readTree(r.getResponse().getContentAsString()).get("token").asText();}
    @Test void completeFlowAndOwnership() throws Exception {
        String owner=token(),other=token();
        var body=Map.of("examType","civil","examName","公务员备考","examDate",Store.today().plusDays(180).toString(),"dailyMinutes",137,"level","normal","subjects",List.of(Map.of("name","行测","weight",60),Map.of("name","申论","weight",40)));
        var response=mvc.perform(post("/api/plans").header("Authorization",owner).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isOk()).andExpect(jsonPath("$.totalTasks").value(360)).andReturn();
        String id=json.readTree(response.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/plans/"+id).header("Authorization",other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/plans").header("Authorization",other)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/plans")).andExpect(status().isUnauthorized());
        var tasks=mvc.perform(get("/api/plans/"+id+"/tasks").header("Authorization",owner)).andExpect(status().isOk()).andReturn();
        String task=json.readTree(tasks.getResponse().getContentAsString()).get(0).get("id").asText();
        mvc.perform(patch("/api/tasks/"+task).header("Authorization",other).contentType("application/json").content("{\"completed\":true}")).andExpect(status().isNotFound());
        for(int i=0;i<2;i++)mvc.perform(patch("/api/tasks/"+task).header("Authorization",owner).contentType("application/json").content("{\"completed\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/plans/"+id+"/records").header("Authorization",owner)).andExpect(status().isOk()).andExpect(jsonPath("$.completedTasks").value(1)).andExpect(jsonPath("$.streak").value(1));
        mvc.perform(patch("/api/tasks/"+task).header("Authorization",owner).contentType("application/json").content("{\"completed\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/plans/"+id+"/records").header("Authorization",owner)).andExpect(jsonPath("$.completedTasks").value(0));
        mvc.perform(post("/api/orders").header("Authorization",owner).contentType("application/json").content(json.writeValueAsString(Map.of("planId",id,"productId","full-plan")))).andExpect(status().isServiceUnavailable());
    }
    @Test void rejectsBadInput() throws Exception {
        String owner=token();
        mvc.perform(post("/api/plans").header("Authorization",owner).contentType("application/json").content("{\"examType\":\"custom\",\"examName\":\"x\",\"examDate\":\"2000-01-01\",\"dailyMinutes\":240,\"level\":\"normal\",\"subjects\":[{\"name\":\"x\",\"weight\":100}]}" )).andExpect(status().isBadRequest());
        mvc.perform(post("/api/plans").header("Authorization",owner).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }
}
