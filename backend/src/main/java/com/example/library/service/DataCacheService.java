
package com.example.library.service;

import com.example.library.entity.Book;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataCacheService {
    
    private static final int MAX_CACHE_SIZE = 5;
    private final Map<String, CacheEntry> queryCache = Collections.synchronizedMap(
            new LinkedHashMap<String, CacheEntry>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
            });
    
    public List<Book> getCachedResult(String queryKey) {
        CacheEntry entry = queryCache.get(queryKey);
        if (entry != null) {
            entry.timestamp = System.currentTimeMillis();
            return entry.result;
        }
        return null;
    }
    
    public void cacheResult(String queryKey, List<Book> result) {
        queryCache.put(queryKey, new CacheEntry(result));
    }
    
    public void clearCache() {
        queryCache.clear();
    }
    
    public int getCacheSize() {
        return queryCache.size();
    }
    
    private static class CacheEntry {
        List<Book> result;
        long timestamp;
        
        CacheEntry(List<Book> result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
