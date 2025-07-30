package net.minecraft.world.entity.ai.goal;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public class GoalSelector {
    private static final WrappedGoal NO_GOAL = new WrappedGoal(Integer.MAX_VALUE, new Goal() {
        @Override
        public boolean canUse() {
            return false;
        }
    }) {
        @Override
        public boolean isRunning() {
            return false;
        }
    };
    private final Map<Goal.Flag, WrappedGoal> lockedFlags = new EnumMap<>(Goal.Flag.class);
    private final Set<WrappedGoal> availableGoals = new ObjectLinkedOpenHashSet<>();
    private final EnumSet<Goal.Flag> disabledFlags = EnumSet.noneOf(Goal.Flag.class);

    public void addGoal(int pPriority, Goal pGoal) {
        this.availableGoals.add(new WrappedGoal(pPriority, pGoal));
    }

    @VisibleForTesting
    public void removeAllGoals(Predicate<Goal> pFilter) {
        this.availableGoals.removeIf(p_262564_ -> pFilter.test(p_262564_.getGoal()));
    }

    public void removeGoal(Goal pGoal) {
        for (WrappedGoal wrappedgoal : this.availableGoals) {
            if (wrappedgoal.getGoal() == pGoal && wrappedgoal.isRunning()) {
                wrappedgoal.stop();
            }
        }

        this.availableGoals.removeIf(p_25378_ -> p_25378_.getGoal() == pGoal);
    }

    private static boolean goalContainsAnyFlags(WrappedGoal pGoal, EnumSet<Goal.Flag> pFlag) {
        for (Goal.Flag goal$flag : pGoal.getFlags()) {
            if (pFlag.contains(goal$flag)) {
                return true;
            }
        }

        return false;
    }

    private static boolean goalCanBeReplacedForAllFlags(WrappedGoal pGoal, Map<Goal.Flag, WrappedGoal> pFlag) {
        for (Goal.Flag goal$flag : pGoal.getFlags()) {
            if (!pFlag.getOrDefault(goal$flag, NO_GOAL).canBeReplacedBy(pGoal)) {
                return false;
            }
        }

        return true;
    }

    public void tick() {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("goalCleanup");

        for (WrappedGoal wrappedgoal : this.availableGoals) {
            if (wrappedgoal.isRunning() && (goalContainsAnyFlags(wrappedgoal, this.disabledFlags) || !wrappedgoal.canContinueToUse())) {
                wrappedgoal.stop();
            }
        }

        this.lockedFlags.entrySet().removeIf(p_326929_ -> !p_326929_.getValue().isRunning());
        profilerfiller.pop();
        profilerfiller.push("goalUpdate");

        for (WrappedGoal wrappedgoal2 : this.availableGoals) {
            if (!wrappedgoal2.isRunning() && !goalContainsAnyFlags(wrappedgoal2, this.disabledFlags) && goalCanBeReplacedForAllFlags(wrappedgoal2, this.lockedFlags) && wrappedgoal2.canUse()) {
                for (Goal.Flag goal$flag : wrappedgoal2.getFlags()) {
                    WrappedGoal wrappedgoal1 = this.lockedFlags.getOrDefault(goal$flag, NO_GOAL);
                    wrappedgoal1.stop();
                    this.lockedFlags.put(goal$flag, wrappedgoal2);
                }

                wrappedgoal2.start();
            }
        }

        profilerfiller.pop();
        this.tickRunningGoals(true);
    }

    public void tickRunningGoals(boolean pTickAllRunning) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("goalTick");

        for (WrappedGoal wrappedgoal : this.availableGoals) {
            if (wrappedgoal.isRunning() && (pTickAllRunning || wrappedgoal.requiresUpdateEveryTick())) {
                wrappedgoal.tick();
            }
        }

        profilerfiller.pop();
    }

    public Set<WrappedGoal> getAvailableGoals() {
        return this.availableGoals;
    }

    public void disableControlFlag(Goal.Flag pFlag) {
        this.disabledFlags.add(pFlag);
    }

    public void enableControlFlag(Goal.Flag pFlag) {
        this.disabledFlags.remove(pFlag);
    }

    public void setControlFlag(Goal.Flag pFlag, boolean pEnabled) {
        if (pEnabled) {
            this.enableControlFlag(pFlag);
        } else {
            this.disableControlFlag(pFlag);
        }
    }
}