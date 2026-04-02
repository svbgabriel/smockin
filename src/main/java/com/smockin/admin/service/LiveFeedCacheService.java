package com.smockin.admin.service;

import com.smockin.admin.dto.response.LiveLoggingDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class LiveFeedCacheService {

    private static final int MAX_CACHE_SIZE = 100;
    private final ConcurrentLinkedDeque<LiveLoggingDTO> cache = new ConcurrentLinkedDeque<>();

    public void add(LiveLoggingDTO dto) {
        cache.addFirst(dto);
        while (cache.size() > MAX_CACHE_SIZE) {
            cache.removeLast();
        }
    }

    public List<LiveLoggingDTO> getCachedLogs() {
        return new ArrayList<>(cache);
    }

    public void clear() {
        cache.clear();
    }
}
