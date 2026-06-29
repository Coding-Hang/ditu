package com.ditu.agent;

import com.ditu.agent.infra.DituProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 滴兔智能体唯一 Spring Boot 启动入口。
 *
 * <p>文档要求后端只保留一个 Maven 工程和一个应用，本类负责装配 auth、user、chat、agent、rag、ticket、
 * admin、infra、common 包下的全部能力，避免业务被拆散到多个 Java 子模块。</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(DituProperties.class)
public class DituAgentApplication {

  public static void main(String[] args) {
    SpringApplication.run(DituAgentApplication.class, args);
  }
}
