package cn.dengkemiao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
public class Store implements ApplicationRunner {
    final JdbcTemplate db;
    final ObjectMapper json;
    public Store(JdbcTemplate db, ObjectMapper json) { this.db=db; this.json=json; }
    public JdbcTemplate jdbc() { return db; }
    public static LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Shanghai")); }
    String encode(Object value) { try {return json.writeValueAsString(value);} catch(Exception e) {throw new IllegalStateException(e);} }
    List<Planner.Subject> decode(String value) {try {return json.readValue(value,new TypeReference<>(){});} catch(Exception e) {throw new IllegalStateException(e);} }
    @Override public void run(ApplicationArguments args) {
        seed("postgraduate", "考研", List.of(new Planner.Subject("数学",40),new Planner.Subject("英语",20),new Planner.Subject("政治",15),new Planner.Subject("专业课",25)));
        seed("civil", "公务员", List.of(new Planner.Subject("行测",60),new Planner.Subject("申论",40)));
        seed("teacher", "教师资格证", List.of(new Planner.Subject("综合素质",35),new Planner.Subject("教育知识与能力",35),new Planner.Subject("学科知识",30)));
        seed("english", "四六级", List.of(new Planner.Subject("词汇",25),new Planner.Subject("听力",25),new Planner.Subject("阅读",30),new Planner.Subject("写作与翻译",20)));
        seed("custom", "自定义考试", List.of(new Planner.Subject("自定义科目",100)));
        if (db.queryForObject("SELECT COUNT(*) FROM product WHERE id='full-plan'",Integer.class)==0)
            db.update("INSERT INTO product VALUES (?,?,?,?)","full-plan","完整备考方案",990,"完整阶段规划、每日任务与学习路线");
    }
    void seed(String code,String name,List<Planner.Subject> subjects) {
        if (db.queryForObject("SELECT COUNT(*) FROM exam_template WHERE code=?",Integer.class,code)==0)
            db.update("INSERT INTO exam_template VALUES (?,?,?,?)",code,name,"1.0",encode(subjects));
    }
    public List<Map<String,Object>> templates() {
        return db.query("SELECT * FROM exam_template ORDER BY CASE code WHEN 'postgraduate' THEN 1 WHEN 'civil' THEN 2 WHEN 'teacher' THEN 3 WHEN 'english' THEN 4 ELSE 5 END",(rs,n)->Map.of("code",rs.getString("code"),"name",rs.getString("name"),"version",rs.getString("version"),"subjects",decode(rs.getString("subjects_json"))));
    }
    void own(String planId,String user) {
        if (db.queryForObject("SELECT COUNT(*) FROM study_plan WHERE id=? AND user_id=?",Integer.class,planId,user)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"计划不存在");
    }
    @Transactional public Map<String,Object> create(String user,Api.CreatePlan input) {
        var matches=db.queryForList("SELECT code FROM exam_template WHERE code=?",input.examType());
        if(matches.isEmpty()) throw new IllegalArgumentException("考试类型无效");
        String id=UUID.randomUUID().toString();
        var subjects=input.subjects().stream().map(s->new Planner.Subject(s.name().trim(),s.weight())).toList();
        var result=Planner.generate(today(),input.examDate(),input.dailyMinutes(),input.level(),subjects);
        db.update("INSERT INTO study_plan VALUES (?,?,?,?,?,?,?,?,?,?)",id,user,input.examType(),input.examName().trim(),input.examDate(),today(),input.dailyMinutes(),input.level(),encode(subjects),LocalDateTime.now());
        for(var s:result.stages()) db.update("INSERT INTO study_plan_stage VALUES (?,?,?,?,?,?,?)",s.id(),id,s.name(),s.goal(),s.start(),s.end(),s.sort());
        db.batchUpdate("INSERT INTO study_task (id,plan_id,stage_id,task_date,subject,task_name,duration,completed) VALUES (?,?,?,?,?,?,?,FALSE)",result.tasks(),200,(ps,t)->{
            ps.setString(1,t.id()); ps.setString(2,id); ps.setString(3,t.stageId());ps.setObject(4,t.date());ps.setString(5,t.subject());ps.setString(6,t.name());ps.setInt(7,t.duration());
        });
        return plan(id,user);
    }
    Map<String,Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
        var m=new LinkedHashMap<String,Object>();
        String id=rs.getString("id");
        m.put("id",id);m.put("examType",rs.getString("exam_type"));m.put("examName",rs.getString("exam_name"));
        m.put("examDate",rs.getDate("exam_date").toLocalDate());m.put("startDate",rs.getDate("start_date").toLocalDate());
        m.put("remainingDays",Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(today(),rs.getDate("exam_date").toLocalDate())));
        m.put("dailyMinutes",rs.getInt("daily_minutes"));m.put("level",rs.getString("foundation"));m.put("subjects",decode(rs.getString("subjects_json")));
        var counts=db.queryForMap("SELECT COUNT(*) total, SUM(CASE WHEN completed THEN 1 ELSE 0 END) done FROM study_task WHERE plan_id=?",id);
        int total=((Number)counts.get("total")).intValue(); int done=counts.get("done")==null?0:((Number)counts.get("done")).intValue();
        m.put("totalTasks",total);m.put("completedTasks",done);m.put("progress",total==0?0:Math.round(done*100.0/total));
        return m;
    }
    public List<Map<String,Object>> plans(String user) {return db.query("SELECT * FROM study_plan WHERE user_id=? ORDER BY created_at DESC",(rs,n)->row(rs),user);}
    public Map<String,Object> plan(String id,String user) {
        own(id,user);
        var plan=db.query("SELECT * FROM study_plan WHERE id=?",(rs,n)->row(rs),id).get(0);
        var stages=db.query("SELECT * FROM study_plan_stage WHERE plan_id=? ORDER BY sort_order",(rs,n)->{
            String sid=rs.getString("id");
            var count=db.queryForMap("SELECT COUNT(*) total, SUM(CASE WHEN completed THEN 1 ELSE 0 END) done FROM study_task WHERE stage_id=?",sid);
            int total=((Number)count.get("total")).intValue();int done=count.get("done")==null?0:((Number)count.get("done")).intValue();
            return Map.of("id",sid,"name",rs.getString("stage_name"),"goal",rs.getString("goal"),"startDate",rs.getDate("start_date").toLocalDate(),"endDate",rs.getDate("end_date").toLocalDate(),"progress",total==0?0:Math.round(done*100.0/total));
        },id);
        plan.put("stages",stages); return plan;
    }
    public List<Map<String,Object>> tasks(String id,String user,LocalDate date) {
        own(id,user);
        return db.query("SELECT * FROM study_task WHERE plan_id=? AND task_date=? ORDER BY subject",(rs,n)->Map.of("id",rs.getString("id"),"subject",rs.getString("subject"),"name",rs.getString("task_name"),"duration",rs.getInt("duration"),"completed",rs.getBoolean("completed"),"date",rs.getDate("task_date").toLocalDate()),id,date);
    }
    @Transactional public void complete(String id,String user,boolean completed) {
        int updated=db.update("UPDATE study_task SET completed_at=CASE WHEN ? THEN COALESCE(completed_at,?) ELSE NULL END,completed=? WHERE id=? AND plan_id IN (SELECT id FROM study_plan WHERE user_id=?)",completed,LocalDateTime.now(ZoneId.of("Asia/Shanghai")),completed,id,user);
        if(updated==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"任务不存在");
    }
    public Map<String,Object> records(String id,String user) {
        own(id,user);
        var totals=db.queryForMap("SELECT COUNT(*) task_count, COALESCE(SUM(duration),0) minutes FROM study_task WHERE plan_id=? AND completed=TRUE",id);
        var dates=db.query("SELECT DISTINCT CAST(completed_at AS DATE) study_day FROM study_task WHERE plan_id=? AND completed=TRUE ORDER BY study_day DESC",(rs,n)->rs.getDate("study_day").toLocalDate(),id);
        int streak=0; LocalDate cursor=today();
        if(!dates.contains(cursor)) cursor=cursor.minusDays(1);
        while(dates.contains(cursor)) {streak++;cursor=cursor.minusDays(1);}
        var subjects=db.query("SELECT subject, SUM(duration) minutes FROM study_task WHERE plan_id=? AND completed=TRUE GROUP BY subject",(rs,n)->Map.of("name",rs.getString("subject"),"minutes",rs.getInt("minutes")),id);
        List<Map<String,Object>> week=new ArrayList<>();
        for(int i=6;i>=0;i--) {
            LocalDate day=today().minusDays(i);
            Integer mins=db.queryForObject("SELECT COALESCE(SUM(duration),0) FROM study_task WHERE plan_id=? AND completed=TRUE AND CAST(completed_at AS DATE)=?",Integer.class,id,day);
            week.add(Map.of("date",day,"minutes",mins));
        }
        return Map.of("completedTasks",totals.get("task_count"),"minutes",totals.get("minutes"),"streak",streak,"subjects",subjects,"week",week);
    }
}
