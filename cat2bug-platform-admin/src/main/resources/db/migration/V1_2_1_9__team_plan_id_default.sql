-- 新建团队未显式绑定时，库级默认普通团队套餐（plan_id=1）
UPDATE sys_team SET plan_id = 1 WHERE plan_id IS NULL;
ALTER TABLE sys_team ALTER COLUMN plan_id SET DEFAULT 1;
