package cn.dengkemiao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeChatResponseTest {
    private java.util.Map<?,?> response(String body, MediaType type) throws Exception {
        var builder=RestClient.builder().baseUrl("https://example.test");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://example.test/sns/jscode2session?appid=test-app&secret=test-secret&js_code=test-code&grant_type=authorization_code"))
            .andRespond(withSuccess(body,type));
        var result=Auth.exchange(builder.build(),new ObjectMapper(),"test-app","test-secret","test-code");
        server.verify();
        return result;
    }
    @Test void acceptsJsonWithTextContentType() throws Exception {
        for(var type:java.util.List.of(MediaType.TEXT_PLAIN,MediaType.APPLICATION_JSON,MediaType.APPLICATION_OCTET_STREAM))
            assertEquals("test-user",response("{\"openid\":\"test-user\"}",type).get("openid"));
    }
    @Test void preservesNumericWechatError() throws Exception {
        assertEquals(40029,response("{\"errcode\":40029,\"errmsg\":\"invalid code\"}",MediaType.TEXT_PLAIN).get("errcode"));
    }
    @Test void rejectsHtmlRatherThanTreatingItAsLogin() {
        assertThrows(java.io.IOException.class,()->response("<html>gateway error</html>",MediaType.TEXT_HTML));
    }
}
