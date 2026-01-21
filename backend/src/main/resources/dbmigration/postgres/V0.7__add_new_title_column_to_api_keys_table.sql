-- apply alter tables
alter table api_keys add column if not exists title varchar(150) default 'Internal Client App';
alter table api_keys add column if not exists created_by_id bigint;
-- foreign keys and indices
create index ix_api_keys_created_by_id on api_keys (created_by_id);
alter table api_keys add constraint fk_api_keys_created_by_id foreign key (created_by_id) references admins (id) on delete restrict on update restrict;

