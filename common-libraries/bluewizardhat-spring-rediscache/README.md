Common Libraries - Simple Redis Cache
==================================================================================================

A simple caching mechanism using Redis as the backend.


### Behaviour

This cache works with two "timeout" values, ```expireAfter``` and an optional ```refreshAfter```.

* If a value is not found in the cache the supplier is called to fetch the value, the value will be written to cache
asynchronously and the value is returned.
* If a value is found in the cache, and it is newer than ```refreshAfter```, the cached value will be returned.
* If a value is found in the cache, but it is older than ```refreshAfter```, the cached value will be returned and the
 supplier will be called asynchronously to refresh the value in the cache.

If ```refreshAfter``` is longer than ```expireAfter``` or ```refreshAfter``` is ```null``` the object will simply expire
without ever being refreshed.

Note if the value is not requested before it expires it will also not be refreshed asynchronously. To keep a value cached
it will need to be requested periodically.


### Usage

To use the cache inject or autowire a ```SimpleRedisCacheFactory``` in your spring component and get a ```SimpleRedisCache```
using the ```forPool``` function of the factory.

Now to use the cache simply surround the code that produces the value you want to cache with a call to the cache

Before:
```kotlin
    fun expensiveMethod(id: String): MyObject {
        // code..
        return value
    }
```

After:
```kotlin
    fun expensiveMethod(id: String): MyObject =
        cache.cache(key = id, expireAfter = Duration.ofHours(1), refreshAfter = Duration.ofMinutes(45)) {
            // code..
            value
        }
```

```refreshAfter``` is optional and defaults to ```null```, if you do not want the refresh behaviour simply don't
supply a value:
```kotlin
    fun expensiveMethod(id: String): MyObject =
        cache.cache(key = id, expireAfter = Duration.ofHours(1)) {
            // code..
            value
        }
```


### Limitations

The cache uses Jackson to serialize the object to JSON. Only values that can be serialized and deserialized by
Jackson can be cached reliably. The cache by default uses the ObjectMapper set up in your spring context.
