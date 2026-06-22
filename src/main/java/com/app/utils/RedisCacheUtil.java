package com.app.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component("appRedisCacheUtil")
public class RedisCacheUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String DEFAULT_KEY_PREFIX = "app:";
    private static final String LOCK_PREFIX = "lock:";
    private static final String NULL_VALUE = "NULL_VALUE";

    private String getKey(String key) {
        return DEFAULT_KEY_PREFIX + key;
    }

    private String getLockKey(String key) {
        return DEFAULT_KEY_PREFIX + LOCK_PREFIX + key;
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(getKey(key), value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(getKey(key), value, timeout, unit);
    }

    public void setWithRandomExpire(String key, Object value, long baseTimeout, TimeUnit unit) {
        long randomFactor = (long) (baseTimeout * 0.1);
        long randomTimeout = baseTimeout + ThreadLocalRandom.current().nextLong(-randomFactor, randomFactor + 1);
        if (randomTimeout < 1) {
            randomTimeout = 1;
        }
        redisTemplate.opsForValue().set(getKey(key), value, randomTimeout, unit);
    }

    public void setWithNull(String key, Object value, long timeout, TimeUnit unit) {
        if (value == null) {
            redisTemplate.opsForValue().set(getKey(key), NULL_VALUE, 1, TimeUnit.MINUTES);
        } else {
            redisTemplate.opsForValue().set(getKey(key), value, timeout, unit);
        }
    }

    public void setWithNullAndRandomExpire(String key, Object value, long baseTimeout, TimeUnit unit) {
        if (value == null) {
            redisTemplate.opsForValue().set(getKey(key), NULL_VALUE, 1, TimeUnit.MINUTES);
        } else {
            setWithRandomExpire(key, value, baseTimeout, unit);
        }
    }

    public Object get(String key) {
        Object value = redisTemplate.opsForValue().get(getKey(key));
        if (NULL_VALUE.equals(value)) {
            return null;
        }
        return value;
    }

    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value != null && clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    public <T> T getWithLock(String key, Class<T> clazz, CacheLoader<T> loader, long timeout, TimeUnit unit) {
        T value = get(key, clazz);
        if (value != null) {
            return value;
        }

        String lockKey = getLockKey(key);
        try {
            boolean locked = tryLock(lockKey, 3, TimeUnit.SECONDS);
            if (locked) {
                try {
                    value = get(key, clazz);
                    if (value != null) {
                        return value;
                    }
                    value = loader.load();
                    if (value != null) {
                        setWithNullAndRandomExpire(key, value, timeout, unit);
                    } else {
                        setWithNullAndRandomExpire(key, null, timeout, unit);
                    }
                } finally {
                    unlock(lockKey);
                }
            } else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return getWithLock(key, clazz, loader, timeout, unit);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return loader.load();
        }
        return value;
    }

    public interface CacheLoader<T> {
        T load();
    }

    public boolean tryLock(String key, long expireTime, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(getLockKey(key), "1", expireTime, unit));
    }

    public void unlock(String key) {
        redisTemplate.delete(getLockKey(key));
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(getKey(key)));
    }

    public long delete(Collection<String> keys) {
        Set<String> prefixedKeys = keys.stream()
                .map(this::getKey)
                .collect(java.util.stream.Collectors.toSet());
        Long count = redisTemplate.delete(prefixedKeys);
        return count != null ? count : 0;
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getKey(key)));
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(getKey(key), timeout, unit));
    }

    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(getKey(key));
        return expire != null ? expire : -1;
    }

    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(getKey(key), hashKey, value);
    }

    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(getKey(key), hashKey);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(getKey(key));
    }

    public void hDelete(String key, Object... hashKeys) {
        redisTemplate.opsForHash().delete(getKey(key), hashKeys);
    }

    public boolean hHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(getKey(key), hashKey);
    }

    public long hSize(String key) {
        return redisTemplate.opsForHash().size(getKey(key));
    }

    public void lPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(getKey(key), value);
    }

    public void rPush(String key, Object value) {
        redisTemplate.opsForList().rightPush(getKey(key), value);
    }

    public Object lPop(String key) {
        return redisTemplate.opsForList().leftPop(getKey(key));
    }

    public Object rPop(String key) {
        return redisTemplate.opsForList().rightPop(getKey(key));
    }

    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(getKey(key), start, end);
    }

    public long lSize(String key) {
        return redisTemplate.opsForList().size(getKey(key));
    }

    public void sAdd(String key, Object... values) {
        redisTemplate.opsForSet().add(getKey(key), values);
    }

    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(getKey(key));
    }

    public boolean sIsMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(getKey(key), value));
    }

    public long sSize(String key) {
        return redisTemplate.opsForSet().size(getKey(key));
    }

    public long sRemove(String key, Object... values) {
        Long count = redisTemplate.opsForSet().remove(getKey(key), values);
        return count != null ? count : 0;
    }

    public void zAdd(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(getKey(key), value, score);
    }

    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(getKey(key), start, end);
    }

    public Set<Object> zReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(getKey(key), start, end);
    }

    public long zSize(String key) {
        return redisTemplate.opsForZSet().size(getKey(key));
    }

    public long zRemove(String key, Object... values) {
        Long count = redisTemplate.opsForZSet().remove(getKey(key), values);
        return count != null ? count : 0;
    }

    public void clearAll() {
        Set<String> keys = redisTemplate.keys(DEFAULT_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void clearByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(DEFAULT_KEY_PREFIX + pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
