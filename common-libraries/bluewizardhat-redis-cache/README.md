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

#### Web / HTTP Caching / Cache-Control header

If you are making a web app instead of autowiring ```SimpleRedisCacheFactory``` you can autowire ```SimpleRedisCacheFactoryWeb```
which will give you the option to also generate a ```Cache-Control``` header.

For example:
```kotlin
    fun expensiveMethod(id: String, response: HttpServletResponse): MyObject =
        cache
            .cacheControl(response, NoCache, NoStore, MaxAge0, MustRevalidate)
            .cache(key = id, expireAfter = Duration.ofHours(1), refreshAfter = Duration.ofMinutes(45)) {
                // code..
                value
        }
```
Will generate ```Cache-Control: no-cache, no-store, max-age=0, must-revalidate``` to prevent caching outside of the redis cache.

The Cache-Control directives that can be generated are

- no-cache
- no-store
- max-age
- s-maxage
- must-revalidate
- proxy-revalidate
- private
- public
- no-transform
- immutable

For ```max-age``` and ```s-maxage``` it is possible to choose different ways to set the value. When used with non-caching
headers like ```no-cache``` and ```no-store``` it will always be zero, but otherwise they can be set to the same value as
```expireAfter``` or ```refreshAfter``` and optionally the time already in the cache can be subtracted.

For example in:
```kotlin
    fun expensiveMethod(id: String, response: HttpServletResponse): MyObject =
        cache
            .cacheControl(response, MaxAgeExpireAfterAged)
            .cache(key = id, expireAfter = Duration.ofSeconds(1000)) {
                // code..
                value
        }
```
The directive ```MaxAgeExpireAfterAged``` will generate a ```max-age=<value>``` directive with the value being the same as
```expireAfter``` minus the time already in cache. So if the object had been in the cache for 250 seconds already and
```expireAfter``` is 1000 seconds as in the example the header generated would be ```Cache-Control: max-age=750```.

It is recommended that you take a look at the documentation on the
[Cache-Control header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control)
if you want to utilize this functionality optimally.


### Limitations

The cache uses Jackson to serialize the object to JSON. Only values that can be serialized and deserialized by
Jackson can be cached reliably. The cache by default uses the ObjectMapper set up in your spring context.
