
create extension if not exists "uuid-ossp";

create table account (
    id                      bigserial primary key not null,
    uuid                    uuid not null default uuid_generate_v4(),
    name                    varchar(64) not null,
    created                 timestamp with time zone not null default now(),
    version                 bigint not null default 1
);
create unique index idx_account_uuid on account (uuid);
