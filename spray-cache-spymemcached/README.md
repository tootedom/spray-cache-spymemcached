# Memcached For Spray #

This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
a memcached backed cache for spray caching.

## Overview ##

Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
keys to executing futures.

When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
At this point the entry is removed from the internal cache; and will only exist in memcached.

Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is
returned.

The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized
objects.

## Usage ##

The library is availble in maven central, and the dependency is as follows:

    <dependency>
      <groupId>org.greencheek.spray</groupId>
      <artifactId>spray-cache-spymemcached</artifactId>
      <version>0.0.7</version>
    </dependency>

## Dependencies ##

The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
It is compiled against the latest version of spray.  The libraries depedencies are as follows:

    [INFO] +- io.spray:spray-caching:jar:1.3.1:compile
    [INFO] |  +- io.spray:spray-util:jar:1.3.1:compile
    [INFO] |  +- org.scala-lang:scala-library:jar:2.10.3:compile
    [INFO] |  \- com.googlecode.concurrentlinkedhashmap:concurrentlinkedhashmap-lru:jar:1.4:compile
    [INFO] +- net.spy:spymemcached:jar:2.10.6:compile
    [INFO] +- com.twitter:jsr166e:jar:1.1.0:compile
    [INFO] +- org.slf4j:slf4j-api:jar:1.7.6:compile

## Thundering Herd ##

The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),
store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may
not be completed, and is specific to the client that generated the future.

This architecture lends its self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/);
where if two requests come in for the same expensive resource (which has the same key), the expensive resource is only
called the once.

    This approach has the advantage of nicely taking care of the thundering herds problem where many requests to a
    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally
    (without special guarding techniques, like so-called “cowboy” entries) this can cause many requests
    to compete for system resources while trying to compute the same result thereby greatly reducing overall
    system performance. When you use a spray-caching cache the very first request that arrives for a certain
    cache key causes a future to be put into the cache which all later requests then “hook into”. As soon as
    the first request completes all other ones complete as well. This minimizes processing time and server
    load for all requests.

Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each
request compute the value, all the requests wait on the 1 invocation of the value to be computed.

### Memcached library, thundering herd, and how does it do it  ###

There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)
and the memcached library implementation.  When two request come for the same key, the future is stored in an internal
ConcurrentLinkedHashMap:

    store.putIfAbsent(keyString, promise.future)

If a subsequent request comes in for the same key, and the future has not completed yet, the existing future in the
ConcurrentLinkedHashMap is returned.

When constructing the MemcachedCache, you can specify the max size of the internal ConcurrentLinkedHash map:

    new MemcachedCache[String](maxCapacity = 500)

With the original SimpleLruCache and the Expiring variant, when the future completes the value is stored by reference in the
ConcurrentLinkedHashMap, associated to a Promise's value.  With this memcached library the value is stored asynchronously
in memcached, and the future completed and removed from the ConcurrentLinkedHashMap.  Therefore, there is a slim time period,
between the completion of the future and the value being saved in memcached.  This means a subsequent request for the same key
could be a cache miss.

If you wish to wait for a period (i.e. make the asynchronous set into memcached call semi synchronous), you can construct
the memcached cached requesting this.  The follow will wait for the memcached set to complete, waiting for a maximum of
1 second.

    new MemcachedCache[String](maxCapacity = 500,
                               waitForMemcachedSet = true,
                               setWaitDuration = Duration(1,TimeUnit.SECONDS))

## Example Uses ##

The use of spray-cache-memcached is really not different to that of the documentation examples on the
spray site (http://spray.io/documentation/1.2.1/spray-caching/), for example:

    // if we have an "expensive" operation
    def expensiveOp(): Double = new java.util.Random().nextDouble()

    // and a Cache for its result type
    val cache: Cache[Double] = new MemcachedCache[Double]()

    // we can wrap the operation with caching support
    // (providing a caching key)
    def cachedOp[T](key: T): Future[Double] = cache(key) {
        expensiveOp()
    }

    // and profit
    cachedOp("foo").await === cachedOp("foo").await
    cachedOp("bar").await !== cachedOp("foo").await

The major differences between the standard spray installations and that of this library is restrictions placed on the cache
by that of memcached:

- Keys must be strings.  For instance the object that is used as a key, has it's toString method called.
- Values must be Serializable (http://docs.oracle.com/javase/7/docs/api/java/io/Serializable.html)

The following examples show various ways of constructing the cache, specific to memcached.  Please see the source for
construct arguments:  https://github.com/tootedom/spray-cache-extensions/blob/master/spray-cache-spymemcached/src/main/scala/org/greencheek/spray/cache/memcached/MemcachedCache.scala#L149


## Specifying the Memcached hosts ##

The hosts are specified in a comma separated list.  The default setting is `localhost:11211`.  If the port isn't supplied
the default `11211` is used

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211")

## Specifying the Expiry of Items in memcached ##

When items are added to memcached, they will have an expiry in seconds (release 0.0.8 will allow for infinite duration):

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211"),
                                                          timeToLive = Duration(10,TimeUnit.MINUTES))


