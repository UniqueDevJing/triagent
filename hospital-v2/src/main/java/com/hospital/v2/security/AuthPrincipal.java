package com.hospital.v2.security;

/** JWT 中经过签名的最小身份，不携带手机号、证件号等敏感信息。 */
public record AuthPrincipal(long userId, String username, String role) {
}
