package net.minecraft.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.WorldVersion;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;

public class HashCache {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final String HEADER_MARKER = "// ";
    private final Path rootDir;
    private final Path cacheDir;
    private final String versionId;
    private final Map<String, HashCache.ProviderCache> caches;
    private final Set<String> cachesToWrite = new HashSet<>();
    final Set<Path> cachePaths = new HashSet<>();
    private final int initialCount;
    private int writes;

    private Path getProviderCachePath(String pProvider) {
        return this.cacheDir.resolve(Hashing.sha1().hashString(pProvider, StandardCharsets.UTF_8).toString());
    }

    public HashCache(Path pRootDir, Collection<String> pProviders, WorldVersion pVersion) throws IOException {
        this.versionId = pVersion.getName();
        this.rootDir = pRootDir;
        this.cacheDir = pRootDir.resolve(".cache");
        Files.createDirectories(this.cacheDir);
        Map<String, HashCache.ProviderCache> map = new HashMap<>();
        int i = 0;

        for (String s : pProviders) {
            Path path = this.getProviderCachePath(s);
            this.cachePaths.add(path);
            HashCache.ProviderCache hashcache$providercache = readCache(pRootDir, path);
            map.put(s, hashcache$providercache);
            i += hashcache$providercache.count();
        }

        this.caches = map;
        this.initialCount = i;
    }

    private static HashCache.ProviderCache readCache(Path pRootDir, Path pCachePath) {
        if (Files.isReadable(pCachePath)) {
            try {
                return HashCache.ProviderCache.load(pRootDir, pCachePath);
            } catch (Exception exception) {
                LOGGER.warn("Failed to parse cache {}, discarding", pCachePath, exception);
            }
        }

        return new HashCache.ProviderCache("unknown", ImmutableMap.of());
    }

    public boolean shouldRunInThisVersion(String pProvider) {
        HashCache.ProviderCache hashcache$providercache = this.caches.get(pProvider);
        return hashcache$providercache == null || !hashcache$providercache.version.equals(this.versionId);
    }

    public CompletableFuture<HashCache.UpdateResult> generateUpdate(String pProvider, HashCache.UpdateFunction pUpdateFunction) {
        HashCache.ProviderCache hashcache$providercache = this.caches.get(pProvider);
        if (hashcache$providercache == null) {
            throw new IllegalStateException("Provider not registered: " + pProvider);
        } else {
            HashCache.CacheUpdater hashcache$cacheupdater = new HashCache.CacheUpdater(pProvider, this.versionId, hashcache$providercache);
            return pUpdateFunction.update(hashcache$cacheupdater).thenApply(p_253376_ -> hashcache$cacheupdater.close());
        }
    }

    public void applyUpdate(HashCache.UpdateResult pUpdateResult) {
        this.caches.put(pUpdateResult.providerId(), pUpdateResult.cache());
        this.cachesToWrite.add(pUpdateResult.providerId());
        this.writes = this.writes + pUpdateResult.writes();
    }

    public void purgeStaleAndWrite() throws IOException {
        final Set<Path> set = new HashSet<>();
        this.caches.forEach((p_253378_, p_253379_) -> {
            if (this.cachesToWrite.contains(p_253378_)) {
                Path path = this.getProviderCachePath(p_253378_);
                p_253379_.save(this.rootDir, path, DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) + "\t" + p_253378_);
            }

            set.addAll(p_253379_.data().keySet());
        });
        set.add(this.rootDir.resolve("version.json"));
        final MutableInt mutableint = new MutableInt();
        final MutableInt mutableint1 = new MutableInt();
        Files.walkFileTree(this.rootDir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path p_335518_, BasicFileAttributes p_328145_) {
                if (HashCache.this.cachePaths.contains(p_335518_)) {
                    return FileVisitResult.CONTINUE;
                } else {
                    mutableint.increment();
                    if (set.contains(p_335518_)) {
                        return FileVisitResult.CONTINUE;
                    } else {
                        try {
                            Files.delete(p_335518_);
                        } catch (IOException ioexception) {
                            HashCache.LOGGER.warn("Failed to delete file {}", p_335518_, ioexception);
                        }

                        mutableint1.increment();
                        return FileVisitResult.CONTINUE;
                    }
                }
            }
        });
        LOGGER.info(
            "Caching: total files: {}, old count: {}, new count: {}, removed stale: {}, written: {}",
            mutableint,
            this.initialCount,
            set.size(),
            mutableint1,
            this.writes
        );
    }

    static class CacheUpdater implements CachedOutput {
        private final String provider;
        private final HashCache.ProviderCache oldCache;
        private final HashCache.ProviderCacheBuilder newCache;
        private final AtomicInteger writes = new AtomicInteger();
        private volatile boolean closed;

        CacheUpdater(String pProvider, String pVersion, HashCache.ProviderCache pOldCache) {
            this.provider = pProvider;
            this.oldCache = pOldCache;
            this.newCache = new HashCache.ProviderCacheBuilder(pVersion);
        }

        private boolean shouldWrite(Path pKey, HashCode pValue) {
            return !Objects.equals(this.oldCache.get(pKey), pValue) || !Files.exists(pKey);
        }

        @Override
        public void writeIfNeeded(Path p_236123_, byte[] p_236124_, HashCode p_236125_) throws IOException {
            if (this.closed) {
                throw new IllegalStateException("Cannot write to cache as it has already been closed");
            } else {
                if (this.shouldWrite(p_236123_, p_236125_)) {
                    this.writes.incrementAndGet();
                    Files.createDirectories(p_236123_.getParent());
                    Files.write(p_236123_, p_236124_);
                }

                this.newCache.put(p_236123_, p_236125_);
            }
        }

        public HashCache.UpdateResult close() {
            this.closed = true;
            return new HashCache.UpdateResult(this.provider, this.newCache.build(), this.writes.get());
        }
    }

    static record ProviderCache(String version, ImmutableMap<Path, HashCode> data) {
        @Nullable
        public HashCode get(Path pPath) {
            return this.data.get(pPath);
        }

        public int count() {
            return this.data.size();
        }

        public static HashCache.ProviderCache load(Path pRootDir, Path pCachePath) throws IOException {
            HashCache.ProviderCache hashcache$providercache;
            try (BufferedReader bufferedreader = Files.newBufferedReader(pCachePath, StandardCharsets.UTF_8)) {
                String s = bufferedreader.readLine();
                if (!s.startsWith("// ")) {
                    throw new IllegalStateException("Missing cache file header");
                }

                String[] astring = s.substring("// ".length()).split("\t", 2);
                String s1 = astring[0];
                Builder<Path, HashCode> builder = ImmutableMap.builder();
                bufferedreader.lines().forEach(p_253382_ -> {
                    int i = p_253382_.indexOf(32);
                    builder.put(pRootDir.resolve(p_253382_.substring(i + 1)), HashCode.fromString(p_253382_.substring(0, i)));
                });
                hashcache$providercache = new HashCache.ProviderCache(s1, builder.build());
            }

            return hashcache$providercache;
        }

        public void save(Path pRootDir, Path pCachePath, String pDate) {
            try (BufferedWriter bufferedwriter = Files.newBufferedWriter(pCachePath, StandardCharsets.UTF_8)) {
                bufferedwriter.write("// ");
                bufferedwriter.write(this.version);
                bufferedwriter.write(9);
                bufferedwriter.write(pDate);
                bufferedwriter.newLine();

                for (Entry<Path, HashCode> entry : this.data.entrySet()) {
                    bufferedwriter.write(entry.getValue().toString());
                    bufferedwriter.write(32);
                    bufferedwriter.write(pRootDir.relativize(entry.getKey()).toString());
                    bufferedwriter.newLine();
                }
            } catch (IOException ioexception) {
                HashCache.LOGGER.warn("Unable write cachefile {}: {}", pCachePath, ioexception);
            }
        }
    }

    static record ProviderCacheBuilder(String version, ConcurrentMap<Path, HashCode> data) {
        ProviderCacheBuilder(String pVersion) {
            this(pVersion, new ConcurrentHashMap<>());
        }

        public void put(Path pKey, HashCode pValue) {
            this.data.put(pKey, pValue);
        }

        public HashCache.ProviderCache build() {
            return new HashCache.ProviderCache(this.version, ImmutableMap.copyOf(this.data));
        }
    }

    @FunctionalInterface
    public interface UpdateFunction {
        CompletableFuture<?> update(CachedOutput pOutput);
    }

    public static record UpdateResult(String providerId, HashCache.ProviderCache cache, int writes) {
    }
}