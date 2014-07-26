- [Memcached For Spray](#memcached-for-spray)
	- [Overview](#overview)
	- [Dependency](#dependency)
    - [Library Dependencies](#library-dependencies)
    - [Thundering Herds](#thundering-herds)
        - [Memcached library, thundering herd, and how does it do it](#memcached-library-thundering-herd-and-how-does-it-do-it)
        - [Thundering herd caveat](#thundering-herd-caveat)
    - [Configuration](#configuration)
        - [Max Capacity](#max-capacity)
        - [Specifying the Memcached hosts](#specifying-the-memcached-hosts)
        - [Host Resolution](#host-resolution)
        - [Host Connection Check](#host-connection-check)
        - [Specifying the Expiry of Items in memcached](#specifying-the-expiry-of-items-in-memcached)
        - [Specifying the Expiry of a single Item](#specifying-the-expiry-of-a-single-item)
        - [No Expiry](#no-expiry)
        - [Using the Binary or Text Protocol](#using-the-binary-or-text-protocol)
        - [Cache Key](#cache-key)
        - [Cache Key Prefix](#cache-key-prefix)
        - [ASCII Keys](#ascii-keys)
        - [Specifying GET Timeout](#specifying-get-timeout)
        - [Specifying SET Timeout](#specifying-set-timeout)
        - [Consistent Hashing](#consistent-hashing)
        - [Caching Serializable Objects](#caching-serializable-objects) 
        - [Serialization Mechanism](#serialization-mechanism)
        - [Stale Caching](#stale-caching)
    - [AWS ElastiCache Support](#aws-elasticache-support)
        - [ElastiCache Configuration Endpoint](#elasticache-configuration-endpoint)
        - [Specifying the polling time](#specifying-the-polling-time)
        - [Persistent Connection](#persistent-connection)
        - [Cluster nodes update and all client closing](#cluster-nodes-update-and-all-client-closing)
        - [ElastiCache Configuration Endpoint timeout](#elasticache-configuration-endpoint-timeout)
        - [Host lookup](#host-lookup)
        
        

----

# Memcached For Spray #

This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
a memcached backed cache for spray caching.

## Overview ##

Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
keys for which futures are currently pending (i.e. the value is still being calculated).

When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
At this point the entry is removed from the internal cache; and will only exist in memcached.

Before the value of the future is computed, memcached is checked for an existing value.  If a pre-existing value is found
this value is returned; wrapped in a Promise.

The keys for the cache must have a toString method that represents that object.  Memcached requires that:

- The key must be a string.
- The values must be serializable objects

----

## Dependency ##

The library is availble in maven central, and the dependency is as follows:

    <dependency>
      <groupId>org.greencheek.spray</groupId>
      <artifactId>spray-cache-spymemcached</artifactId>
      <version>0.2.0</version>
    </dependency>

The library was build using scala 2.10.x.  It has not be tested with scala 2.9.x.  Therefore, consider it only compatible
with 2.10.x

## Library Dependencies ##

The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
It is compiled against the latest version of spray.  The libraries depedencies are as follows:

    [INFO] +- io.netty:netty-all:jar:4.0.21.Final:compile
    [INFO] +- io.spray:spray-caching:jar:1.3.1:compile
    [INFO] |  +- io.spray:spray-util:jar:1.3.1:compile
    [INFO] |  +- org.scala-lang:scala-library:jar:2.10.3:compile
    [INFO] |  \- com.googlecode.concurrentlinkedhashmap:concurrentlinkedhashmap-lru:jar:1.4:compile
    [INFO] +- net.spy:spymemcached:jar:2.11.4:compile
    [INFO] +- com.twitter:jsr166e:jar:1.1.0:compile
    [INFO] +- org.slf4j:slf4j-api:jar:1.7.6:compile
    [INFO] +- net.jpountz.lz4:lz4:jar:1.2.0:compile
    [INFO] +- org.iq80.snappy:snappy:jar:0.3:compile
    [INFO] +- de.ruedigermoeller:fst:jar:1.58:compile
    [INFO] |  \- org.javassist:javassist:jar:3.18.1-GA:compile

The akka library is a requirement for spray, and therefore is a `provided` dependency:

    [INFO] +- com.typesafe.akka:akka-actor_2.10:jar:2.3.0:provided
    [INFO] |  \- com.typesafe:config:jar:1.2.0:provided

----

## Thundering Herds ##

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
and the memcached library implementation.  When two requests come for the same key, the future is stored in an internal
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
                               

### Thundering herd caveat ###

The thundering herd protection is just for the value's calculation, and not that of the direct lookups against the cache (i.e. `.get(key : Any)`)
The reason being that the base `Cache` interface does not support it.  Only the `.apply(..)` method takes an implicit 
Execution context.  The other interface methods do not take an ExecutionContext, and as a result cannot execute futures 
asynchronously.
                               

----

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

----

## Configuration ##

The below will give a couple of example code snippets for using, and configuring the cache for various scenarios:

- The hosts to connect to
- The expiry of items
- The timeout of a get request

----

### Max Capacity ###

As described previously, when the cache is used a future associated with the calculation of the value that is to be
subsequently stored in the cache  is added to an internal ConcurrentLinkedHashMap.


When a requests come for a key, the future is stored in the internal ConcurrentLinkedHashMap:
 
     store.putIfAbsent(keyString, promise.future)
 
If a subsequent request comes in for the same key, and the future has not completed yet, the existing future in the
ConcurrentLinkedHashMap is returned.
 
When constructing the MemcachedCache, you can specify the max size of this internal ConcurrentLinkedHash map:
 
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

----

### Specifying the Memcached hosts ###

The hosts are specified in a comma separated list.  The default setting is `localhost:11211`.  If the port isn't supplied
the default `11211` is used

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211")


----

### Host Resolution ###

The underlying memcached library uses IP address, therefore the passed in host names are resolved to IP address at construction
time.  The default timeout for IP address resolution, per host, is 3 seconds.  This can be changed:

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          dnsConnectionTimeout = Duration(5,TimeUnit.SECONDS))
                                                          
                                                          
----

### Host Connection Check ###

When a host is resolved to an IP address, that does not guarantee that, that host is contactable.  You are able to perform
an additional connection check at construction time to determine that you are able to connect to that host.  If you unable, then
that particular host shall be removed from the list of connactable hosts the memcached client will distribute requests across.
To perform a connection test against the resolved host, and port; at construction time use the `doHostConnectionAttempt = true`
constructor parameter.  This too has a timeout (default 1 second), that can be specified via the `hostConnectionAttemptTimeout = Duration(2,TimeUnit.SECONDS)`:
 
 
    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          dnsConnectionTimeout = Duration(5,TimeUnit.SECONDS),
                                                          doHostConnectionAttempt = true,
                                                          hostConnectionAttemptTimeout = Duration(2,TimeUnit.SECONDS))
                                                          
Normally you do not want to perform this check, as the memcached server might only be temporarily down (maintenance, or other reason).
By default the check is not performed, so that the memcached library will still consider that host eligible for use.  When it is available
again, it will reconnect to it.                                                          

----

### Specifying the Expiry of Items in memcached ###

When items are added to memcached, they will have an expiry in seconds:

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          timeToLive = Duration(10,TimeUnit.MINUTES))


### Specifying the Expiry of a single Item ###

By default when you add an item to the cache, it will be given a default expiry, or the expiry you have set the cache to
having in the cache constructor.

    val cache = new MemcachedCache[String] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
                                             timeToLive = Duration(5,TimeUnit.SECONDS),waitForMemcachedSet = true)


    cache("2")("F")

The item "2", will be given an expiry of 5 seconds.  All items add to the above cache will have a expiry of 5 seconds.
The spray `Cache` interface does not allow the adding of specific items, with specific timeouts.

Therefore an additional method has been added to the `MemcachedCache[Serializable]` class to allow this.  Therefore, your
cache `val` has to be typed to `MemcachedCache` and not `Cache`.

Here is an example of setting a per item expiry:

    val cache = new MemcachedCache[String] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
        timeToLive = Duration(5,TimeUnit.SECONDS),waitForMemcachedSet = true)

    val x = Duration(1,TimeUnit.SECONDS)
    cache( ("1",x) )("A")

In the above the "Key" is specified in a tuple, along with a Duration, i.e. `Tuple2[Any,Duration]`.  It can also be used
in the opposite: `Tuple2[Duration,Any]`:

    cache( (Duration.Inf,"3") )("E")

### No Expiry ###

To have items never expire, you can use `Duration.Inf`, `Duration.Zero` or any duration that is less than
`Duration(1,TimeUnit.SECONDS)`

    val cache = new MemcachedCache[String] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
                                             timeToLive = Duration.Inf,waitForMemcachedSet = true)

----

### Using the Binary or Text Protocol ###

When talking to memcached you can either use the TEXT or BINARY protocol.  The Binary protocol is used by default.  The
following shows how to configure either:


    import net.spy.memcached.ConnectionFactoryBuilder.Protocol

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          protocol = Protocol.BINARY)

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          protocol = Protocol.TEXT)

----

### Cache Key ###

The cache key has to have a `toString` method.  `Memcached` has a requirement for makeup of keys, when using the TEXT
protocol, such that your `toString` method on your key object must conform to the following requirements.

- Need to be a string
- cannot contain ' '(space), '\r'(return), '\n'(linefeed)

If you are using the BINARY protocol these requirements do not apply.  However, you may wish to perform hashing of the
string representing the key to allow for any character to be used.  The library (`as of 0.1.6`), has the ability for a couple
of hash representations of the key:

- SHA256
- MD5
- JenkinsHash
- XXHash

To used either of these you need to specify the hashing method to be used at cache construction time.  For the best performance,
XXHash is recommended:

#### MD5 ####

To use UPPERCASE md5

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          protocol = Protocol.TEXT, keyHashType = MD5KeyHash)

To use lowercase md5

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          protocol = Protocol.TEXT, keyHashType = MD5LowerKeyHash)


#### SHA256 ####

to use UPPERCASE sha256

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          protocol = Protocol.TEXT, keyHashType = SHA256KeyHash)
                                                                  
to use lowercase sha256

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          protocol = Protocol.TEXT, keyHashType = SHA256LowerKeyHash)

#### No Hashing (default) ####

The default is to use no hashing of the key (i.e. it just uses the toString) value.  As mentioned previously this is 
restrictive in size and characters, when using the TEXT protocol.  

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                                  protocol = Protocol.TEXT, keyHashType = NoKeyHash)

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                                  protocol = Protocol.TEXT, keyHashType = null)
                                                                  
                                                                  
### XXHash ###

The xx hash implementation is that of lz4-java (https://github.com/jpountz/lz4-java/tree/master/src/java/net/jpountz/xxhash).
Of which there are two implementations.  A Pure Java version, and a JNI version.                                                                  
                                                                
Java Version:
                                                                  
    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                                  protocol = Protocol.TEXT, keyHashType = XXJavaHash)
                                                                  

JNI Version:
                                                                  
    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                                  protocol = Protocol.TEXT, keyHashType = XXNativeJavaHash)                                                                  
                                                                                                                                    

### Jenkins ###

The Jenkins hash is taken from xmemcached (https://github.com/killme2008/xmemcached/blob/master/src/main/java/net/rubyeye/xmemcached/HashAlgorithm.java): 
                                                                                                                                    
    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                                  protocol = Protocol.TEXT, keyHashType = JenkinsHash)                                                                                                                                    


----

### ASCII Keys ###

Available as of `0.1.17`

If you know your key is made up of ASCII characters (the `toString`) method of your class returns an ASCII only string,
you can add the parameter `asciiOnlyKeys = true` to the Cache constructor.  This will improve performance slightly, as the
conversion of a string to a byte[] is quicker for ASCII strings than it is for UTF-8.


    val cache: Cache[MyCaseClass] = new MemcachedCache[MyCaseClass](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                                    protocol = Protocol.TEXT, keyHashType = XXJavaHash,
                                                                    asciiOnlyKeys = true)
                                                                    
----                                                                    

### Cache Key Prefix ###

You may have several in memory caches in your application that you wish to migrate to a using a shared distributed cache. 
However, as this means you effectively now have just a single cache rather that many; suddenly you have key collisions, or worse
`ClassCastExceptions` at runtime as a cache is expecting one object type, but returns another.

For example you may have had the follow, that stores `Product` and `ProductCategories` in two separate caches.  The key for the
cache item is `ProductId` which can be the same.:

    @SerialVersionUID(1l) case class ProductId(id : String)
    @SerialVersionUID(1l) case class Product(description : String)
    @SerialVersionUID(1l) case class ProductCategories(category : Seq[String])
    
    val productId = ProductId("1a")
    val fridge = Product("huge smeg")
    val fridgeCategories = ProductCategories(Seq("kitchen","home","luxury"))
        
    val categoryCache: Cache[ProductCategories] = new SimpleLruCache[ProductCategories](10,10)
    val productCache: Cache[Product] = new SimpleLruCache[Product](10,10)
    
    categoryCache(productId)(fridgeCategories).await === fridgeCategories
    productCache(productId)(fridge).await === fridge
    categoryCache.get(productId).get.await == fridgeCategories
            
            
If you were to convert the above directly, replacing the two caches with memcached equivalent, you would obtain a 
`ClassCastException` when looking the item in the `productCache`.  The reason being the Memcached is a shared distributed
cache.  This means the cache keys are shared amongst all the cache objects.

    @SerialVersionUID(1l) case class ProductId(id : String)
    @SerialVersionUID(1l) case class Product(description : String)
    @SerialVersionUID(1l) case class ProductCategories(category : Seq[String])
    
    val productId = ProductId("1a")
    val fridge = Product("huge smeg")
    val fridgeCategories = ProductCategories(Seq("kitchen","home","luxury"))

    val categoryCache: Cache[ProductCategories] = new MemcachedCache[ProductCategories](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
      timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash)
    val productCache: Cache[Product] = new MemcachedCache[Product](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
      timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash)

    categoryCache(productId)(fridgeCategories).await === fridgeCategories
    productCache(productId)(fridge).await must throwA[ClassCastException]
    categoryCache.get(productId).get.await == fridgeCategories
    
    
To avoid this, a `keyPrefix` option is available on the `MemcachedCache`.  This takes a String that is pre-pended to the
key as the item is stored against; `keyPrefix = Some("product")`:


    @SerialVersionUID(1l) case class ProductId(id : String)
    @SerialVersionUID(1l) case class Product(description : String)
    @SerialVersionUID(1l) case class ProductCategories(category : Seq[String])
    
    val productId = ProductId("1a")
    val fridge = Product("huge smeg")
    val fridgeCategories = ProductCategories(Seq("kitchen","home","luxury"))

    val categoryCache: Cache[ProductCategories] = new MemcachedCache[ProductCategories](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
      timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash, keyPrefix = Some("productcategories"))
    val productCache: Cache[Product] = new MemcachedCache[Product](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
      timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash, keyPrefix = Some("product"))


    categoryCache(productId)(fridgeCategories).await === fridgeCategories
    productCache(productId)(fridge).await === fridge
    categoryCache.get(productId).get.await == fridgeCategories    

----

### Server Hash Algorithm ###

By default the memcached implementation uses Ketama Consistent Hashing for the node locator (distribution of the memcached servers) as described here:
(http://www.last.fm/user/RJ/journal/2007/04/10/rz_libketama_-_a_consistent_hashing_algo_for_memcache_clients).  

The memcached key is hashed to a number (unsigned int), which is then mapped to the memcached node that is closest to that
integer value.  The generation of the unsigned int is performed by a hashing algorithm.  This hashing algorithm can be configured
at construction time by the parameter: `hashAlgorithm`.  It can take one of the following values:

- `MemcachedCache.XXHASH_ALGORITHM`
- `MemcachedCache.JENKINS_ALGORITHM`
- `MemcachedCache.DEFAULT_ALGORITHM

The `MemcachedCache.DEFAULT_ALGORITHM` is that of `DefaultHashAlgorithm.KETAMA_HASH`.  An example is the following:
 
    new MemcachedCache[SimpleClass](Duration.Zero, 10000, "localhost:11211", protocol = Protocol.TEXT,
         waitForMemcachedSet = true, keyHashType = SHA256KeyHash, hashAlgorithm = MemcachedCache.XXHASH_ALGORITHM)


### Specifying GET Timeout ###

When querying memcached, there is a timeout associated with the get request; otherwise the GET from memcache would block
the caller.  As a result there is a default timeout when memcached is queried.  This is `Duration(2500,TimeUnit.MILLISECONDS)`,
in other words `2.5 seconds`.  This is the default that is supplied with the SPY memcached library being used to talk to
memcached.

To change the length of time to wait/block on the get, you need to specify the `Duration` to wait at construction time

memcachedGetTimeout

    val cache: Cache[Double] = new MemcachedCache[Double](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          protocol = Protocol.TEXT, keyHashType = SHA256KeyHash,
                                                          memcachedGetTimeout = Duration(1,TimeUnit.SECONDS))
                                                          
### Specifying SET Timeout ###
                                                          
As mentioned previous, a ConcurrentLinkHashMap is used internally to store future objects; whilst the cache value is calculated.
When the cache value has been computed is ready for storing in memcached, the future is removed from the internal map. 
The set on memcached is done asynchronously in the background.  Therefore, there is a slim time period, between the 
completion of the future and the value being saved in memcached.  This means a subsequent request for the same key could 
be a cache miss; and result in both the value being calculated and then set in memcached.

Therefore it is recommended that you wait for a period of time for the memcached set to complete  (i.e. make the asynchronous set into 
memcached call semi synchronous).  This is done at cache construction time.   The follow will wait for the memcached set to complete,
waiting for a maximum of 1 second.

    new MemcachedCache[String](waitForMemcachedSet = true,
                               setWaitDuration = Duration(1,TimeUnit.SECONDS))
                                                          

----

### Caching Serializable Objects ###

As mentioned previously memcached uses `String` keys, and values are binary objects (i.e. Object must be `Serializable`).
Case classes by default extend the scala Serializable trait which extends the Java Serializable interface.  

Remember you need to assign you class a @SerialVersionUID to avoid any runtime `java.io.InvalidClassException` that may 
result in a changing serialVersionUID value when additional method (but no state changes) are made to a class.  The reason
being that the items within memcached may be cached for longer that your current application version.  You may release
 a new version of an application, which fetches a cached serialized object from the old application.  If the serialVersionUID is
 created by the compiler, you are more likely to hit the `InvalidClassException` or a `ClassNotFoundException`

````java
    import org.greencheek.spray.cache.memcached.MemcachedCache
    import org.greencheek.util.memcached.WithMemcached
    import org.specs2.mutable.Specification
    import net.spy.memcached.ConnectionFactoryBuilder.Protocol
    import scala.concurrent.duration.Duration
    import java.util.concurrent.TimeUnit
    import spray.util._
    import scala.concurrent._
    import ExecutionContext.Implicits.global
    import spray.caching.Cache
    import org.greencheek.spray.cache.memcached.keyhashing.XXJavaHash
    
    
    @SerialVersionUID(1l) case class PonziScheme(owner : Person, victims : Seq[Person])
    
    @SerialVersionUID(2l) case class Person(val firstName : String,val lastName : String) {
      private val fullName = firstName + " " + lastName
      override def toString() : String = {
        fullName
      }
    }
        
    
    class SerializationExampleSpec extends Specification {
      val memcachedContext = WithMemcached(false)
    
      "Example case class serialization" in memcachedContext {
        val memcachedHosts = "localhost:" + memcachedContext.memcached.port
        val cache: Cache[PonziScheme] = new MemcachedCache[PonziScheme](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
          timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash)
    
    
        val madoff = new Person("Bernie","Madoff")
        val victim1 = new Person("Kevin","Bacon")
        val victim2 = new Person("Kyra", "Sedgwick")
    
        val madoffsScheme = new PonziScheme(madoff,Seq(victim1,victim2))
    
        cache(madoff)(madoffsScheme).await === madoffsScheme
        // Wait for a bit.. item is still cached (5 second expiry)
        Thread.sleep(2000)
        cache.get(madoff) must beSome
        cache.get(madoff).get.await.owner === madoff

        // Use the expensive operation method, this returns as it's in memcached
        cachedOp(cache, madoff).await === madoffsScheme
        cachedOp(cache, Person("Charles","Ponzi")).await === new PonziScheme(Person("Charles","Ponzi"),Seq(Person("Rose","Gnecco")))

        // if we have an "expensive" operation
        def expensiveOp(): PonziScheme = {
          Thread.sleep(500)
          new PonziScheme(Person("Charles","Ponzi"),Seq(Person("Rose","Gnecco")))
        }

        def cachedOp[T](cache: Cache[PonziScheme], key: T): Future[PonziScheme] = cache(key) {
          expensiveOp()
        }
    
        true
      }
    }
````    

----

### Serialization Mechanism ###

Since `0.1.17` the default serialization mechanism used by the MemcachedCache is that provided by teh `fast-serialization`
library (https://github.com/RuedigerMoeller/fast-serialization).

Prior to this a standard `ObjectOutputStream/ObjectInputStream` was used (i.e. standard java serialization).  The Fast library
is significantly more performant than that of the standard JDK serialization.  However, if you find issues with serialization,
you can choose to use the JDK serialization:

To use the Default JDK Serialization use the `serializingTranscoder` constructor parameter, as follows:

    val cache: Cache[ProductCase] = new MemcachedCache[ProductCase](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          protocol = Protocol.TEXT, keyHashType = XXJavaHash,
                                                          memcachedGetTimeout = Duration(1,TimeUnit.SECONDS),
                                                          serializingTranscoder = new SerializingTranscoder())
                                                          
The Fast Serialization, which is the default, can be explicitly set as follows:                                                          

    val cache: Cache[ProductCase] = new MemcachedCache[ProductCase](memcachedHosts = "host1:11211,host2:11211,host3:11211",
                                                          protocol = Protocol.TEXT, keyHashType = XXJavaHash,
                                                          memcachedGetTimeout = Duration(1,TimeUnit.SECONDS),
                                                          serializingTranscoder = new FastSerializingTranscoder())
                                                          
                                                          
----
                                                          
### Stale Caching ###
                                                          
Since `0.2.0` the client supports a stale caching mechanism; this by default is not enabled as it requires an additional
future (via composition) to perform the additional cache lookup.  It is also an addition lookup on the memcached server, and
also will use x2 the memory (items are stored twice in the cache).
 
The stale caching function is a mini "stale-while-revalidate" mechanism.  Without the stale caching enabled, when an item
expires in the cache, which is popular; then a lot of requests will be waiting on the cache item to be regenerated from the
backend.  This means you can have a spike in a larger than you would like requests.

With stale caching enabled, only one request will regenerate the item from the backend cache.  The other request will use
a stale cache.  The stale cached is ONLY checked is a future exists in the internal cache, meaning that a backend request 
is in operation to calculate the cache item

With stale caching enabled when an item is stored in memcached, it is stored twice.  The 2nd time it is stored under a 
different key.

The following options control the stale caching.

````
    val useStaleCache : Boolean = false,
    val staleCacheAdditionalTimeToLive : Duration = Duration.MinusInf,
    val staleCachePrefix  : String = "stale",
    val staleMaxCapacity : Int = -1,
    val staleCacheMemachedGetTimeout : Duration = Duration.MinusInf
````                                   
         
                                                          
The default settings for the Stale caching, after it is turned on with `useStaleCache = true` are as above.  To set the 
prefix (which is prepended to the origin key, after it is hash), use `staleCachePrefix`.  To set the time to live of
stale item use `staleCacheAdditionalTimeToLive`.  This Duration is ADDED to the original `timeToLive` of the item. 
A `Duration.MinusInf` means the stale item is kept for `timeToLive` x 2.   The capacity of internal thundering herd 
cache for the stale caching is specified with `staleMaxCapacity`.  Again '-1' means the capacity is the same as `maxCapacity`.
The timeout for the get against memcached for the stale entry is specified in the param `staleCacheMemachedGetTimeout`.
If this is `Duration.MinusInf` the timeout is the same as the timeout for the original key (`memcachedGetTimeout`)
                                                          
----

## AWS ElastiCache Support ##

Since release `0.2.0` there has been support AWS's ElasticCache memcached cluster:
 
- http://aws.amazon.com/elasticache/
- http://docs.aws.amazon.com/AmazonElastiCache/latest/UserGuide/WhatIs.html

This is done by creating an instance of `ElastiCache` rather than `MemcachedCache`.  An example is as follows:
 
```` 
    cache = new ElastiCache[String](elastiCacheConfigHosts = "yourcluster.jgkygp.0001.euw1.cache.amazonaws.com:11211",
                                    initialConfigPollingDelay = 0,
                                    configPollingTime = 60,
                                    configPollingTimeUnit = TimeUnit.SECONDS,                                    
                                    delayBeforeClientClose = Duration(10,TimeUnit.SECONDS),                                    
                                    dnsConnectionTimeout = Duration(3,TimeUnit.SECONDS),
                                    timeToLive = Duration(1, TimeUnit.SECONDS),
                                    connectionTimeoutInMillis = 3000,
                                    numberOfConsecutiveInvalidConfigurationsBeforeReconnect = 3,
                                    reconnectDelay = Duration(5,TimeUnit.SECONDS),
                                    idleReadTimeout = Duration(125,TimeUnit.SECONDS)                                    
                                    )                                                                                                         
````        

The ElastiCache supports the same configuration options that have been described previously:

- timeToLive
- maxCapacity
- set timeout
- get timeout
- hashing
- key prefix
                                           
### ElastiCache Configuration Endpoint ###
                                           
The `ElastiCache` cache works by using the auto discovery mechanism as described here:
                                            
- http://docs.aws.amazon.com/AmazonElastiCache/latest/UserGuide/AutoDiscovery.AddingToYourClientLibrary.html
                                            
You supply to the `ElastiCache` cache the url of the ElastiCache Configuration Endpoint.  The `ElastiCache` cache uses
the netty library (http://netty.io/) to periodically send the `config get cluster` command to the ElastiCache Configuration Endpoint.
The ElastiCache keeps a persistent connection open to the ElastiCache Configuration Endpoint, sending the command periodically.
The ElastiCache Configuration Endpoint returns a configuration similar to the following, that details the actually memcached 
instances that should be connected to:
                                             
````
    CONFIG cluster 0 147
    12
    myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211
    
    END
````

When the version number (the second line) increases a new spy memcached instance is created, and the old spy memcached instance
is scheduled for being closed.

The ElastiCache Configuration Endpoint is specified via the `elastiCacheConfigHosts` parameter.

````
    elastiCacheConfigHosts = "yourcluster.jgkygp.0001.euw1.cache.amazonaws.com:11211"   
````    

For the moment you should only specify 1 configuration host.  Currently a cache cluster is only in one Availability Zone.
a cluster cannot at the moment in AWS span multiple Availability Zones.  You can have 3 separate elasticache clusters,
one in each availability zone, but the cache will only connect to 1 availability zone at any one time.

Whilst is is technically possible for the client to take a comma separated list:


````
    elastiCacheConfigHosts = "yourcluster.jgkygp.0001.euw1.cache.amazonaws.com:11211,xxxx:11211,xxxx:11211"       
````    

It will currently only connect to 1 of the clusters, and if a reconnect to a cluster is performed (see below), then the
Elasticache client will go to the next cluster in the comma separated hosts list. i.e.  Given the following:
 
````
    elastiCacheConfigHosts = "clusterzone1:11211,clusterzone2:11211,clusterzone3:11211"                    
````    

If we are currently connected to clusterzone1, and a reconnection occurs, the client will attempt to connect to the 
clusterzone2 configuration endpoint.  It does NOT connect to all zones.  As you can see this is not really something that
useful; and will mostly like change in the immediate future.  Therefore, for the moment, only specify one 
elasticcache configuration endpoint host:port combo.
 



### Specifying the polling time ###

By default the ElastiCache cache polls the ElastiCache Configuration Endpoint for an update to the nodes that make up the 
cluster every 60 seconds.  This can be configured via the following parameters:

````
    initialConfigPollingDelay = 0,
    configPollingTime = 60,
    configPollingTimeUnit = TimeUnit.SECONDS,
````    
    
If no hosts are available, then the cache is disabled and the following warning message will be output.  Your application
will still continue to work, but you will just have no caching or thundering herd protection.  All `.get()` will be misses
and any `apply(..)` calls will wait on a new future to complete (i.e. call the backend).

````
    o.g.s.c.memcached.MemcachedCache - Cache is disabled
````    

### Persistent Connection ###
    
The ElastiCache uses a persistent connection to the ElastiCache Configuration Endpoint.  If the connection is lost,
the client will automatically reconnect.  The client will wait for a period (default 5 seconds) before reconnecting.  This
can be changed by specifying `reconnectDelay`.

If the client does also receive any data from the ElastiCache Configuration Endpoint, a reconnection will be made; this
idle period is controlled by the setting `idleReadTimeout`.  This is set to 125 seconds by default.  If you modify this setting
you shouldn't set it lower that the polling duration; as you will just end up in the persistent connection not being persistent.

````
    reconnectDelay = Duration(5,TimeUnit.SECONDS),
    idleReadTimeout = Duration(125,TimeUnit.SECONDS)                                    
````

If the ElastiCache Configuration Endpoint is in some way returning invalid configurations, then the client will reconnect
to the Configuration Endpoint.  By default it takes 3 consecutive invalid configurations before the client will reconnect.
This is controlled by the parameter: `numberOfConsecutiveInvalidConfigurationsBeforeReconnect`

````
   numberOfConsecutiveInvalidConfigurationsBeforeReconnect = 5
````   

### Cluster nodes update and all client closing ###
    
When the ElastiCache Configuration Endpoint, outputs a configuration update a new spy memcached client is created, and
the old client is closed.  There a delay before the old client is closed, as it may still be in use (i.e. network requests 
may still be executing).  By default the delay is 10 second; this can be change by specifying the parameter:

````
    delayBeforeClientClose = Duration(1,TimeUnit.SECONDS),
````
                                        
### ElastiCache Configuration Endpoint timeout ###

By default the client will wait for 3 seconds for a connection to the ElastiCache Configuration Endpoint.  This can be 
changed by the following parameter `connectionTimeoutInMillis`.
    
````                                       
    connectionTimeoutInMillis = 3000
````    

### Host lookup ###

When the ElastiCache Configuration Endpoint returns the configuration information it returns the hostname, and it may 
send with it the IP address.

````
    CONFIG cluster 0 147
    12
    myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211
    
    END
````

If the IP address is not returned the client will perform a DNS lookup on the hostname.  By default the timeout is 3 seconds.
This can be changed with the parameter `dnsConnectionTimeout`

````
    dnsConnectionTimeout = Duration(5,TimeUnit.SECONDS)
````

