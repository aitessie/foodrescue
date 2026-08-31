#!/bin/sh

set -e

create_database() {
  db_name="$1"
  db_user="$2"
  db_password="$3"

  psql \
    -v ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --set=db_name="$db_name" \
    --set=db_user="$db_user" \
    --set=db_password="$db_password" <<'EOSQL'

SELECT format(
    'CREATE ROLE %I LOGIN PASSWORD %L',
    :'db_user',
    :'db_password'
)
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = :'db_user'
)
\gexec

SELECT format(
    'CREATE DATABASE %I OWNER %I',
    :'db_name',
    :'db_user'
)
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_database
    WHERE datname = :'db_name'
)
\gexec

SELECT format(
    'REVOKE CONNECT ON DATABASE %I FROM PUBLIC',
    :'db_name'
)
\gexec

SELECT format(
    'GRANT CONNECT ON DATABASE %I TO %I',
    :'db_name',
    :'db_user'
)
\gexec

EOSQL
}

create_database \
  "$PARTNER_DB_NAME" \
  "$PARTNER_DB_USER" \
  "$PARTNER_DB_PASSWORD"

create_database \
  "$OFFER_DB_NAME" \
  "$OFFER_DB_USER" \
  "$OFFER_DB_PASSWORD"

create_database \
  "$KEYCLOAK_DB_NAME" \
  "$KEYCLOAK_DB_USER" \
  "$KEYCLOAK_DB_PASSWORD"
