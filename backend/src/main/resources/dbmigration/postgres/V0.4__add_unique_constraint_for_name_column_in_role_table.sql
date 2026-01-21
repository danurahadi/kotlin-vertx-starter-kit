-- apply post alter
alter table roles add constraint uq_roles_name unique  (name);
