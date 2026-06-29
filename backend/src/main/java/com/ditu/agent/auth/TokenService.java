package com.ditu.agent.auth;

import com.ditu.agent.infra.DituProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * 轻量 Token 服务。
 *
 * <p>Token 只携带用户 id、角色、过期时间和类型，签名密钥来自环境变量，避免服务端保存会话状态。</p>
 */
@Service
public class TokenService {
  private final DituProperties properties;

  public TokenService(DituProperties properties) {
    this.properties = properties;
  }

  public String issueAccessToken(Long userId, String roleCode) {
    return issue(userId, roleCode, "ACCESS", properties.auth().accessTokenSeconds());
  }

  public String issueRefreshToken(Long userId, String roleCode) {
    return issue(userId, roleCode, "REFRESH", properties.auth().refreshTokenSeconds());
  }

  public ParsedToken parse(String token, String expectedType) {
    String[] parts = token.split("\\.");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Token 格式错误");
    }
    String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
    String expectedSign = sign(parts[0]);
    if (!constantTimeEquals(expectedSign, parts[1])) {
      throw new IllegalArgumentException("Token 签名无效");
    }
    String[] fields = payload.split(":");
    if (fields.length != 4 || !expectedType.equals(fields[3])) {
      throw new IllegalArgumentException("Token 类型无效");
    }
    long expiresAt = Long.parseLong(fields[2]);
    if (Instant.now().getEpochSecond() > expiresAt) {
      throw new IllegalArgumentException("Token 已过期");
    }
    return new ParsedToken(Long.parseLong(fields[0]), fields[1], expiresAt, fields[3]);
  }

  private String issue(Long userId, String roleCode, String type, long ttlSeconds) {
    long expiresAt = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
    String payload = userId + ":" + roleCode + ":" + expiresAt + ":" + type;
    String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    return encoded + "." + sign(encoded);
  }

  private String sign(String encodedPayload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(properties.auth().tokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Base64.getUrlEncoder().withoutPadding()
          .encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Token 签名失败", ex);
    }
  }

  private boolean constantTimeEquals(String left, String right) {
    byte[] a = left.getBytes(StandardCharsets.UTF_8);
    byte[] b = right.getBytes(StandardCharsets.UTF_8);
    if (a.length != b.length) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length; i++) {
      result |= a[i] ^ b[i];
    }
    return result == 0;
  }

  public record ParsedToken(Long userId, String roleCode, long expiresAt, String type) {
  }
}
