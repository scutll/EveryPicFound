package com.everypicfound.security.contract;


/**
 * 服务间共享的权限范围常量。
 */
public class SecurityScopes {

    public static final String IMAGE_SEARCH = "image:search";
    public static final String IMAGE_UPLOAD = "image:upload";
    public static final String IMAGE_READ = "image:read";

    public static final String USER_READ = "user:read";
    public static final String USER_WRITE = "user:write";

    public static final String SESSION_READ = "session:read";
    public static final String SESSION_REVOKE = "session:revoke";

    private SecurityScopes() {
    }
}
