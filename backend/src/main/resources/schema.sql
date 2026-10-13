CREATE TABLE IF NOT EXISTS app_user (
 id VARCHAR(36) PRIMARY KEY, openid VARCHAR(100) NOT NULL UNIQUE,
 nickname VARCHAR(80) NOT NULL, created_at TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS auth_session (
 token_hash VARCHAR(64) PRIMARY KEY, user_id VARCHAR(36) NOT NULL,
 expires_at TIMESTAMP NOT NULL, FOREIGN KEY (user_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS exam_template (
 code VARCHAR(30) PRIMARY KEY, name VARCHAR(80) NOT NULL,
 version VARCHAR(20) NOT NULL, subjects_json TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS study_plan (
 id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL,
 exam_type VARCHAR(30) NOT NULL, exam_name VARCHAR(80) NOT NULL,
 exam_date DATE NOT NULL, start_date DATE NOT NULL, daily_minutes INT NOT NULL,
 foundation VARCHAR(20) NOT NULL, subjects_json TEXT NOT NULL,
 created_at TIMESTAMP NOT NULL,
 FOREIGN KEY (user_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS study_plan_stage (
 id VARCHAR(36) PRIMARY KEY, plan_id VARCHAR(36) NOT NULL,
 stage_name VARCHAR(40) NOT NULL, goal VARCHAR(300) NOT NULL,
 start_date DATE NOT NULL, end_date DATE NOT NULL, sort_order INT NOT NULL,
 FOREIGN KEY (plan_id) REFERENCES study_plan(id)
);
CREATE TABLE IF NOT EXISTS study_task (
 id VARCHAR(36) PRIMARY KEY, plan_id VARCHAR(36) NOT NULL,
 stage_id VARCHAR(36) NOT NULL, task_date DATE NOT NULL,
 subject VARCHAR(80) NOT NULL, task_name VARCHAR(200) NOT NULL,
 duration INT NOT NULL, completed BOOLEAN NOT NULL DEFAULT FALSE,
 completed_at TIMESTAMP NULL,
 FOREIGN KEY (plan_id) REFERENCES study_plan(id),
 FOREIGN KEY (stage_id) REFERENCES study_plan_stage(id)
);
CREATE TABLE IF NOT EXISTS product (
 id VARCHAR(30) PRIMARY KEY, name VARCHAR(80) NOT NULL,
 price_fen INT NOT NULL, description VARCHAR(500) NOT NULL
);
CREATE TABLE IF NOT EXISTS order_info (
 id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL,
 plan_id VARCHAR(36) NOT NULL, product_id VARCHAR(30) NOT NULL,
 amount_fen INT NOT NULL, status VARCHAR(20) NOT NULL,
 created_at TIMESTAMP NOT NULL,
 FOREIGN KEY (user_id) REFERENCES app_user(id),
 FOREIGN KEY (plan_id) REFERENCES study_plan(id),
 FOREIGN KEY (product_id) REFERENCES product(id)
);
