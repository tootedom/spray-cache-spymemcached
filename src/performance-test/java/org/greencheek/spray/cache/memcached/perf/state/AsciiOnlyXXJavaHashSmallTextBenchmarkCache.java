package org.greencheek.spray.cache.memcached.perf.state;

import org.greencheek.spray.cache.memcached.MemcachedCache;
import org.greencheek.spray.cache.memcached.perftests.cacheobjects.CacheFactory;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Created by dominictootell on 08/06/2014.
 */
@State(Scope.Benchmark)
public class AsciiOnlyXXJavaHashSmallTextBenchmarkCache {
    public MemcachedCache<SmallCacheObject> cache;

    @Setup
    public void setUp() {
        cache = CacheFactory.createAsciiOnlySmallXXJavaTextXXHashCache();
    }

    @TearDown
    public void tearDown() {
        cache.close();
    }

}
