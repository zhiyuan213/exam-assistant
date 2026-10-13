package cn.dengkemiao;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Pure, deterministic engine: dates stop the day BEFORE the exam; minute budgets are conserved. */
public final class Planner {
    public record Subject(String name, int weight) {}
    public record Stage(String id, String name, String goal, LocalDate start, LocalDate end, int sort) {}
    public record Task(String id, String stageId, LocalDate date, String subject, String name, int duration) {}
    public record Result(List<Stage> stages, List<Task> tasks) {}
    public static Result generate(LocalDate start, LocalDate exam, int minutes, String level, List<Subject> subjects) {
        int days = Math.toIntExact(ChronoUnit.DAYS.between(start, exam));
        if (days < 1 || days > 730 || minutes < 30 || minutes > 720 || subjects.isEmpty() || subjects.size() > 8)
            throw new IllegalArgumentException("考试需在未来730天内，每日学习30至720分钟，科目1至8个");
        if (subjects.stream().anyMatch(s -> s.weight() <= 0 || s.name() == null || s.name().isBlank()))
            throw new IllegalArgumentException("科目名称和权重无效");
        if (subjects.stream().map(Subject::name).distinct().count() != subjects.size())
            throw new IllegalArgumentException("科目不能重复");
        double foundationRatio = switch (level) { case "weak" -> .50; case "good" -> .25; case "normal" -> .40; default -> throw new IllegalArgumentException("基础水平无效"); };
        int[] cuts = {0, (int)Math.round(days * foundationRatio), (int)Math.round(days * .75), days};
        String[] names = {"基础阶段", "强化阶段", "冲刺阶段"};
        String[] goals = {"梳理知识框架，建立学习习惯，掌握基础方法", "专题练习与复盘，巩固薄弱环节", "限时模拟与错题回顾，调整考试节奏"};
        List<Stage> stages = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();
        int[] allocation = allocate(minutes, subjects);
        for (int i = 0; i < 3; i++) {
            if (cuts[i] == cuts[i + 1]) continue;
            Stage stage = new Stage(UUID.randomUUID().toString(), names[i], goals[i], start.plusDays(cuts[i]), start.plusDays(cuts[i+1]-1), i);
            stages.add(stage);
            for (int d = cuts[i]; d < cuts[i+1]; d++) {
                for (int s = 0; s < subjects.size(); s++) {
                    if (allocation[s] == 0) continue;
                    String topic = switch (i) { case 0 -> "基础梳理与例题练习"; case 1 -> "专题练习与错题复盘"; default -> "限时练习与重点回顾"; };
                    // Templates deliberately avoid pretending to know a user's major/course syllabus.
                    tasks.add(new Task(UUID.randomUUID().toString(), stage.id(), start.plusDays(d), subjects.get(s).name(), topic, allocation[s]));
                }
            }
        }
        return new Result(stages, tasks);
    }
    private static int[] allocate(int minutes, List<Subject> subjects) {
        long total = subjects.stream().mapToLong(Subject::weight).sum();
        int[] out = new int[subjects.size()];
        List<Integer> order = new ArrayList<>();
        int allocated = 0;
        for (int i=0; i<out.length; i++) { out[i] = (int)((long)minutes*subjects.get(i).weight()/total); allocated += out[i]; order.add(i); }
        order.sort(Comparator.<Integer>comparingLong(i -> (long)minutes*subjects.get(i).weight()%total).reversed());
        for (int i=0; i<minutes-allocated; i++) out[order.get(i)]++;
        return out;
    }
}
