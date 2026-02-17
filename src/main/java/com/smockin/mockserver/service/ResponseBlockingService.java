package com.smockin.mockserver.service;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.dto.BlockedPathToRelease;
import com.smockin.mockserver.dto.LiveBlockPath;
import com.smockin.mockserver.dto.LiveLoggingUserOverrideResponse;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ResponseBlockingService {

    private final Object responseBlockingMonitor = new Object();
    private final Map<String, Optional<LiveLoggingUserOverrideResponse>> responseAmendments = new HashMap<>();
    private final List<BlockedPathToRelease> userCallsToRelease = new ArrayList<>();
    private final AtomicBoolean liveBlockingModeEnabled = new AtomicBoolean();
    private final AtomicReference<List<LiveBlockPath>> liveBlockPathsRef = new AtomicReference<>(new ArrayList<>());

    public void releaseBlockedLiveLoggingResponse(final String traceId, final Optional<LiveLoggingUserOverrideResponse> responseAmendmentOpt) {
        synchronized (responseBlockingMonitor) {
            responseAmendments.put(traceId, responseAmendmentOpt);
            responseBlockingMonitor.notifyAll();
        }
    }

    public void updateLiveBlockingMode(final boolean liveBlockEnabled) {
        liveBlockingModeEnabled.set(liveBlockEnabled);
        if (!liveBlockingModeEnabled.get()) {
            synchronized (responseBlockingMonitor) {
                responseBlockingMonitor.notifyAll();
            }
        }
    }

    public void notifyBlockedLiveLoggingCalls(final RestMethodEnum method, final String userCtxOrFullPath) {
        final BlockedPathToRelease blockedPathToRelease = new BlockedPathToRelease(method, userCtxOrFullPath);
        synchronized (responseBlockingMonitor) {
            userCallsToRelease.add(blockedPathToRelease);
            responseBlockingMonitor.notifyAll();
        }
        Executors.newScheduledThreadPool(1).schedule(() -> {
            synchronized (responseBlockingMonitor) {
                userCallsToRelease.remove(blockedPathToRelease);
            }
        }, 8000, TimeUnit.MILLISECONDS);
    }

    public void addPathToLiveBlocking(final RestMethodEnum method, final String path, final String ownerUserId) throws com.smockin.admin.exception.ValidationException {
        if (liveBlockPathsRef.get().contains(new LiveBlockPath(method, path, ownerUserId))) {
            throw new com.smockin.admin.exception.ValidationException("This endpoint is already being blocked");
        }
        liveBlockPathsRef.get().add(new LiveBlockPath(method, path, ownerUserId));
    }

    public void removePathFromLiveBlocking(final RestMethodEnum method, final String path, final String ownerUserId) {
        liveBlockPathsRef.compareAndSet(liveBlockPathsRef.get(),
                liveBlockPathsRef.get().stream()
                        .filter(p -> !(Strings.CI.equals(p.getPath(), path)
                                && p.getMethod().equals(method)
                                && Strings.CI.equals(p.getOwnerUserId(), ownerUserId)))
                        .toList());
    }

    public long countLiveBlockingPathsForUser(final RestMethodEnum method, final String path, final String ownerUserId) {
        return liveBlockPathsRef.get().stream()
                .filter(p -> Strings.CI.equals(p.getPath(), path)
                        && p.getMethod().equals(method)
                        && Strings.CI.equals(p.getOwnerUserId(), ownerUserId))
                .count();
    }

    public void clearAllPathsFromLiveBlocking() {
        liveBlockPathsRef.get().clear();
    }

    public void clearAllPathsFromLiveBlockingForUser(final String ownerUserId) {
        liveBlockPathsRef.compareAndSet(liveBlockPathsRef.get(),
                liveBlockPathsRef.get().stream()
                        .filter(p -> !Strings.CI.equals(p.getOwnerUserId(), ownerUserId))
                        .toList());
        if (liveBlockPathsRef.get().isEmpty()) {
            updateLiveBlockingMode(false);
        }
    }

    public boolean isLiveBlockingModeEnabled() {
        return liveBlockingModeEnabled.get();
    }

    public boolean isPathBlocked(String method, String path) {
        return liveBlockPathsRef.get().stream()
                .anyMatch(p -> p.getMethod().name().equalsIgnoreCase(method)
                        && GeneralUtils.matchPaths(p.getPath(), path));
    }

    public Optional<LiveLoggingUserOverrideResponse> getAndRemoveAmendment(String traceId) {
        return responseAmendments.remove(traceId);
    }

    public boolean hasAmendment(String traceId) {
        return responseAmendments.containsKey(traceId);
    }

    public boolean shouldReleaseUserCall(HttpServletRequest request) {
        return userCallsToRelease.stream().anyMatch(p -> {
            if (p.getMethod() != null) {
                return request.getMethod().equalsIgnoreCase(p.getMethod().name())
                        && Strings.CS.equals(request.getPathInfo(), p.getPathPattern());
            }
            return Strings.CS.startsWith(request.getPathInfo(), p.getPathPattern());
        });
    }

    public Object getResponseBlockingMonitor() {
        return responseBlockingMonitor;
    }

}
