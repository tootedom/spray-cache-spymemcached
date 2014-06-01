package org.greencheek.spray.cache.memcached.perf.state;

import org.openjdk.jmh.annotations.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by dominictootell on 01/06/2014.
 */
@State(Scope.Thread)
public class SmallCacheObject implements Serializable {
  public String name = "bob";

  @Setup
  public synchronized void setup() {
      name = UUID.randomUUID().toString();
  }
}
