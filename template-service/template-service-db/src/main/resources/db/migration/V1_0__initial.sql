
create extension if not exists "uuid-ossp";

create table account (
    id                      uuid primary key not null default uuid_generate_v4(),
    name                    varchar(64) not null,
    created                 timestamp with time zone not null default now(),
    updated                 timestamp with time zone not null default now(),
    version                 bigint not null default 1
);
create index idx_account_created on account (created);
