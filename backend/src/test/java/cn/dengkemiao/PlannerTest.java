package cn.dengkemiao;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlannerTest {
    final LocalDate start=LocalDate.of(2026,10,13);
    final List<Planner.Subject> subjects=List.of(new Planner.Subject("数学",40),new Planner.Subject("英语",20),new Planner.Subject("政治",15),new Planner.Subject("专业课",25));
    @Test void budgetsAndDatesAreConserved() {
        for(int days:List.of(1,2,3,180,730))for(String level:List.of("weak","normal","good")){
            var result=Planner.generate(start,start.plusDays(days),137,level,subjects);
            assertEquals(days*137,result.tasks().stream().mapToInt(Planner.Task::duration).sum());
            for(int d=0;d<days;d++){LocalDate date=start.plusDays(d);assertEquals(137,result.tasks().stream().filter(t->t.date().equals(date)).mapToInt(Planner.Task::duration).sum());}
            assertEquals(start,result.stages().get(0).start());
            assertEquals(start.plusDays(days-1),result.stages().get(result.stages().size()-1).end());
            for(int i=1;i<result.stages().size();i++)assertEquals(result.stages().get(i-1).end().plusDays(1),result.stages().get(i).start());
        }
    }
    @Test void rejectsImpossiblePlans(){
        assertThrows(IllegalArgumentException.class,()->Planner.generate(start,start,240,"normal",subjects));
        assertThrows(IllegalArgumentException.class,()->Planner.generate(start,start.plusDays(731),240,"normal",subjects));
        assertThrows(IllegalArgumentException.class,()->Planner.generate(start,start.plusDays(5),29,"normal",subjects));
        assertThrows(IllegalArgumentException.class,()->Planner.generate(start,start.plusDays(5),240,"normal",List.of(new Planner.Subject("a",0))));
    }
}
