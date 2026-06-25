package com.mercury.poker.network;

/**
 * 当前 Pod 标识，多实例时写入 Redis 房间元数据。
 */
public final class PodIdentity {

    private static final String POD_NAME = resolvePodName();

    private PodIdentity() {
    }

    public static String getPodName() {
        return POD_NAME;
    }

    private static String resolvePodName() {
        String podName = System.getenv("POD_NAME");
        if (podName == null || podName.isBlank()) {
            return "local";
        }
        return podName.trim();
    }
}
