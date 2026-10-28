package com.example.emilia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tiny server-tick scheduler used to make spells travel and rise over time. */
public final class Scheduler {
    private record Task(long runAt, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static long now = 0;

    private Scheduler() {}

    public static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new Task(now + Math.max(1, delayTicks), action));
    }

    public static void tick() {
        now++;
        if (TASKS.isEmpty()) return;
        List<Runnable> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.runAt() <= now) {
                due.add(t.action());
                it.remove();
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void clear() {
        TASKS.clear();
    }
}
