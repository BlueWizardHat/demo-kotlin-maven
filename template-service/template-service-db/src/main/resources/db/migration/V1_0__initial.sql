
create table account (
    id                      bigserial primary key not null,
    name                    varchar(64) not null,
    version                 bigint not null default 1
);
