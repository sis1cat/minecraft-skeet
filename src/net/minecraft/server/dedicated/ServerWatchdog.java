package net.minecraft.server.dedicated;

import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.Util;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

public class ServerWatchdog implements Runnable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MAX_SHUTDOWN_TIME = 10000L;
    private static final int SHUTDOWN_STATUS = 1;
    private final DedicatedServer server;
    private final long maxTickTimeNanos;

    public ServerWatchdog(DedicatedServer pServer) {
        this.server = pServer;
        this.maxTickTimeNanos = pServer.getMaxTickLength() * TimeUtil.NANOSECONDS_PER_MILLISECOND;
    }

    @Override
    public void run() {
        while (this.server.isRunning()) {
            long i = this.server.getNextTickTime();
            long j = Util.getNanos();
            long k = j - i;
            if (k > this.maxTickTimeNanos) {
                LOGGER.error(
                    LogUtils.FATAL_MARKER,
                    "A single server tick took {} seconds (should be max {})",
                    String.format(Locale.ROOT, "%.2f", (float)k / (float)TimeUtil.NANOSECONDS_PER_SECOND),
                    String.format(Locale.ROOT, "%.2f", this.server.tickRateManager().millisecondsPerTick() / (float)TimeUtil.MILLISECONDS_PER_SECOND)
                );
                LOGGER.error(LogUtils.FATAL_MARKER, "Considering it to be crashed, server will forcibly shutdown.");
                CrashReport crashreport = createWatchdogCrashReport("Watching Server", this.server.getRunningThread().threadId());
                this.server.fillSystemReport(crashreport.getSystemReport());
                CrashReportCategory crashreportcategory = crashreport.addCategory("Performance stats");
                crashreportcategory.setDetail("Random tick rate", () -> this.server.getWorldData().getGameRules().getRule(GameRules.RULE_RANDOMTICKING).toString());
                crashreportcategory.setDetail(
                    "Level stats",
                    () -> Streams.stream(this.server.getAllLevels())
                            .map(p_374881_ -> p_374881_.dimension().location() + ": " + p_374881_.getWatchdogStats())
                            .collect(Collectors.joining(",\n"))
                );
                Bootstrap.realStdoutPrintln("Crash report:\n" + crashreport.getFriendlyReport(ReportType.CRASH));
                Path path = this.server.getServerDirectory().resolve("crash-reports").resolve("crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
                if (crashreport.saveToFile(path, ReportType.CRASH)) {
                    LOGGER.error("This crash report has been saved to: {}", path.toAbsolutePath());
                } else {
                    LOGGER.error("We were unable to save this crash report to disk.");
                }

                this.exit();
            }

            try {
                Thread.sleep((i + this.maxTickTimeNanos - j) / TimeUtil.NANOSECONDS_PER_MILLISECOND);
            } catch (InterruptedException interruptedexception) {
            }
        }
    }

    public static CrashReport createWatchdogCrashReport(String pTitle, long pThreadId) {
        ThreadMXBean threadmxbean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] athreadinfo = threadmxbean.dumpAllThreads(true, true);
        StringBuilder stringbuilder = new StringBuilder();
        Error error = new Error("Watchdog");

        for (ThreadInfo threadinfo : athreadinfo) {
            if (threadinfo.getThreadId() == pThreadId) {
                error.setStackTrace(threadinfo.getStackTrace());
            }

            stringbuilder.append(threadinfo);
            stringbuilder.append("\n");
        }

        CrashReport crashreport = new CrashReport(pTitle, error);
        CrashReportCategory crashreportcategory = crashreport.addCategory("Thread Dump");
        crashreportcategory.setDetail("Threads", stringbuilder);
        return crashreport;
    }

    private void exit() {
        try {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Runtime.getRuntime().halt(1);
                }
            }, 10000L);
            System.exit(1);
        } catch (Throwable throwable) {
            Runtime.getRuntime().halt(1);
        }
    }
}