package net.minecraft;

import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceImmutableList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SingleKeyCache;
import net.minecraft.util.TimeSource;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.state.properties.Property;
import net.optifine.SmartExecutorService;
import org.slf4j.Logger;

public class Util {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_MAX_THREADS = 255;
    private static final int DEFAULT_SAFE_FILE_OPERATION_RETRIES = 10;
    private static final String MAX_THREADS_SYSTEM_PROPERTY = "max.bg.threads";
    private static final TracingExecutor BACKGROUND_EXECUTOR = makeExecutor("Main");
    private static final TracingExecutor IO_POOL = makeIoExecutor("IO-Worker-", false);
    private static final TracingExecutor DOWNLOAD_POOL = makeIoExecutor("Download-", true);
    private static final DateTimeFormatter FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
    public static final int LINEAR_LOOKUP_THRESHOLD = 8;
    private static final Set<String> ALLOWED_UNTRUSTED_LINK_PROTOCOLS = Set.of("http", "https");
    public static final long NANOS_PER_MILLI = 1000000L;
    public static TimeSource.NanoTimeSource timeSource = System::nanoTime;
    public static final Ticker TICKER = new Ticker() {
        @Override
        public long read() {
            return Util.timeSource.getAsLong();
        }
    };
    public static final UUID NIL_UUID = new UUID(0L, 0L);
    public static final FileSystemProvider ZIP_FILE_SYSTEM_PROVIDER = FileSystemProvider.installedProviders()
        .stream()
        .filter(providerIn -> providerIn.getScheme().equalsIgnoreCase("jar"))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No jar file system provider found"));
    private static Consumer<String> thePauser = nameIn -> {
    };
    private static Exception exceptionOpenUrl;
    private static final TracingExecutor CAPE_EXECUTOR = makeExecutor("Cape");

    public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
        return Collectors.toMap(Entry::getKey, Entry::getValue);
    }

    public static <T> Collector<T, ?, List<T>> toMutableList() {
        return Collectors.toCollection(Lists::newArrayList);
    }

    public static <T extends Comparable<T>> String getPropertyName(Property<T> pProperty, Object pValue) {
        return pProperty.getName((T)pValue);
    }

    public static String makeDescriptionId(String pType, @Nullable ResourceLocation pId) {
        return pId == null
            ? pType + ".unregistered_sadface"
            : pType + "." + pId.getNamespace() + "." + pId.getPath().replace('/', '.');
    }

    public static long getMillis() {
        return getNanos() / 1000000L;
    }

    public static long getNanos() {
        return timeSource.getAsLong();
    }

    public static long getEpochMillis() {
        return Instant.now().toEpochMilli();
    }

    public static String getFilenameFormattedDateTime() {
        return FILENAME_DATE_TIME_FORMATTER.format(ZonedDateTime.now());
    }

    private static TracingExecutor makeExecutor(String pName) {
        int i = maxAllowedExecutorThreads();
        ExecutorService executorservice;
        if (i <= 0) {
            executorservice = MoreExecutors.newDirectExecutorService();
        } else {
            AtomicInteger atomicinteger = new AtomicInteger(1);
            executorservice = new ForkJoinPool(i, poolIn -> {
                final String s = "Worker-" + pName + "-" + atomicinteger.getAndIncrement();
                ForkJoinWorkerThread forkjoinworkerthread = new ForkJoinWorkerThread(poolIn) {
                    @Override
                    protected void onStart() {
                        TracyClient.setThreadName(s, pName.hashCode());
                        super.onStart();
                    }

                    @Override
                    protected void onTermination(Throwable p_onTermination_1_) {
                        if (p_onTermination_1_ != null) {
                            Util.LOGGER.warn("{} died", this.getName(), p_onTermination_1_);
                        } else {
                            Util.LOGGER.debug("{} shutdown", this.getName());
                        }

                        super.onTermination(p_onTermination_1_);
                    }
                };
                forkjoinworkerthread.setName(s);
                if (pName.equals("Bootstrap")) {
                    forkjoinworkerthread.setPriority(1);
                }

                return forkjoinworkerthread;
            }, Util::onThreadException, true);
        }

        if (pName.equals("Bootstrap")) {
            executorservice = createSmartExecutor(executorservice);
        }

        return new TracingExecutor(executorservice);
    }

    private static ExecutorService createSmartExecutor(ExecutorService executor) {
        int i = Runtime.getRuntime().availableProcessors();
        if (i <= 1) {
            return executor;
        } else {
            ExecutorService executorservice = new SmartExecutorService(executor);
            return executorservice;
        }
    }

    public static int maxAllowedExecutorThreads() {
        return Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, getMaxThreads());
    }

    private static int getMaxThreads() {
        String s = System.getProperty("max.bg.threads");
        if (s != null) {
            try {
                int i = Integer.parseInt(s);
                if (i >= 1 && i <= 255) {
                    return i;
                }

                LOGGER.error("Wrong {} property value '{}'. Should be an integer value between 1 and {}.", "max.bg.threads", s, 255);
            } catch (NumberFormatException numberformatexception1) {
                LOGGER.error("Could not parse {} property value '{}'. Should be an integer value between 1 and {}.", "max.bg.threads", s, 255);
            }
        }

        return 255;
    }

    public static TracingExecutor backgroundExecutor() {
        return BACKGROUND_EXECUTOR;
    }

    public static TracingExecutor ioPool() {
        return IO_POOL;
    }

    public static TracingExecutor nonCriticalIoPool() {
        return DOWNLOAD_POOL;
    }

    public static void shutdownExecutors() {
        BACKGROUND_EXECUTOR.shutdownAndAwait(3L, TimeUnit.SECONDS);
        IO_POOL.shutdownAndAwait(3L, TimeUnit.SECONDS);
        CAPE_EXECUTOR.shutdownAndAwait(3L, TimeUnit.SECONDS);
    }

    private static TracingExecutor makeIoExecutor(String pName, boolean pDaemon) {
        AtomicInteger atomicinteger = new AtomicInteger(1);
        return new TracingExecutor(Executors.newCachedThreadPool(runIn -> {
            Thread thread = new Thread(runIn);
            String s = pName + atomicinteger.getAndIncrement();
            TracyClient.setThreadName(s, pName.hashCode());
            thread.setName(s);
            thread.setDaemon(pDaemon);
            thread.setUncaughtExceptionHandler(Util::onThreadException);
            return thread;
        }));
    }

    public static void throwAsRuntime(Throwable pThrowable) {
        throw pThrowable instanceof RuntimeException ? (RuntimeException)pThrowable : new RuntimeException(pThrowable);
    }

    private static void onThreadException(Thread pThread, Throwable pThrowable) {
        pauseInIde(pThrowable);
        if (pThrowable instanceof CompletionException) {
            pThrowable = pThrowable.getCause();
        }

        if (pThrowable instanceof ReportedException reportedexception) {
            Bootstrap.realStdoutPrintln(reportedexception.getReport().getFriendlyReport(ReportType.CRASH));
            System.exit(-1);
        }

        LOGGER.error(String.format(Locale.ROOT, "Caught exception in thread %s", pThread), pThrowable);
    }

    @Nullable
    public static Type<?> fetchChoiceType(TypeReference pType, String pChoiceName) {
        return !SharedConstants.CHECK_DATA_FIXER_SCHEMA ? null : doFetchChoiceType(pType, pChoiceName);
    }

    @Nullable
    private static Type<?> doFetchChoiceType(TypeReference pType, String pChoiceName) {
        Type<?> type = null;

        try {
            type = DataFixers.getDataFixer()
                .getSchema(DataFixUtils.makeKey(SharedConstants.getCurrentVersion().getDataVersion().getVersion()))
                .getChoiceType(pType, pChoiceName);
        } catch (IllegalArgumentException illegalargumentexception) {
            LOGGER.debug("No data fixer registered for {}", pChoiceName);
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                throw illegalargumentexception;
            }
        }

        return type;
    }

    public static void runNamed(Runnable pTask, String pName) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            Thread thread = Thread.currentThread();
            String s = thread.getName();
            thread.setName(pName);

            try (Zone zone = TracyClient.beginZone(pName, SharedConstants.IS_RUNNING_IN_IDE)) {
                pTask.run();
            } finally {
                thread.setName(s);
            }
        } else {
            try (Zone zone1 = TracyClient.beginZone(pName, SharedConstants.IS_RUNNING_IN_IDE)) {
                pTask.run();
            }
        }
    }

    public static <T> String getRegisteredName(Registry<T> pRegistry, T pValue) {
        ResourceLocation resourcelocation = pRegistry.getKey(pValue);
        return resourcelocation == null ? "[unregistered]" : resourcelocation.toString();
    }

    public static <T> Predicate<T> allOf() {
        return objIn -> true;
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> pPredicate) {
        return (Predicate<T>)pPredicate;
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> pPredicate1, Predicate<? super T> pPredicate2) {
        return objIn -> pPredicate1.test(objIn) && pPredicate2.test(objIn);
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> pPredicate1, Predicate<? super T> pPredicate2, Predicate<? super T> pPredicate3) {
        return objIn -> pPredicate1.test(objIn) && pPredicate2.test(objIn) && pPredicate3.test(objIn);
    }

    public static <T> Predicate<T> allOf(
        Predicate<? super T> pPredicate1, Predicate<? super T> pPredicate2, Predicate<? super T> pPredicate3, Predicate<? super T> pPredicate4
    ) {
        return objIn -> pPredicate1.test(objIn) && pPredicate2.test(objIn) && pPredicate3.test(objIn) && pPredicate4.test(objIn);
    }

    public static <T> Predicate<T> allOf(
        Predicate<? super T> pPredicate1,
        Predicate<? super T> pPredicate2,
        Predicate<? super T> pPredicate3,
        Predicate<? super T> pPredicate4,
        Predicate<? super T> pPredicate5
    ) {
        return objIn -> pPredicate1.test(objIn) && pPredicate2.test(objIn) && pPredicate3.test(objIn) && pPredicate4.test(objIn) && pPredicate5.test(objIn);
    }

    @SafeVarargs
    public static <T> Predicate<T> allOf(Predicate<? super T>... pPredicates) {
        return valIn -> {
            for (Predicate<? super T> predicate : pPredicates) {
                if (!predicate.test(valIn)) {
                    return false;
                }
            }

            return true;
        };
    }

    public static <T> Predicate<T> allOf(List<? extends Predicate<? super T>> pPredicates) {
        return switch (pPredicates.size()) {
            case 0 -> allOf();
            case 1 -> allOf((Predicate<? super T>)pPredicates.get(0));
            case 2 -> allOf((Predicate<? super T>)pPredicates.get(0), (Predicate<? super T>)pPredicates.get(1));
            case 3 -> allOf((Predicate<? super T>)pPredicates.get(0), (Predicate<? super T>)pPredicates.get(1), (Predicate<? super T>)pPredicates.get(2));
            case 4 -> allOf(
            (Predicate<? super T>)pPredicates.get(0),
            (Predicate<? super T>)pPredicates.get(1),
            (Predicate<? super T>)pPredicates.get(2),
            (Predicate<? super T>)pPredicates.get(3)
        );
            case 5 -> allOf(
            (Predicate<? super T>)pPredicates.get(0),
            (Predicate<? super T>)pPredicates.get(1),
            (Predicate<? super T>)pPredicates.get(2),
            (Predicate<? super T>)pPredicates.get(3),
            (Predicate<? super T>)pPredicates.get(4)
        );
            default -> {
                Predicate<? super T>[] predicate = pPredicates.toArray(Predicate[]::new);
                yield allOf(predicate);
            }
        };
    }

    public static <T> Predicate<T> anyOf() {
        return objIn -> false;
    }

    public static <T> Predicate<T> anyOf(Predicate<? super T> pPredicate) {
        return (Predicate<T>)pPredicate;
    }

    public static <T> Predicate<T> anyOf(Predicate<? super T> pPredicate1, Predicate<? super T> pPredicate2) {
        return objIn -> pPredicate1.test(objIn) || pPredicate2.test(objIn);
    }

    public static <T> Predicate<T> anyOf(Predicate<? super T> pPredicate1, Predicate<? super T> pPredicate2, Predicate<? super T> pPredicate3) {
        return objIn -> pPredicate1.test(objIn) || pPredicate2.test(objIn) || pPredicate3.test(objIn);
    }

    public static <T> Predicate<T> anyOf(
        Predicate<? super T> pPredicate1, Predicate<? super T> pPredicate2, Predicate<? super T> pPredicate3, Predicate<? super T> pPredicate4
    ) {
        return objIn -> pPredicate1.test(objIn) || pPredicate2.test(objIn) || pPredicate3.test(objIn) || pPredicate4.test(objIn);
    }

    public static <T> Predicate<T> anyOf(
        Predicate<? super T> pPredicate1,
        Predicate<? super T> pPredicate2,
        Predicate<? super T> pPredicate3,
        Predicate<? super T> pPredicate4,
        Predicate<? super T> pPredicate5
    ) {
        return objIn -> pPredicate1.test(objIn) || pPredicate2.test(objIn) || pPredicate3.test(objIn) || pPredicate4.test(objIn) || pPredicate5.test(objIn);
    }

    @SafeVarargs
    public static <T> Predicate<T> anyOf(Predicate<? super T>... pPredicates) {
        return valIn -> {
            for (Predicate<? super T> predicate : pPredicates) {
                if (predicate.test(valIn)) {
                    return true;
                }
            }

            return false;
        };
    }

    public static <T> Predicate<T> anyOf(List<? extends Predicate<? super T>> pPredicates) {
        return switch (pPredicates.size()) {
            case 0 -> anyOf();
            case 1 -> anyOf((Predicate<? super T>)pPredicates.get(0));
            case 2 -> anyOf((Predicate<? super T>)pPredicates.get(0), (Predicate<? super T>)pPredicates.get(1));
            case 3 -> anyOf((Predicate<? super T>)pPredicates.get(0), (Predicate<? super T>)pPredicates.get(1), (Predicate<? super T>)pPredicates.get(2));
            case 4 -> anyOf(
            (Predicate<? super T>)pPredicates.get(0),
            (Predicate<? super T>)pPredicates.get(1),
            (Predicate<? super T>)pPredicates.get(2),
            (Predicate<? super T>)pPredicates.get(3)
        );
            case 5 -> anyOf(
            (Predicate<? super T>)pPredicates.get(0),
            (Predicate<? super T>)pPredicates.get(1),
            (Predicate<? super T>)pPredicates.get(2),
            (Predicate<? super T>)pPredicates.get(3),
            (Predicate<? super T>)pPredicates.get(4)
        );
            default -> {
                Predicate<? super T>[] predicate = pPredicates.toArray(Predicate[]::new);
                yield anyOf(predicate);
            }
        };
    }

    public static <T> boolean isSymmetrical(int pWidth, int pHeight, List<T> pList) {
        if (pWidth == 1) {
            return true;
        } else {
            int i = pWidth / 2;

            for (int j = 0; j < pHeight; j++) {
                for (int k = 0; k < i; k++) {
                    int l = pWidth - 1 - k;
                    T t = pList.get(k + j * pWidth);
                    T t1 = pList.get(l + j * pWidth);
                    if (!t.equals(t1)) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    public static Util.OS getPlatform() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (s.contains("win")) {
            return Util.OS.WINDOWS;
        } else if (s.contains("mac")) {
            return Util.OS.OSX;
        } else if (s.contains("solaris")) {
            return Util.OS.SOLARIS;
        } else if (s.contains("sunos")) {
            return Util.OS.SOLARIS;
        } else if (s.contains("linux")) {
            return Util.OS.LINUX;
        } else {
            return s.contains("unix") ? Util.OS.LINUX : Util.OS.UNKNOWN;
        }
    }

    public static URI parseAndValidateUntrustedUri(String pUri) throws URISyntaxException {
        URI uri = new URI(pUri);
        String s = uri.getScheme();
        if (s == null) {
            throw new URISyntaxException(pUri, "Missing protocol in URI: " + pUri);
        } else {
            String s1 = s.toLowerCase(Locale.ROOT);
            if (!ALLOWED_UNTRUSTED_LINK_PROTOCOLS.contains(s1)) {
                throw new URISyntaxException(pUri, "Unsupported protocol in URI: " + pUri);
            } else {
                return uri;
            }
        }
    }

    public static Stream<String> getVmArguments() {
        RuntimeMXBean runtimemxbean = ManagementFactory.getRuntimeMXBean();
        return runtimemxbean.getInputArguments().stream().filter(argIn -> argIn.startsWith("-X"));
    }

    public static <T> T lastOf(List<T> pList) {
        return pList.get(pList.size() - 1);
    }

    public static <T> T findNextInIterable(Iterable<T> pIterable, @Nullable T pElement) {
        Iterator<T> iterator = pIterable.iterator();
        T t = iterator.next();
        if (pElement != null) {
            T t1 = t;

            while (t1 != pElement) {
                if (iterator.hasNext()) {
                    t1 = iterator.next();
                }
            }

            if (iterator.hasNext()) {
                return iterator.next();
            }
        }

        return t;
    }

    public static <T> T findPreviousInIterable(Iterable<T> pIterable, @Nullable T pCurrent) {
        Iterator<T> iterator = pIterable.iterator();
        T t = null;

        while (iterator.hasNext()) {
            T t1 = iterator.next();
            if (t1 == pCurrent) {
                if (t == null) {
                    t = iterator.hasNext() ? Iterators.getLast(iterator) : pCurrent;
                }
                break;
            }

            t = t1;
        }

        return t;
    }

    public static <T> T make(Supplier<T> pSupplier) {
        return pSupplier.get();
    }

    public static <T> T make(T pObject, Consumer<? super T> pConsumer) {
        pConsumer.accept(pObject);
        return pObject;
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> makeEnumMap(Class<K> pEnumClass, Function<K, V> pValueGetter) {
        EnumMap<K, V> enummap = new EnumMap<>(pEnumClass);

        for (K k : pEnumClass.getEnumConstants()) {
            enummap.put(k, pValueGetter.apply(k));
        }

        return enummap;
    }

    public static <V> CompletableFuture<List<V>> sequence(List<? extends CompletableFuture<V>> pFutures) {
        if (pFutures.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        } else if (pFutures.size() == 1) {
            return pFutures.get(0).thenApply(List::of);
        } else {
            CompletableFuture<Void> completablefuture = CompletableFuture.allOf(pFutures.toArray(new CompletableFuture[0]));
            return completablefuture.thenApply(objIn -> pFutures.stream().map(CompletableFuture::join).toList());
        }
    }

    public static <V> CompletableFuture<List<V>> sequenceFailFast(List<? extends CompletableFuture<? extends V>> pCompletableFutures) {
        CompletableFuture<List<V>> completablefuture = new CompletableFuture<>();
        return fallibleSequence(pCompletableFutures, completablefuture::completeExceptionally).applyToEither(completablefuture, Function.identity());
    }

    public static <V> CompletableFuture<List<V>> sequenceFailFastAndCancel(List<? extends CompletableFuture<? extends V>> pCompletableFutures) {
        CompletableFuture<List<V>> completablefuture = new CompletableFuture<>();
        return fallibleSequence(pCompletableFutures, throwableIn -> {
            if (completablefuture.completeExceptionally(throwableIn)) {
                for (CompletableFuture<? extends V> completablefuture1 : pCompletableFutures) {
                    completablefuture1.cancel(true);
                }
            }
        }).applyToEither(completablefuture, Function.identity());
    }

    private static <V> CompletableFuture<List<V>> fallibleSequence(List<? extends CompletableFuture<? extends V>> pCompletableFutures, Consumer<Throwable> pThrowableConsumer) {
        List<V> list = Lists.newArrayListWithCapacity(pCompletableFutures.size());
        CompletableFuture<?>[] completablefuture = new CompletableFuture[pCompletableFutures.size()];
        pCompletableFutures.forEach(futureIn -> {
            int i = list.size();
            list.add(null);
            completablefuture[i] = futureIn.whenComplete((objIn, throwableIn) -> {
                if (throwableIn != null) {
                    pThrowableConsumer.accept(throwableIn);
                } else {
                    list.set(i, (V)objIn);
                }
            });
        });
        return CompletableFuture.allOf(completablefuture).thenApply(voidIn -> list);
    }

    public static Exception getExceptionOpenUrl() {
        return exceptionOpenUrl;
    }

    public static void setExceptionOpenUrl(Exception exceptionOpenUrl) {
        Util.exceptionOpenUrl = exceptionOpenUrl;
    }

    public static TracingExecutor getCapeExecutor() {
        return CAPE_EXECUTOR;
    }

    public static <T> Optional<T> ifElse(Optional<T> pOpt, Consumer<T> pConsumer, Runnable pOrElse) {
        if (pOpt.isPresent()) {
            pConsumer.accept(pOpt.get());
        } else {
            pOrElse.run();
        }

        return pOpt;
    }

    public static <T> Supplier<T> name(Supplier<T> pItem, Supplier<String> pNameSupplier) {
        return pItem;
    }

    public static Runnable name(Runnable pItem, Supplier<String> pNameSupplier) {
        return pItem;
    }

    public static void logAndPauseIfInIde(String pError) {
        LOGGER.error(pError);
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            doPause(pError);
        }
    }

    public static void logAndPauseIfInIde(String pMessage, Throwable pError) {
        LOGGER.error(pMessage, pError);
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            doPause(pMessage);
        }
    }

    public static <T extends Throwable> T pauseInIde(T pThrowable) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            LOGGER.error("Trying to throw a fatal exception, pausing in IDE", pThrowable);
            doPause(pThrowable.getMessage());
        }

        return pThrowable;
    }

    public static void setPause(Consumer<String> pThePauser) {
        thePauser = pThePauser;
    }

    private static void doPause(String pMessage) {
        Instant instant = Instant.now();
        LOGGER.warn("Did you remember to set a breakpoint here?");
        boolean flag = Duration.between(instant, Instant.now()).toMillis() > 500L;
        if (!flag) {
            thePauser.accept(pMessage);
        }
    }

    public static String describeError(Throwable pThrowable) {
        if (pThrowable.getCause() != null) {
            return describeError(pThrowable.getCause());
        } else {
            return pThrowable.getMessage() != null ? pThrowable.getMessage() : pThrowable.toString();
        }
    }

    public static <T> T getRandom(T[] pSelections, RandomSource pRandom) {
        return pSelections[pRandom.nextInt(pSelections.length)];
    }

    public static int getRandom(int[] pSelections, RandomSource pRandom) {
        return pSelections[pRandom.nextInt(pSelections.length)];
    }

    public static <T> T getRandom(List<T> pSelections, RandomSource pRandom) {
        return pSelections.get(pRandom.nextInt(pSelections.size()));
    }

    public static <T> Optional<T> getRandomSafe(List<T> pSelections, RandomSource pRandom) {
        return pSelections.isEmpty() ? Optional.empty() : Optional.of(getRandom(pSelections, pRandom));
    }

    private static BooleanSupplier createRenamer(final Path pFilePath, final Path pNewName) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                try {
                    Files.move(pFilePath, pNewName);
                    return true;
                } catch (IOException ioexception) {
                    Util.LOGGER.error("Failed to rename", (Throwable)ioexception);
                    return false;
                }
            }

            @Override
            public String toString() {
                return "rename " + pFilePath + " to " + pNewName;
            }
        };
    }

    private static BooleanSupplier createDeleter(final Path pFilePath) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                try {
                    Files.deleteIfExists(pFilePath);
                    return true;
                } catch (IOException ioexception) {
                    Util.LOGGER.warn("Failed to delete", (Throwable)ioexception);
                    return false;
                }
            }

            @Override
            public String toString() {
                return "delete old " + pFilePath;
            }
        };
    }

    private static BooleanSupplier createFileDeletedCheck(final Path pFilePath) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return !Files.exists(pFilePath);
            }

            @Override
            public String toString() {
                return "verify that " + pFilePath + " is deleted";
            }
        };
    }

    private static BooleanSupplier createFileCreatedCheck(final Path pFilePath) {
        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return Files.isRegularFile(pFilePath);
            }

            @Override
            public String toString() {
                return "verify that " + pFilePath + " is present";
            }
        };
    }

    private static boolean executeInSequence(BooleanSupplier... pSuppliers) {
        for (BooleanSupplier booleansupplier : pSuppliers) {
            if (!booleansupplier.getAsBoolean()) {
                LOGGER.warn("Failed to execute {}", booleansupplier);
                return false;
            }
        }

        return true;
    }

    private static boolean runWithRetries(int pMaxTries, String pActionName, BooleanSupplier... pSuppliers) {
        for (int i = 0; i < pMaxTries; i++) {
            if (executeInSequence(pSuppliers)) {
                return true;
            }

            LOGGER.error("Failed to {}, retrying {}/{}", pActionName, i, pMaxTries);
        }

        LOGGER.error("Failed to {}, aborting, progress might be lost", pActionName);
        return false;
    }

    public static void safeReplaceFile(Path pCurrent, Path pLatest, Path pOldBackup) {
        safeReplaceOrMoveFile(pCurrent, pLatest, pOldBackup, false);
    }

    public static boolean safeReplaceOrMoveFile(Path pCurrent, Path pLatest, Path pOldBackup, boolean pRestore) {
        if (Files.exists(pCurrent)
            && !runWithRetries(10, "create backup " + pOldBackup, createDeleter(pOldBackup), createRenamer(pCurrent, pOldBackup), createFileCreatedCheck(pOldBackup))) {
            return false;
        } else if (!runWithRetries(10, "remove old " + pCurrent, createDeleter(pCurrent), createFileDeletedCheck(pCurrent))) {
            return false;
        } else if (!runWithRetries(10, "replace " + pCurrent + " with " + pLatest, createRenamer(pLatest, pCurrent), createFileCreatedCheck(pCurrent)) && !pRestore) {
            runWithRetries(10, "restore " + pCurrent + " from " + pOldBackup, createRenamer(pOldBackup, pCurrent), createFileCreatedCheck(pCurrent));
            return false;
        } else {
            return true;
        }
    }

    public static int offsetByCodepoints(String pText, int pCursorPos, int pDirection) {
        int i = pText.length();
        if (pDirection >= 0) {
            for (int j = 0; pCursorPos < i && j < pDirection; j++) {
                if (Character.isHighSurrogate(pText.charAt(pCursorPos++)) && pCursorPos < i && Character.isLowSurrogate(pText.charAt(pCursorPos))) {
                    pCursorPos++;
                }
            }
        } else {
            for (int k = pDirection; pCursorPos > 0 && k < 0; k++) {
                pCursorPos--;
                if (Character.isLowSurrogate(pText.charAt(pCursorPos)) && pCursorPos > 0 && Character.isHighSurrogate(pText.charAt(pCursorPos - 1))) {
                    pCursorPos--;
                }
            }
        }

        return pCursorPos;
    }

    public static Consumer<String> prefix(String pPrefix, Consumer<String> pExpectedSize) {
        return strIn -> pExpectedSize.accept(pPrefix + strIn);
    }

    public static DataResult<int[]> fixedSize(IntStream pStream, int pSize) {
        int[] aint = pStream.limit((long)(pSize + 1)).toArray();
        if (aint.length != pSize) {
            Supplier<String> supplier = () -> "Input is not a list of " + pSize + " ints";
            return aint.length >= pSize ? DataResult.error(supplier, Arrays.copyOf(aint, pSize)) : DataResult.error(supplier);
        } else {
            return DataResult.success(aint);
        }
    }

    public static DataResult<long[]> fixedSize(LongStream pStream, int pExpectedSize) {
        long[] along = pStream.limit((long)(pExpectedSize + 1)).toArray();
        if (along.length != pExpectedSize) {
            Supplier<String> supplier = () -> "Input is not a list of " + pExpectedSize + " longs";
            return along.length >= pExpectedSize ? DataResult.error(supplier, Arrays.copyOf(along, pExpectedSize)) : DataResult.error(supplier);
        } else {
            return DataResult.success(along);
        }
    }

    public static <T> DataResult<List<T>> fixedSize(List<T> pList, int pExpectedSize) {
        if (pList.size() != pExpectedSize) {
            Supplier<String> supplier = () -> "Input is not a list of " + pExpectedSize + " elements";
            return pList.size() >= pExpectedSize ? DataResult.error(supplier, pList.subList(0, pExpectedSize)) : DataResult.error(supplier);
        } else {
            return DataResult.success(pList);
        }
    }

    public static void startTimerHackThread() {
        Thread thread = new Thread("Timer hack thread") {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2147483647L);
                    } catch (InterruptedException interruptedexception) {
                        Util.LOGGER.warn("Timer hack thread interrupted, that really should not happen");
                        return;
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    public static void copyBetweenDirs(Path pFromDirectory, Path pToDirectory, Path pFilePath) throws IOException {
        Path path = pFromDirectory.relativize(pFilePath);
        Path path1 = pToDirectory.resolve(path);
        Files.copy(pFilePath, path1);
    }

    public static String sanitizeName(String pFileName, CharPredicate pCharacterValidator) {
        return pFileName.toLowerCase(Locale.ROOT)
            .chars()
            .mapToObj(charIn -> pCharacterValidator.test((char)charIn) ? Character.toString((char)charIn) : "_")
            .collect(Collectors.joining());
    }

    public static <K, V> SingleKeyCache<K, V> singleKeyCache(Function<K, V> pComputeValue) {
        return new SingleKeyCache<>(pComputeValue);
    }

    public static <T, R> Function<T, R> memoize(final Function<T, R> pMemoFunction) {
        return new Function<T, R>() {
            private final Map<T, R> cache = new ConcurrentHashMap<>();

            @Override
            public R apply(T p_apply_1_) {
                return this.cache.computeIfAbsent(p_apply_1_, pMemoFunction);
            }

            @Override
            public String toString() {
                return "memoize/1[function=" + pMemoFunction + ", size=" + this.cache.size() + "]";
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R> memoize(final BiFunction<T, U, R> pMemoBiFunction) {
        return new BiFunction<T, U, R>() {
            private final Map<Pair<T, U>, R> cache = new ConcurrentHashMap<>();

            @Override
            public R apply(T p_apply_1_, U p_apply_2_) {
                return this.cache.computeIfAbsent(Pair.of(p_apply_1_, p_apply_2_), pairIn -> pMemoBiFunction.apply(pairIn.getFirst(), pairIn.getSecond()));
            }

            @Override
            public String toString() {
                return "memoize/2[function=" + pMemoBiFunction + ", size=" + this.cache.size() + "]";
            }
        };
    }

    public static <T> List<T> toShuffledList(Stream<T> pStream, RandomSource pRandom) {
        ObjectArrayList<T> objectarraylist = pStream.collect(ObjectArrayList.toList());
        shuffle(objectarraylist, pRandom);
        return objectarraylist;
    }

    public static IntArrayList toShuffledList(IntStream pStream, RandomSource pRandom) {
        IntArrayList intarraylist = IntArrayList.wrap(pStream.toArray());
        int i = intarraylist.size();

        for (int j = i; j > 1; j--) {
            int k = pRandom.nextInt(j);
            intarraylist.set(j - 1, intarraylist.set(k, intarraylist.getInt(j - 1)));
        }

        return intarraylist;
    }

    public static <T> List<T> shuffledCopy(T[] pArray, RandomSource pRandom) {
        ObjectArrayList<T> objectarraylist = new ObjectArrayList<>(pArray);
        shuffle(objectarraylist, pRandom);
        return objectarraylist;
    }

    public static <T> List<T> shuffledCopy(ObjectArrayList<T> pList, RandomSource pRandom) {
        ObjectArrayList<T> objectarraylist = new ObjectArrayList<>(pList);
        shuffle(objectarraylist, pRandom);
        return objectarraylist;
    }

    public static <T> void shuffle(List<T> pList, RandomSource pRandom) {
        int i = pList.size();

        for (int j = i; j > 1; j--) {
            int k = pRandom.nextInt(j);
            pList.set(j - 1, pList.set(k, pList.get(j - 1)));
        }
    }

    public static <T> CompletableFuture<T> blockUntilDone(Function<Executor, CompletableFuture<T>> pTask) {
        return blockUntilDone(pTask, CompletableFuture::isDone);
    }

    public static <T> T blockUntilDone(Function<Executor, T> pTask, Predicate<T> pDonePredicate) {
        BlockingQueue<Runnable> blockingqueue = new LinkedBlockingQueue<>();
        T t = pTask.apply(blockingqueue::add);

        while (!pDonePredicate.test(t)) {
            try {
                Runnable runnable = blockingqueue.poll(100L, TimeUnit.MILLISECONDS);
                if (runnable != null) {
                    runnable.run();
                }
            } catch (InterruptedException interruptedexception) {
                LOGGER.warn("Interrupted wait");
                break;
            }
        }

        int i = blockingqueue.size();
        if (i > 0) {
            LOGGER.warn("Tasks left in queue: {}", i);
        }

        return t;
    }

    public static <T> ToIntFunction<T> createIndexLookup(List<T> pList) {
        int i = pList.size();
        if (i < 8) {
            return pList::indexOf;
        } else {
            Object2IntMap<T> object2intmap = new Object2IntOpenHashMap<>(i);
            object2intmap.defaultReturnValue(-1);

            for (int j = 0; j < i; j++) {
                object2intmap.put(pList.get(j), j);
            }

            return object2intmap;
        }
    }

    public static <T> ToIntFunction<T> createIndexIdentityLookup(List<T> pList) {
        int i = pList.size();
        if (i < 8) {
            ReferenceList<T> referencelist = new ReferenceImmutableList<>(pList);
            return referencelist::indexOf;
        } else {
            Reference2IntMap<T> reference2intmap = new Reference2IntOpenHashMap<>(i);
            reference2intmap.defaultReturnValue(-1);

            for (int j = 0; j < i; j++) {
                reference2intmap.put(pList.get(j), j);
            }

            return reference2intmap;
        }
    }

    public static <A, B> Typed<B> writeAndReadTypedOrThrow(Typed<A> pTyped, Type<B> pType, UnaryOperator<Dynamic<?>> pOperator) {
        Dynamic<?> dynamic = (Dynamic<?>)pTyped.write().getOrThrow();
        return readTypedOrThrow(pType, pOperator.apply(dynamic), true);
    }

    public static <T> Typed<T> readTypedOrThrow(Type<T> pType, Dynamic<?> pData) {
        return readTypedOrThrow(pType, pData, false);
    }

    public static <T> Typed<T> readTypedOrThrow(Type<T> pType, Dynamic<?> pData, boolean pPartial) {
        DataResult<Typed<T>> dataresult = pType.readTyped(pData).map(Pair::getFirst);

        try {
            return pPartial ? dataresult.getPartialOrThrow(IllegalStateException::new) : dataresult.getOrThrow(IllegalStateException::new);
        } catch (IllegalStateException illegalstateexception) {
            CrashReport crashreport = CrashReport.forThrowable(illegalstateexception, "Reading type");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Info");
            crashreportcategory.setDetail("Data", pData);
            crashreportcategory.setDetail("Type", pType);
            throw new ReportedException(crashreport);
        }
    }

    public static <T> List<T> copyAndAdd(List<T> pList, T pValue) {
        return ImmutableList.<T>builderWithExpectedSize(pList.size() + 1).addAll(pList).add(pValue).build();
    }

    public static <T> List<T> copyAndAdd(T pValue, List<T> pList) {
        return ImmutableList.<T>builderWithExpectedSize(pList.size() + 1).add(pValue).addAll(pList).build();
    }

    public static <K, V> Map<K, V> copyAndPut(Map<K, V> pMap, K pKey, V pValue) {
        return ImmutableMap.<K, V>builderWithExpectedSize(pMap.size() + 1).putAll(pMap).put(pKey, pValue).buildKeepingLast();
    }

    public static enum OS {
        LINUX("linux"),
        SOLARIS("solaris"),
        WINDOWS("windows") {
            @Override
            protected String[] getOpenUriArguments(URI p_345402_) {
                return new String[]{"rundll32", "url.dll,FileProtocolHandler", p_345402_.toString()};
            }
        },
        OSX("mac") {
            @Override
            protected String[] getOpenUriArguments(URI p_342159_) {
                return new String[]{"open", p_342159_.toString()};
            }
        },
        UNKNOWN("unknown");

        private final String telemetryName;

        private OS(final String pTelemetryName) {
            this.telemetryName = pTelemetryName;
        }

        public void openUri(URI pUri) {
            try {
                Process process = AccessController.doPrivileged(
                    (PrivilegedExceptionAction<Process>)(() -> Runtime.getRuntime().exec(this.getOpenUriArguments(pUri)))
                );
                process.getInputStream().close();
                process.getErrorStream().close();
                process.getOutputStream().close();
            } catch (PrivilegedActionException | IOException ioexception) {
                Util.LOGGER.error("Couldn't open location '{}'", pUri, ioexception);
                Util.exceptionOpenUrl = ioexception;
            }
        }

        public void openFile(File pFile) {
            this.openUri(pFile.toURI());
        }

        public void openPath(Path pPath) {
            this.openUri(pPath.toUri());
        }

        protected String[] getOpenUriArguments(URI pUri) {
            String s = pUri.toString();
            if ("file".equals(pUri.getScheme())) {
                s = s.replace("file:", "file://");
            }

            return new String[]{"xdg-open", s};
        }

        public void openUri(String pUri) {
            try {
                this.openUri(new URI(pUri));
            } catch (URISyntaxException | IllegalArgumentException illegalargumentexception) {
                Util.LOGGER.error("Couldn't open uri '{}'", pUri, illegalargumentexception);
            }
        }

        public String telemetryName() {
            return this.telemetryName;
        }
    }
}
