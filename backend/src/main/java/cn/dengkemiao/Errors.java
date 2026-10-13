package cn.dengkemiao;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class Errors {
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> invalid(IllegalArgumentException e) {return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<?> validation(MethodArgumentNotValidException e) {return ResponseEntity.badRequest().body(Map.of("message","请检查考试名称、日期、科目和每日学习时间"));}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> status(ResponseStatusException e) {return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",ObjectsSafe.reason(e)));}
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class) public ResponseEntity<?> parse(Exception e) {return ResponseEntity.badRequest().body(Map.of("message","请求格式无效"));}
    private static class ObjectsSafe {static String reason(ResponseStatusException e) {return e.getReason()==null?"请求失败":e.getReason();}}
}
