-- apply alter tables
alter table users add column if not exists login2_facode varchar(10);
alter table users add column if not exists login2_facode_expired_on timestamp;
alter table users add column if not exists last_send2_facode timestamp;
