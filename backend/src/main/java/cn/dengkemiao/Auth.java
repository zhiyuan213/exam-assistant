package cn.dengkemiao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

@Service
public class Auth {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(Auth.class);
    final Store store;
    final com.fasterxml.jackson.databind.ObjectMapper json;
    @Value("${app.demo-login}") boolean demo;
    @Value("${app.wechat.appid}") String appid;
    @Value("${app.wechat.secret}") String secret;
    public Auth(Store store, com.fasterxml.jackson.databind.ObjectMapper json) {this.store=store;this.json=json;}
    static Map<?,?> exchange(RestClient client, com.fasterxml.jackson.databind.ObjectMapper json, String appid, String secret, String code) throws java.io.IOException {
        // WeChat can label a JSON response as text/plain; decode the payload explicitly.
        String body=client.get().uri(b->b.path("/sns/jscode2session").queryParam("appid",appid).queryParam("secret",secret).queryParam("js_code",code).queryParam("grant_type","authorization_code").build()).retrieve().body(String.class);
        if(body==null || body.isBlank()) throw new java.io.IOException("Empty WeChat response");
        return json.readValue(body,Map.class);
    }
    String hash(String token) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    public Map<String,Object> login(String code, boolean demoRequest) {
        String openid;
        if(demoRequest) {
            if(!demo) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"正式环境不支持体验登录");
            // Fresh isolated guest account; existing token in the client preserves its identity.
            openid="demo-"+UUID.randomUUID();
        } else {
            if(code==null || code.isBlank()) throw new IllegalArgumentException("微信登录code不能为空");
            if(secret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"尚未配置微信登录");
            Map<?,?> response;
            try {
                response=exchange(RestClient.create("https://api.weixin.qq.com"),json,appid,secret,code);
            } catch(Exception e) {
                Throwable cause=e;
                for(int i=0;i<8 && cause.getCause()!=null;i++) cause=cause.getCause();
                // Exception messages and request URLs can contain the AppSecret and login code.
                log.warn("WeChat login request failed: type={}, cause={}",e.getClass().getSimpleName(),cause.getClass().getSimpleName());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"微信登录服务暂不可用");
            }
            if(response==null || !(response.get("openid") instanceof String value) || value.isBlank()) {
                Object errcode=response==null?null:response.get("errcode");
                log.warn("WeChat login rejected: errcode={}",errcode instanceof Number?errcode:"unknown");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"微信登录失效，请重试");
            }
            openid=value;
        }
        var users=store.jdbc().queryForList("SELECT id,nickname FROM app_user WHERE openid=?",openid);
        String user;
        if(users.isEmpty()) {user=UUID.randomUUID().toString();store.jdbc().update("INSERT INTO app_user VALUES (?,?,?,?)",user,openid,"登科喵同学",LocalDateTime.now());}
        else user=(String)users.get(0).get("id");
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        store.jdbc().update("DELETE FROM auth_session WHERE expires_at<?",LocalDateTime.now());
        store.jdbc().update("INSERT INTO auth_session VALUES (?,?,?)",hash(token),user,LocalDateTime.now().plusDays(7));
        return Map.of("token",token,"user",Map.of("id",user,"nickname","登科喵同学"),"demo",demoRequest);
    }
    public String user(String header) {
        if(header==null || !header.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录");
        var users=store.jdbc().queryForList("SELECT user_id FROM auth_session WHERE token_hash=? AND expires_at>?",hash(header.substring(7)),LocalDateTime.now());
        if(users.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"登录已过期");
        return (String)users.get(0).get("user_id");
    }
}

