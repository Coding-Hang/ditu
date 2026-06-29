-- DEV-001: 滴兔智能体核心库表迁移，注释说明权限边界、次数流水、模型密钥和 RAG 过滤等业务规则。
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE app_user (
  id                BIGSERIAL PRIMARY KEY,
  username          VARCHAR(64) UNIQUE NOT NULL,
  phone             VARCHAR(32),
  email             VARCHAR(128),
  password_hash     VARCHAR(255) NOT NULL,
  display_name      VARCHAR(80) NOT NULL,
  avatar_url        VARCHAR(512),
  role_code         VARCHAR(32) NOT NULL DEFAULT 'USER',
  status            VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  plan_code         VARCHAR(32) NOT NULL DEFAULT 'BASIC',
  quota_total       INTEGER NOT NULL DEFAULT 0,
  quota_used        INTEGER NOT NULL DEFAULT 0,
  quota_reset_at    TIMESTAMPTZ,
  wechat_openid     VARCHAR(128),
  last_login_at     TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_app_user_role CHECK (role_code IN ('USER', 'ADMIN', 'RAG_ADMIN', 'CS_MANAGER')),
  CONSTRAINT ck_app_user_status CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED')),
  CONSTRAINT ck_app_user_quota CHECK (quota_total >= 0 AND quota_used >= 0 AND quota_used <= quota_total)
);
CREATE INDEX idx_app_user_phone ON app_user(phone);
CREATE INDEX idx_app_user_status ON app_user(status);
CREATE INDEX idx_app_user_plan ON app_user(plan_code);
COMMENT ON TABLE app_user IS '系统账号表，承载用户端和管理端身份、角色、状态、套餐与次数快照。';
COMMENT ON COLUMN app_user.password_hash IS 'BCrypt 密码哈希，禁止保存明文密码。';
COMMENT ON COLUMN app_user.role_code IS '角色决定管理端权限，ADMIN 拥有全部管理能力。';
COMMENT ON COLUMN app_user.status IS '账号状态，DISABLED 或 LOCKED 禁止登录和发起会话。';
COMMENT ON COLUMN app_user.quota_total IS '用户总次数快照，所有变化必须在 quota_ledger 中有流水。';
COMMENT ON COLUMN app_user.quota_used IS '已成功提交扣减的次数，预占和回滚不增加该字段。';

CREATE TABLE subscription_plan (
  code              VARCHAR(32) PRIMARY KEY,
  name              VARCHAR(64) NOT NULL,
  level_order       INTEGER NOT NULL,
  monthly_quota     INTEGER NOT NULL DEFAULT 0,
  rag_enabled       BOOLEAN NOT NULL DEFAULT false,
  priority_support  BOOLEAN NOT NULL DEFAULT false,
  description       TEXT,
  benefits          JSONB NOT NULL DEFAULT '[]'::jsonb,
  enabled           BOOLEAN NOT NULL DEFAULT true,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE subscription_plan IS '套餐配置表，用户端和管理端共同展示 BASIC、PRO、PLUS 三档权益。';
COMMENT ON COLUMN subscription_plan.benefits IS '套餐权益 JSON，前端直接渲染为权益列表。';
INSERT INTO subscription_plan(code, name, level_order, monthly_quota, rag_enabled, priority_support, description, benefits)
VALUES
  ('BASIC', '基础', 1, 30, false, false, '适合低频知识产权咨询用户。', '["基础智能问答", "通用知识库"]'::jsonb),
  ('PRO', 'Pro', 2, 300, true, false, '适合中小企业日常知识产权咨询。', '["多轮智能问答", "通用知识库", "用户专属知识库"]'::jsonb),
  ('PLUS', 'Plus', 3, 1000, true, true, '适合高频咨询和重点客户。', '["高次数额度", "用户专属知识库", "优先专属客服"]'::jsonb);

CREATE TABLE app_user_model_config (
  id                  BIGSERIAL PRIMARY KEY,
  user_id             BIGINT NOT NULL REFERENCES app_user(id),
  config_name         VARCHAR(80) NOT NULL,
  provider_code       VARCHAR(64) NOT NULL DEFAULT 'CUSTOM',
  base_url            VARCHAR(1024) NOT NULL,
  model_name          VARCHAR(128) NOT NULL,
  auth_type           VARCHAR(32) NOT NULL DEFAULT 'API_KEY',
  api_key_ciphertext  TEXT,
  enabled             BOOLEAN NOT NULL DEFAULT true,
  last_test_status    VARCHAR(32) NOT NULL DEFAULT 'UNTESTED',
  last_test_message   TEXT,
  last_test_at        TIMESTAMPTZ,
  created_by          BIGINT REFERENCES app_user(id),
  updated_by          BIGINT REFERENCES app_user(id),
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uk_user_model_config_user UNIQUE(user_id),
  CONSTRAINT ck_user_model_auth_type CHECK (auth_type IN ('NONE', 'API_KEY', 'BEARER')),
  CONSTRAINT ck_user_model_test_status CHECK (last_test_status IN ('UNTESTED', 'SUCCESS', 'FAILED'))
);
CREATE INDEX idx_user_model_config_enabled ON app_user_model_config(user_id, enabled);
COMMENT ON TABLE app_user_model_config IS '用户级大模型链接配置表，一个用户最多一条，启用后覆盖平台默认模型。';
COMMENT ON COLUMN app_user_model_config.api_key_ciphertext IS '模型密钥密文，接口响应和日志不得输出明文。';
COMMENT ON COLUMN app_user_model_config.enabled IS '启用配置优先用于 Agent run，停用后回退平台默认模型链接。';

CREATE TABLE quota_ledger (
  id                BIGSERIAL PRIMARY KEY,
  user_id           BIGINT NOT NULL REFERENCES app_user(id),
  change_type       VARCHAR(32) NOT NULL,
  delta_count       INTEGER NOT NULL,
  before_total      INTEGER NOT NULL,
  before_used       INTEGER NOT NULL,
  after_total       INTEGER NOT NULL,
  after_used        INTEGER NOT NULL,
  reason            VARCHAR(255) NOT NULL,
  ref_type          VARCHAR(64),
  ref_id            BIGINT,
  operator_user_id  BIGINT REFERENCES app_user(id),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_quota_change_type CHECK (change_type IN ('ALLOCATE', 'DEDUCT', 'RESERVE', 'COMMIT', 'ROLLBACK', 'RESET', 'ADJUST'))
);
CREATE INDEX idx_quota_ledger_user_time ON quota_ledger(user_id, created_at DESC);
CREATE INDEX idx_quota_ledger_ref ON quota_ledger(ref_type, ref_id);
COMMENT ON TABLE quota_ledger IS '用户次数流水表，所有次数变化包括预占、提交、回滚、调整都必须写入。';
COMMENT ON COLUMN quota_ledger.change_type IS '次数变化类型，Agent 成功用 COMMIT，失败用 ROLLBACK，管理调整用 ADJUST。';
COMMENT ON COLUMN quota_ledger.operator_user_id IS '操作人，用户会话扣次为用户本人，管理调整为管理员。';

CREATE TABLE conversation (
  id                BIGSERIAL PRIMARY KEY,
  user_id           BIGINT NOT NULL REFERENCES app_user(id),
  title             VARCHAR(160) NOT NULL DEFAULT '新会话',
  status            VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  last_message_at   TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_conversation_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);
CREATE INDEX idx_conversation_user_time ON conversation(user_id, updated_at DESC);
COMMENT ON TABLE conversation IS '用户会话表，所有用户端查询必须通过 user_id 做归属过滤。';

CREATE TABLE chat_message (
  id                BIGSERIAL PRIMARY KEY,
  conversation_id   BIGINT NOT NULL REFERENCES conversation(id),
  user_id           BIGINT NOT NULL REFERENCES app_user(id),
  role              VARCHAR(32) NOT NULL,
  sequence_no       INTEGER NOT NULL,
  content           TEXT NOT NULL,
  content_format    VARCHAR(32) NOT NULL DEFAULT 'MARKDOWN',
  agent_run_id      BIGINT,
  token_count       INTEGER,
  quota_cost        INTEGER NOT NULL DEFAULT 0,
  metadata          JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_chat_message_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
  UNIQUE(conversation_id, sequence_no)
);
CREATE INDEX idx_chat_message_conversation_seq ON chat_message(conversation_id, sequence_no ASC);
COMMENT ON TABLE chat_message IS '会话消息表，sequence_no 用于恢复多轮上下文顺序。';
COMMENT ON COLUMN chat_message.quota_cost IS '助手消息对应成功扣减次数，用户消息不直接扣次。';

CREATE TABLE agent_run (
  id                BIGSERIAL PRIMARY KEY,
  conversation_id   BIGINT NOT NULL REFERENCES conversation(id),
  user_id           BIGINT NOT NULL REFERENCES app_user(id),
  user_message_id   BIGINT REFERENCES chat_message(id),
  model_config_id   BIGINT REFERENCES app_user_model_config(id),
  model_name        VARCHAR(128),
  status            VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
  quota_reserved    INTEGER NOT NULL DEFAULT 1,
  started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at      TIMESTAMPTZ,
  error_code        VARCHAR(64),
  error_message     TEXT,
  metadata          JSONB NOT NULL DEFAULT '{}'::jsonb,
  CONSTRAINT ck_agent_run_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED', 'HITL_WAITING'))
);
CREATE INDEX idx_agent_run_conversation ON agent_run(conversation_id, started_at DESC);
CREATE INDEX idx_agent_run_user_status ON agent_run(user_id, status);
CREATE INDEX idx_agent_run_model_config ON agent_run(model_config_id);
ALTER TABLE chat_message ADD CONSTRAINT fk_chat_message_agent_run FOREIGN KEY(agent_run_id) REFERENCES agent_run(id);
COMMENT ON TABLE agent_run IS 'Agent 单次运行表，记录本次实际使用的用户级或平台默认模型。';
COMMENT ON COLUMN agent_run.model_config_id IS '用户级模型配置 ID，使用平台默认模型时为空。';
COMMENT ON COLUMN agent_run.status IS '运行状态，失败、取消或超时时必须回滚预占次数。';

CREATE TABLE agent_run_event (
  id                BIGSERIAL PRIMARY KEY,
  run_id            BIGINT NOT NULL REFERENCES agent_run(id),
  conversation_id   BIGINT NOT NULL REFERENCES conversation(id),
  event_type        VARCHAR(64) NOT NULL,
  sequence_no       INTEGER NOT NULL,
  visible_to_user   BOOLEAN NOT NULL DEFAULT true,
  payload           JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(run_id, sequence_no)
);
CREATE INDEX idx_agent_event_run_seq ON agent_run_event(run_id, sequence_no ASC);
CREATE INDEX idx_agent_event_type ON agent_run_event(event_type);
COMMENT ON TABLE agent_run_event IS 'Agent 事件表，SSE 输出与数据库事件保持一致，可用于断线补发。';
COMMENT ON COLUMN agent_run_event.sequence_no IS 'SSE id，Last-Event-ID 依赖该序号补发未消费事件。';

CREATE TABLE customer_service_profile (
  id                BIGSERIAL PRIMARY KEY,
  service_type      VARCHAR(64) NOT NULL,
  name              VARCHAR(80) NOT NULL,
  role_name         VARCHAR(80) NOT NULL,
  positioning       VARCHAR(255) NOT NULL,
  intro             TEXT NOT NULL,
  avatar_url        VARCHAR(512),
  enabled           BOOLEAN NOT NULL DEFAULT true,
  sort_order        INTEGER NOT NULL DEFAULT 100,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cs_profile_type_enabled ON customer_service_profile(service_type, enabled);
COMMENT ON TABLE customer_service_profile IS '专属客服展示配置，用户端据此选择商标、专利、版权或综合咨询客服。';
INSERT INTO customer_service_profile(service_type, name, role_name, positioning, intro, avatar_url, sort_order)
VALUES
  ('TRADEMARK', '商标顾问小滴', '商标注册顾问', '商标检索、注册流程、驳回复审初步判断', '处理商标申请准备、材料清单、流程节点等问题。', '/assets/cs/trademark.png', 10),
  ('PATENT', '专利顾问小兔', '专利申请顾问', '专利申请材料、流程节点、保护范围初步判断', '处理专利申请准备、流程跟踪和常见材料问题。', '/assets/cs/patent.png', 20),
  ('COPYRIGHT', '版权顾问小知', '版权登记顾问', '软著、作品登记、版权材料准备', '处理软件著作权、作品版权登记和材料核对。', '/assets/cs/copyright.png', 30),
  ('GENERAL', '综合顾问小企', '知识产权综合顾问', '综合知识产权咨询与人工协调', '处理不能明确归类的问题，并协调人工服务。', '/assets/cs/general.png', 40);

CREATE TABLE support_ticket (
  id                  BIGSERIAL PRIMARY KEY,
  user_id             BIGINT NOT NULL REFERENCES app_user(id),
  service_profile_id  BIGINT REFERENCES customer_service_profile(id),
  conversation_id     BIGINT REFERENCES conversation(id),
  title               VARCHAR(160) NOT NULL,
  content             TEXT NOT NULL,
  status              VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  priority            VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  assigned_to         BIGINT REFERENCES app_user(id),
  last_message_at     TIMESTAMPTZ,
  closed_at           TIMESTAMPTZ,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_ticket_status CHECK (status IN ('OPEN', 'PENDING', 'PROCESSING', 'RESOLVED', 'CLOSED')),
  CONSTRAINT ck_ticket_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'))
);
CREATE INDEX idx_ticket_user_time ON support_ticket(user_id, created_at DESC);
CREATE INDEX idx_ticket_status_time ON support_ticket(status, updated_at DESC);
COMMENT ON TABLE support_ticket IS '专属客服工单表，用户只能访问自己的工单，管理端可分配和处理。';
COMMENT ON COLUMN support_ticket.status IS '工单状态，CLOSED 后禁止用户和管理端继续追加消息。';

CREATE TABLE support_ticket_message (
  id                BIGSERIAL PRIMARY KEY,
  ticket_id         BIGINT NOT NULL REFERENCES support_ticket(id),
  sender_user_id    BIGINT NOT NULL REFERENCES app_user(id),
  sender_role       VARCHAR(32) NOT NULL,
  content           TEXT NOT NULL,
  attachments       JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_ticket_msg_sender_role CHECK (sender_role IN ('USER', 'ADMIN', 'CS_MANAGER', 'AGENT'))
);
CREATE INDEX idx_ticket_msg_ticket_time ON support_ticket_message(ticket_id, created_at ASC);
COMMENT ON TABLE support_ticket_message IS '工单消息表，记录用户、管理员、客服主管和 Agent 转人工消息。';

CREATE TABLE rag_collection (
  id                BIGSERIAL PRIMARY KEY,
  scope             VARCHAR(32) NOT NULL,
  owner_user_id     BIGINT REFERENCES app_user(id),
  name              VARCHAR(128) NOT NULL,
  description       TEXT,
  enabled           BOOLEAN NOT NULL DEFAULT true,
  created_by        BIGINT REFERENCES app_user(id),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_rag_collection_scope CHECK (scope IN ('GLOBAL', 'USER')),
  CONSTRAINT ck_rag_collection_owner CHECK (
    (scope = 'GLOBAL' AND owner_user_id IS NULL)
    OR (scope = 'USER' AND owner_user_id IS NOT NULL)
  )
);
CREATE INDEX idx_rag_collection_scope_owner ON rag_collection(scope, owner_user_id, enabled);
COMMENT ON TABLE rag_collection IS 'RAG 知识库，GLOBAL 所有用户可检索，USER 仅 owner_user_id 用户可检索。';
COMMENT ON COLUMN rag_collection.owner_user_id IS 'USER 知识库所属用户，检索时必须与当前用户匹配。';

CREATE TABLE rag_document (
  id                BIGSERIAL PRIMARY KEY,
  collection_id     BIGINT NOT NULL REFERENCES rag_collection(id),
  source_type       VARCHAR(32) NOT NULL DEFAULT 'UPLOAD',
  file_name         VARCHAR(255) NOT NULL,
  file_path         VARCHAR(1024) NOT NULL,
  mime_type         VARCHAR(128),
  checksum_sha256   VARCHAR(128) NOT NULL,
  status            VARCHAR(32) NOT NULL DEFAULT 'UPLOADED',
  metadata          JSONB NOT NULL DEFAULT '{}'::jsonb,
  uploaded_by       BIGINT REFERENCES app_user(id),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_rag_document_status CHECK (status IN ('UPLOADED', 'PARSING', 'READY', 'FAILED', 'DISABLED'))
);
CREATE INDEX idx_rag_document_collection_status ON rag_document(collection_id, status);
CREATE UNIQUE INDEX uk_rag_document_checksum ON rag_document(collection_id, checksum_sha256);
COMMENT ON TABLE rag_document IS 'RAG 文档表，只有 READY 文档参与检索，DISABLED 和 FAILED 不参与召回。';
COMMENT ON COLUMN rag_document.file_path IS '服务器存储路径，用户端接口不得返回该字段。';

CREATE TABLE rag_chunk (
  id                BIGSERIAL PRIMARY KEY,
  document_id       BIGINT NOT NULL REFERENCES rag_document(id) ON DELETE CASCADE,
  collection_id     BIGINT NOT NULL REFERENCES rag_collection(id),
  chunk_index       INTEGER NOT NULL,
  content           TEXT NOT NULL,
  content_hash      VARCHAR(128) NOT NULL,
  embedding         vector(1536) NOT NULL,
  metadata          JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(document_id, chunk_index)
);
CREATE INDEX idx_rag_chunk_collection ON rag_chunk(collection_id);
CREATE INDEX idx_rag_chunk_embedding ON rag_chunk USING ivfflat (embedding vector_cosine_ops);
COMMENT ON TABLE rag_chunk IS 'RAG 切片表，embedding 固定为 vector(1536)，检索必须关联 rag_collection 做权限过滤。';

CREATE TABLE rag_ingest_job (
  id                BIGSERIAL PRIMARY KEY,
  document_id       BIGINT NOT NULL REFERENCES rag_document(id),
  status            VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  error_message     TEXT,
  started_at        TIMESTAMPTZ,
  completed_at      TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_rag_job_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'))
);
COMMENT ON TABLE rag_ingest_job IS 'RAG 入库任务表，跟踪文档解析、切片和 Embedding 写入结果。';

CREATE TABLE audit_log (
  id                BIGSERIAL PRIMARY KEY,
  actor_user_id     BIGINT REFERENCES app_user(id),
  action            VARCHAR(128) NOT NULL,
  target_type       VARCHAR(64) NOT NULL,
  target_id         BIGINT,
  detail            JSONB NOT NULL DEFAULT '{}'::jsonb,
  ip_address        VARCHAR(64),
  user_agent        VARCHAR(512),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_actor_time ON audit_log(actor_user_id, created_at DESC);
CREATE INDEX idx_audit_target ON audit_log(target_type, target_id);
COMMENT ON TABLE audit_log IS '管理端敏感操作审计日志，覆盖用户、次数、模型链接、工单和 RAG 操作。';
COMMENT ON COLUMN audit_log.detail IS '审计详情 JSON，禁止写入模型密钥明文。';
