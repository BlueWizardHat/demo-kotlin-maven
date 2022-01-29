#!/bin/bash

# If you make any changes to this file remember to run "./runLocal.sh refresh".

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER template PASSWORD 'pg12345';
    CREATE DATABASE template;
    GRANT ALL PRIVILEGES ON DATABASE template TO template;
EOSQL
