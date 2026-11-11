package com.hospital.v2.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** 轻量 HMAC-SHA256 JWT，实现无状态登录，不把密钥或隐私字段写入令牌。 */
@Service
public class TokenService {
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private final ObjectMapper json;
    private final byte[] secret;
    private final long tokenSeconds;

    public TokenService(ObjectMapper json,
                        @Value("${hospital.auth.jwt-secret}") String secret,
                        @Value("${hospital.auth.token-hours:8}") long tokenHours) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("hospital.auth.jwt-secret 至少需要 32 个字符");
        }
        this.json = json;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.tokenSeconds = Math.max(1, tokenHours) * 3600;
    }

    public String issue(AuthPrincipal p) {
        try {
            String header = enc(json.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("sub", p.username());
            claims.put("uid", p.userId());
            claims.put("role", p.role());
            claims.put("iat", Instant.now().getEpochSecond());
            claims.put("exp", Instant.now().getEpochSecond() + tokenSeconds);
            String payload = enc(json.writeValueAsBytes(claims));
            String unsigned = header + "." + payload;
            return unsigned + "." + enc(sign(unsigned));
        } catch (Exception e) {
            throw new IllegalStateException("令牌生成失败", e);
        }
    }

    public AuthPrincipal verify(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("令牌格式错误");
            String unsigned = parts[0] + "." + parts[1];
            if (!java.security.MessageDigest.isEqual(sign(unsigned), B64D.decode(parts[2]))) {
                throw new IllegalArgumentException("令牌签名无效");
            }
            Map<String, Object> c = json.readValue(B64D.decode(parts[1]), new TypeReference<>() {});
            long exp = ((Number) c.get("exp")).longValue();
            if (exp <= Instant.now().getEpochSecond()) throw new IllegalArgumentException("令牌已过期");
            return new AuthPrincipal(((Number) c.get("uid")).longValue(),
                    String.valueOf(c.get("sub")), String.valueOf(c.get("role")));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("令牌无效", e);
        }
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.US_ASCII));
    }

    private String enc(byte[] value) { return B64.encodeToString(value); }
}
