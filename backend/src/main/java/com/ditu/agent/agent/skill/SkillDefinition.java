package com.ditu.agent.agent.skill;

/**
 * Agent 咨询技能定义。
 *
 * <p>技能用于把商标、专利、版权和综合咨询问题路由到不同提示词域，避免所有知识产权问题混用一个意图。</p>
 */
public record SkillDefinition(String code, String name, String instruction) {
  public static SkillDefinition trademark() {
    return new SkillDefinition("TRADEMARK", "商标咨询", "处理商标检索、注册流程、驳回复审初步判断。");
  }

  public static SkillDefinition patent() {
    return new SkillDefinition("PATENT", "专利咨询", "处理专利申请材料、流程节点和保护范围初步判断。");
  }

  public static SkillDefinition copyright() {
    return new SkillDefinition("COPYRIGHT", "版权咨询", "处理软著、作品登记和版权材料准备。");
  }

  public static SkillDefinition general() {
    return new SkillDefinition("GENERAL", "综合咨询", "处理不能明确归类的知识产权综合问题。");
  }
}
