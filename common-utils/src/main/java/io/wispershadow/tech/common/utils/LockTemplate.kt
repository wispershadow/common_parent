package io.wispershadow.tech.common.utils

import java.util.concurrent.locks.ReadWriteLock

object LockTemplate {
    inline fun <K, V> lockForCacheLoad(readWriteLock: ReadWriteLock,
                                       key: K, cacheMap: MutableMap<K, V>,
                                       cacheDataLoadFun: (K) -> V): V? {
        var result: V? = null
        readWriteLock.readLock().lock()
        result = cacheMap[key]
        if (result == null) {
            readWriteLock.readLock().unlock()
            readWriteLock.writeLock().lock()
            try {
                result = cacheMap[key]
                if (result == null) {
                    result = cacheDataLoadFun.invoke(key)
                    cacheMap[key] = result
                }
                readWriteLock.readLock().lock()
            }
            finally {
                readWriteLock.writeLock().unlock()
            }
        }
        try {
            return result
        }
        finally {
            readWriteLock.readLock().unlock()
        }
    }
}