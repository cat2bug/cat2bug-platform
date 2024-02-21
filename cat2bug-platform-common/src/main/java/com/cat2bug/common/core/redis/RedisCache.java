package com.cat2bug.common.core.redis;

import net.oschina.j2cache.CacheChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * spring redis 工具类
 *
 * @author ruoyi
 **/
@Component
public class RedisCache
{
    private final static String DEFAULT_CACHE_REGION = "default";

    /**
     * 验证码的缓存组名称
     */
    public final static String VERIFY_CODE_CACHE_REGION = "verifyCode";

    /**
     * 用户锁定时间
     */
    public final static String MEMBER_LOCAL_CACHE_REGION = "memberLocalTime";

    /**
     * 用户锁定时间
     */
    public final static String LOGIN_TOKEN_CACHE_REGION = "tokenExpireTime";

    /**
     * API缓存
     */
    public final static String API_TOKEN_CACHE_REGION = "apiToken";

    /**
     * 字典
     */
    public final static String DICT_CACHE_REGION = "dict";

    /**
     * 配置
     */
    public final static String CONFIG_CACHE_REGION = "config";

    /**
     * 配置
     */
    public final static String REPEAT_SUBMIT_CACHE_REGION = "apiRepeatSubmit";

    @Autowired
    private CacheChannel cacheChannel;
//    /**
//     * 缓存基本的对象，Integer、String、实体类等
//     *
//     * @param key 缓存的键值
//     * @param value 缓存的值
//     */
//    public <T> void setCacheObject(final String group, final String key, final T value)
//    {
//        cacheChannel.set(DEFAULT_CACHE_REGION,key, value);
////        redisTemplate.opsForValue().set(key, value);
//    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param timeout 时间
     * @param timeUnit 时间颗粒度
     */
//    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit)
//    {
//        cacheChannel.set();
//        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
//    }

    /**
     *
     * @param group 缓存数据的组，用于配置缓存时间,在caffeine.properties文件中配置
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String group, final String key, final T value)
    {
        cacheChannel.set(group, key, value);
    }
//    /**
//     * 设置有效时间
//     *
//     * @param key Redis键
//     * @param timeout 超时时间
//     * @return true=设置成功；false=设置失败
//     */
//    public boolean expire(final String key, final long timeout)
//    {
//        return expire(key, timeout, TimeUnit.SECONDS);
//    }
//
//    /**
//     * 设置有效时间
//     *
//     * @param key Redis键
//     * @param timeout 超时时间
//     * @param unit 时间单位
//     * @return true=设置成功；false=设置失败
//     */
//    public boolean expire(final String key, final long timeout, final TimeUnit unit)
//    {
//        return redisTemplate.expire(key, timeout, unit);
//    }

//    /**
//     * 获取有效时间
//     *
//     * @param key Redis键
//     * @return 有效时间
//     */
//    public long getExpire(final String key)
//    {
//        return redisTemplate.getExpire(key);
//    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(final String group, String key)
    {
        return cacheChannel.exists(group, key);
//        return redisTemplate.hasKey(key);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String group, final String key)
    {
        return (T)cacheChannel.get(group,key).getValue();
//        ValueOperations<String, T> operation = redisTemplate.opsForValue();
//        return operation.get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String group, final String key)
    {
        cacheChannel.evict(group,key);
        return true;
//        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final String group, final Collection collection)
    {
        String[] keys = (String[])collection.toArray();
        cacheChannel.evict(group,keys);
        return true;
//        return redisTemplate.delete(collection) > 0;
    }
//
//    /**
//     * 缓存List数据
//     *
//     * @param key 缓存的键值
//     * @param dataList 待缓存的List数据
//     * @return 缓存的对象
//     */
//    public <T> long setCacheList(final String key, final List<T> dataList)
//    {
//        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
//        return count == null ? 0 : count;
//    }
//
//    /**
//     * 获得缓存的list对象
//     *
//     * @param key 缓存的键值
//     * @return 缓存键值对应的数据
//     */
//    public <T> List<T> getCacheList(final String key)
//    {
//        return redisTemplate.opsForList().range(key, 0, -1);
//    }
//
//    /**
//     * 缓存Set
//     *
//     * @param key 缓存键值
//     * @param dataSet 缓存的数据
//     * @return 缓存数据的对象
//     */
//    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
//    {
//        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
//        Iterator<T> it = dataSet.iterator();
//        while (it.hasNext())
//        {
//            setOperation.add(it.next());
//        }
//        return setOperation;
//    }
//
//    /**
//     * 获得缓存的set
//     *
//     * @param key
//     * @return
//     */
//    public <T> Set<T> getCacheSet(final String key)
//    {
//        return redisTemplate.opsForSet().members(key);
//    }
//
//    /**
//     * 缓存Map
//     *
//     * @param key
//     * @param dataMap
//     */
//    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
//    {
//        if (dataMap != null) {
//            redisTemplate.opsForHash().putAll(key, dataMap);
//        }
//    }
//
//    /**
//     * 获得缓存的Map
//     *
//     * @param key
//     * @return
//     */
//    public <T> Map<String, T> getCacheMap(final String key)
//    {
//        return redisTemplate.opsForHash().entries(key);
//    }
//
//    /**
//     * 往Hash中存入数据
//     *
//     * @param key Redis键
//     * @param hKey Hash键
//     * @param value 值
//     */
//    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
//    {
//        redisTemplate.opsForHash().put(key, hKey, value);
//    }
//
//    /**
//     * 获取Hash中的数据
//     *
//     * @param key Redis键
//     * @param hKey Hash键
//     * @return Hash中的对象
//     */
//    public <T> T getCacheMapValue(final String key, final String hKey)
//    {
//        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
//        return opsForHash.get(key, hKey);
//    }
//
//    /**
//     * 获取多个Hash中的数据
//     *
//     * @param key Redis键
//     * @param hKeys Hash键集合
//     * @return Hash对象集合
//     */
//    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
//    {
//        return redisTemplate.opsForHash().multiGet(key, hKeys);
//    }
//
//    /**
//     * 删除Hash中的某条数据
//     *
//     * @param key Redis键
//     * @param hKey Hash键
//     * @return 是否成功
//     */
//    public boolean deleteCacheMapValue(final String key, final String hKey)
//    {
//        return redisTemplate.opsForHash().delete(key, hKey) > 0;
//    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String group, final String pattern)
    {
        return cacheChannel.keys(group).stream().filter(n->n.indexOf(pattern)>-1).collect(Collectors.toList());
//        return redisTemplate.keys(pattern);
    }
}
