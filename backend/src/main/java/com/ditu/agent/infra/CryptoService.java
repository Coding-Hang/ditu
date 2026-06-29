package com.ditu.agent.infra;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * 用户级模型密钥加密服务。
 *
 * <p>管理端保存模型链接时只落密文，运行时解析模型连接才在内存中短暂解密，避免密钥进入响应、SSE 或日志。</p>
 */
@Service
public class CryptoService {
  private static final String CIPHER = "AES/GCM/NoPadding";
  private static final int IV_BYTES = 12;
  private final SecureRandom secureRandom = new SecureRandom();
  private final SecretKeySpec secretKey;

  public CryptoService(DituProperties properties) {
    this.secretKey = new SecretKeySpec(deriveKey(properties.crypto().secret()), "AES");
  }

  public String encrypt(String plainText) {
    if (plainText == null || plainText.isBlank()) {
      return null;
    }
    try {
      byte[] iv = new byte[IV_BYTES];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      byte[] payload = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
      return Base64.getEncoder().encodeToString(payload);
    } catch (Exception ex) {
      throw new IllegalStateException("模型密钥加密失败", ex);
    }
  }

  public String decrypt(String cipherText) {
    if (cipherText == null || cipherText.isBlank()) {
      return null;
    }
    try {
      byte[] payload = Base64.getDecoder().decode(cipherText);
      byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
      byte[] encrypted = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
      Cipher cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new IllegalStateException("模型密钥解密失败", ex);
    }
  }

  private byte[] deriveKey(String rawSecret) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(rawSecret.getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new IllegalStateException("密钥派生失败", ex);
    }
  }
}
