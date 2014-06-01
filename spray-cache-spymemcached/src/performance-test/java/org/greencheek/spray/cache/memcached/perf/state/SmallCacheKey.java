package org.greencheek.spray.cache.memcached.perf.state;

/**
 * Created by dominictootell on 01/06/2014.
 */
import org.openjdk.jmh.annotations.*;
import java.util.UUID;

@State(Scope.Thread)
public class SmallCacheKey {
  public String key  = "unknown";

  @Setup
  public synchronized void setup() {
    key = UUID.randomUUID().toString();
  }

  @Override
  public String toString() {
    return key;
  }
}
