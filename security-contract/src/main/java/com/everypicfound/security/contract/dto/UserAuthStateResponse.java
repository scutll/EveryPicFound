package com.everypicfound.security.contract.dto;

/**
 * identity-service 返回给 gateway-service 的用户认证状态。
 *
 * @param userId 用户 ID
 * @param status 用户状态
 * @param authValidAfterEpochMilli 认证有效时间分界点
 */
public record UserAuthStateResponse(
        Long userId,
        String status,
        long authValidAfterEpochMilli) {
}