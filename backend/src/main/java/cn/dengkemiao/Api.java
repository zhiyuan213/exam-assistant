package cn.dengkemiao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class Api {
    final Store store; final Auth auth;
    public Api(Store store,Auth auth) {this.store=store;this.auth=auth;}
    public record Login(String code,boolean demo) {}
    public record CreatePlan(@NotBlank @Size(max=30) String examType,@NotBlank @Size(max=80) String examName,
        @NotNull LocalDate examDate,@Min(30) @Max(720) int dailyMinutes,@NotBlank String level,
        @NotEmpty @Size(max=8) List<Planner.Subject> subjects) {}
    public record Complete(boolean completed) {}
    public record Order(@NotBlank String planId,@NotBlank String productId) {}
    @GetMapping("/health") public Map<String,Object> health() {return Map.of("status","ok","date",Store.today());}
    @GetMapping("/config") public Map<String,Object> config() {return Map.of("demoLogin",auth.demo,"paymentEnabled",false,"templateNotice","通用学习路线，请按考试大纲与个人科目调整");}
    @PostMapping("/auth/login") public Map<String,Object> login(@RequestBody Login input) {return auth.login(input.code(),input.demo());}
    @GetMapping("/templates") public Object templates() {return store.templates();}
    @GetMapping("/plans") public Object plans(@RequestHeader(value="Authorization",required=false) String token) {return store.plans(auth.user(token));}
    @PostMapping("/plans") public Object create(@RequestHeader(value="Authorization",required=false) String token,@Valid @RequestBody CreatePlan input) {
        if(input.subjects().stream().anyMatch(s->s==null || s.name()==null || s.name().length()>80 || s.weight()>1000)) throw new IllegalArgumentException("科目名称最长80字，权重最大1000");
        return store.create(auth.user(token),input);
    }
    @GetMapping("/plans/{id}") public Object plan(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id) {return store.plan(id,auth.user(token));}
    @GetMapping("/plans/{id}/tasks") public Object tasks(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id,@RequestParam(required=false) LocalDate date) {return store.tasks(id,auth.user(token),date==null?Store.today():date);}
    @PatchMapping("/tasks/{id}") public Object complete(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id,@RequestBody Complete input) {store.complete(id,auth.user(token),input.completed());return Map.of("ok",true);}
    @GetMapping("/plans/{id}/records") public Object records(@RequestHeader(value="Authorization",required=false) String token,@PathVariable String id) {return store.records(id,auth.user(token));}
    @GetMapping("/products") public Object products() {return store.jdbc().query("SELECT * FROM product",(rs,n)->Map.of("id",rs.getString("id"),"name",rs.getString("name"),"priceFen",rs.getInt("price_fen"),"description",rs.getString("description")));}
    @PostMapping("/orders") public Object order(@RequestHeader(value="Authorization",required=false) String token,@Valid @RequestBody Order input) {
        String user=auth.user(token);store.own(input.planId(),user);
        // No fake payment, no client-side entitlement. Enable only after approved virtual-payment integration.
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"虚拟支付尚未开通，当前仅提供免费开发体验");
    }
}

