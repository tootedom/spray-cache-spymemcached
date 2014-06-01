package org.greencheek.spray.cache.memcached

/**
 * Created by dominictootell on 01/06/2014.
 */
object LargeString {
   val string : String = """# Memcached For Spray #
                           |
                           |This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
                           |a memcached backed cache for spray caching.
                           |
                           |## Overview ##
                           |
                           |Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
                           |keys to executing futures.
                           |
                           |When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
                           |At this point the entry is removed from the internal cache; and will only exist in memcached.
                           |
                           |Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is
                           |returned.
                           |
                           |The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized
                           |objects.
                           |
                           |
                           |## Dependencies ##
                           |
                           |The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
                           |
                           |## Thundering Herd ##
                           |
                           |The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),
                           |store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may
                           |not be completed, and is specific to the client that generated the future.
                           |
                           |This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):
                           |
                           |    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a
                           |    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally
                           |    (without special guarding techniques, like so-called “cowboy” entries) this can cause many requests
                           |    to compete for system resources while trying to compute the same result thereby greatly reducing overall
                           |    system performance. When you use a spray-caching cache the very first request that arrives for a certain
                           |    cache key causes a future to be put into the cache which all later requests then “hook into”. As soon as
                           |    the first request completes all other ones complete as well. This minimizes processing time and server
                           |    load for all requests.
                           |
                           |Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each
                           |request compute the value, all the requests wait on the 1 invocation of the value to be computed.
                           |
                           |### Memcached thundering herd, how does it do it  ###
                           |
                           |There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)
                           |and the memcached library implementation.
                           |
                           |
                           |# Memcached For Spray #
                           |
                           |This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
                           |a memcached backed cache for spray caching.
                           |
                           |## Overview ##
                           |
                           |Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
                           |keys to executing futures.
                           |
                           |When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
                           |At this point the entry is removed from the internal cache; and will only exist in memcached.
                           |
                           |Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is
                           |returned.
                           |
                           |The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized
                           |objects.
                           |
                           |
                           |## Dependencies ##
                           |
                           |The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
                           |
                           |## Thundering Herd ##
                           |
                           |The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),
                           |store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may
                           |not be completed, and is specific to the client that generated the future.
                           |
                           |This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):
                           |
                           |    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a
                           |    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally
                           |    (without special guarding techniques, like so-called “cowboy” entries) this can cause many requests
                           |    to compete for system resources while trying to compute the same result thereby greatly reducing overall
                           |    system performance. When you use a spray-caching cache the very first request that arrives for a certain
                           |    cache key causes a future to be put into the cache which all later requests then “hook into”. As soon as
                           |    the first request completes all other ones complete as well. This minimizes processing time and server
                           |    load for all requests.
                           |
                           |Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each
                           |request compute the value, all the requests wait on the 1 invocation of the value to be computed.
                           |
                           |### Memcached thundering herd, how does it do it  ###
                           |
                           |There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)
                           |and the memcached library implementation.
                           |
                           |
                           |# Memcached For Spray #
                           |
                           |This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
                           |a memcached backed cache for spray caching.
                           |
                           |## Overview ##
                           |
                           |Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
                           |keys to executing futures.
                           |
                           |When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
                           |At this point the entry is removed from the internal cache; and will only exist in memcached.
                           |
                           |Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is
                           |returned.
                           |
                           |The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized
                           |objects.
                           |
                           |
                           |## Dependencies ##
                           |
                           |The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
                           |
                           |## Thundering Herd ##
                           |
                           |The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),
                           |store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may
                           |not be completed, and is specific to the client that generated the future.
                           |
                           |This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):
                           |
                           |    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a
                           |    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally
                           |    (without special guarding techniques, like so-called “cowboy” entries) this can cause many requests
                           |    to compete for system resources while trying to compute the same result thereby greatly reducing overall
                           |    system performance. When you use a spray-caching cache the very first request that arrives for a certain
                           |    cache key causes a future to be put into the cache which all later requests then “hook into”. As soon as
                           |    the first request completes all other ones complete as well. This minimizes processing time and server
                           |    load for all requests.
                           |
                           |Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each
                           |request compute the value, all the requests wait on the 1 invocation of the value to be computed.
                           |
                           |### Memcached thundering herd, how does it do it  ###
                           |
                           |There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)
                           |and the memcached library implementation.
                           |
                           |
                           |# Memcached For Spray #
                           |
                           |This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
                           |a memcached backed cache for spray caching.
                           |
                           |## Overview ##
                           |
                           |Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
                           |keys to executing futures.
                           |
                           |When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
                           |At this point the entry is removed from the internal cache; and will only exist in memcached.
                           |
                           |Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is
                           |returned.
                           |
                           |The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized
                           |objects.
                           |
                           |
                           |## Dependencies ##
                           |
                           |The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
                           |
                           |## Thundering Herd ##
                           |
                           |The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),
                           |store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may
                           |not be completed, and is specific to the client that generated the future.
                           |
                           |This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):
                           |
                           |    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a
                           |    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally
                           |    (without special guarding techniques, like so-called “cowboy” entries) this can cause many requests
                           |    to compete for system resources while trying to compute the same result thereby greatly reducing overall
                           |    system performance. When you use a spray-caching cache the very first request that arrives for a certain
                           |    cache key causes a future to be put into the cache which all later requests then “hook into”. As soon as
                           |    the first request completes all other ones complete as well. This minimizes processing time and server
                           |    load for all requests.
                           |
                           |Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each
                           |request compute the value, all the requests wait on the 1 invocation of the value to be computed.
                           |
                           |### Memcached thundering herd, how does it do it  ###
                           |
                           |There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)
                           |and the memcached library implementation.
                           |
                           |
                           |# Memcached For Spray #
                           |
                           |This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
                           |a memcached backed cache for spray caching.
                           |
                           |## Overview ##
                           |
                           |Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
                           |keys to executing futures.
                           |
                           |When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
                           |At this point the entry is removed from the internal cache; and will only exist in memcached.
                           |
                           |Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is
                           |returned.
                           |
                           |The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized
                           |objects.
                           |
                           |
                           |## Dependencies ##
                           |
                           |The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
                           |
                           |## Thundering Herd ##
                           |
                           |The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),
                           |store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may
                           |not be completed, and is specific to the client that generated the future.
                           |
                           |This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):
                           |
                           |    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a
                           |    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally
                           |    (without special guarding techniques, like so-called “cowboy” entries) this can cause many requests
                           |    to compete for system resources while trying to compute the same result thereby greatly reducing overall
                           |    system performance. When you use a spray-caching cache the very first request that arrives for a certain
                           |    cache key causes a future to be put into the cache which all later requests then “hook into”. As soon as
                           |    the first request completes all other ones complete as well. This minimizes processing time and server
                           |    load for all requests.
                           |
                           |Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each
                           |request compute the value, all the requests wait on the 1 invocation of the value to be computed.
                           |
                           |### Memcached thundering herd, how does it do it  ###
                           |
                           |There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)
                           |and the memcached library implementation.
                           |
                           |
                           |# Memcached For Spray #
                           |
                           |This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
                           |a memcached backed cache for spray caching.
                           |
                           |## Overview ##
                           |
                           |Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
                           |keys to executing futures.
                           |
                           |When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
                           |At this point the entry is removed from the internal cache; and will only exist in memcached.
                           |
                           |Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is
                           |returned.
                           |
                           |The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized
                           |objects.
                           |
                           |
                           |## Dependencies ##
                           |
                           |The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
                           |
                           |## Thundering Herd ##
                           |
                           |The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),
                           |store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may
                           |not be completed, and is specific to the client that generated the future.
                           |
                           |This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):
                           |
                           |    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a
                           |    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally
                           |    (without special guarding techniques, like so-called “cowboy” entries) this can cause many requests
                           |    to compete for system resources while trying to compute the same result thereby greatly reducing overall
                           |    system performance. When you use a spray-caching cache the very first request that arrives for a certain
                           |    cache key causes a future to be put into the cache which all later requests then “hook into”. As soon as
                           |    the first request completes all other ones complete as well. This minimizes processing time and server
                           |    load for all requests.
                           |
                           |Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each
                           |request compute the value, all the requests wait on the 1 invocation of the value to be computed.
                           |
                           |### Memcached thundering herd, how does it do it  ###
                           |
                           |There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)
                           |and the memcached library implementation.
                           |
                           |
                           |# Memcached For Spray #
                           |
                           |This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
                           |a memcached backed cache for spray caching.
                           |
                           |## Overview ##
                           |
                           |Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
                           |keys to executing futures.
                           |
                           |When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
                           |At this point the entry is removed from the internal cache; and will only exist in memcached.
                           |
                           |Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is
                           |returned.
                           |
                           |The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized
                           |objects.
                           |
                           |
                           |## Dependencies ##
                           |
                           |The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
                           |
                           |## Thundering Herd ##
                           |
                           |The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),
                           |store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may
                           |not be completed, and is specific to the client that generated the future.
                           |
                           |This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):
                           |
                           |    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a
                           |    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally
                           |    (without special guarding techniques, like so-called “cowboy” entries) this can cause many requests
                           |    to compete for system resources while trying to compute the same result thereby greatly reducing overall
                           |    system performance. When you use a spray-caching cache the very first request that arrives for a certain
                           |    cache key causes a future to be put into the cache which all later requests then “hook into”. As soon as
                           |    the first request completes all other ones complete as well. This minimizes processing time and server
                           |    load for all requests.
                           |
                           |Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each
                           |request compute the value, all the requests wait on the 1 invocation of the value to be computed.
                           |
                           |### Memcached thundering herd, how does it do it  ###
                           |
                           |There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)
                           |and the memcached library implementation.
                           |
                           |
                           |# Memcached For Spray #
                           |
                           |This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
                           |a memcached backed cache for spray caching.
                           |
                           |## Overview ##
                           |
                           |Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
                           |keys to executing futures.
                           |
                           |When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
                           |At this point the entry is removed from the internal cache; and will only exist in memcached.
                           |
                           |Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is
                           |returned.
                           |
                           |The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized
                           |objects.
                           |
                           |
                           |## Dependencies ##
                           |
                           |The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
                           |
                           |## Thundering Herd ##
                           |
                           |The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),
                           |store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may
                           |not be completed, and is specific to the client that generated the future.
                           |
                           |This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):
                           |
                           |    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a
                           |    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally
                           |    (without special guarding techniques, like so-called “cowboy” entries) this can cause many requests
                           |    to compete for system resources while trying to compute the same result thereby greatly reducing overall
                           |    system performance. When you use a spray-caching cache the very first request that arrives for a certain
                           |    cache key causes a future to be put into the cache which all later requests then “hook into”. As soon as
                           |    the first request completes all other ones complete as well. This minimizes processing time and server
                           |    load for all requests.
                           |
                           |Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each
                           |request compute the value, all the requests wait on the 1 invocation of the value to be computed.
                           |
                           |### Memcached thundering herd, how does it do it  ###
                           |
                           |There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)
                           |and the memcached library implementation.
                           |
                           |
                           |# Memcached For Spray #
                           |
                           |This library is an extension of the spray-caching (http://spray.io/documentation/1.2.1/spray-caching/).  It provides
                           |a memcached backed cache for spray caching.
                           |
                           |## Overview ##
                           |
                           |Uses an internal ConcurrentLinkedHashMap (https://code.google.com/p/concurrentlinkedhashmap/) to provide a storage of
                           |keys to executing futures.
                           |
                           |When the future completes the computed value (which must be a serializable object), is asynchronously stored in memcached.
                           |At this point the entry is removed from the internal cache; and will only exist in memcached.
                           |
                           |Before the value of the future is computed, memcached is checked for a value.  If a pre-existing value is found this is
                           |returned.
                           |
                           |The keys for the cache must have a toString method that represents that object.  Memcached requires string keys, and serialized
                           |objects.
                           |
                           |
                           |## Dependencies ##
                           |
                           |The library uses the Java Spy Memcached library (https://code.google.com/p/spymemcached/), to communicate with memcached.
                           |
                           |## Thundering Herd ##
                           |
                           |The spray caching api, isn't quite suited for distributed caching implementations.  The existing LRU (Simple and Expiring),
                           |store in the cache a future.  As a result you can't really store a future in a distributed cache.  As the future may or may
                           |not be completed, and is specific to the client that generated the future.
                           |
                           |This architecture lend it's self nicely to the thundering herd issue (as detailed on http://spray.io/documentation/1.2.1/spray-caching/):
                           |
                           |    his approach has the advantage of nicely taking care of the thundering herds problem where many requests to a
                           |    particular cache key (e.g. a resource URI) arrive before the first one could be completed. Normally
                           |    (without special guarding techniques, like so-called “cowboy” entries) this can cause many requests
                           |    to compete for system resources while trying to compute the same result thereby greatly reducing overall
                           |    system performance. When you use a spray-caching cache the very first request that arrives for a certain
                           |    cache key causes a future to be put into the cache which all later requests then “hook into”. As soon as
                           |    the first request completes all other ones complete as well. This minimizes processing time and server
                           |    load for all requests.
                           |
                           |Basically if many requests come in for the same key, and the value has not yet been computed.  Rather than each
                           |request compute the value, all the requests wait on the 1 invocation of the value to be computed.
                           |
                           |### Memcached thundering herd, how does it do it  ###
                           |
                           |There is really no real difference between the SimpleLruCache (https://github.com/spray/spray/blob/v1.2.1/spray-caching/src/main/scala/spray/caching/LruCache.scala#L54)
                           |and the memcached library implementation.
                           |
                           |""".stripMargin
}
