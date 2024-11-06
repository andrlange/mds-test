#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
-- 01-create-database.sql
SELECT 'CREATE DATABASE demo'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'demo')\gexec

-- 02-setup-extensions.sql
\c demo
CREATE EXTENSION IF NOT EXISTS dblink;

-- Connect to the "demo" database
\c demo;
-- Step 1: Create users if they do not exist

SELECT 'CREATE ROLE user_one WITH LOGIN PASSWORD ''password_one'''
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'user_one')\gexec

SELECT 'CREATE ROLE user_two WITH LOGIN PASSWORD ''password_two'''
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'user_two')\gexec

SELECT 'CREATE ROLE user_three WITH LOGIN PASSWORD ''password_three'''
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'user_three')\gexec

-- Step 2: Create schemas if they do not exist and assign ownership to respective users
CREATE SCHEMA IF NOT EXISTS schema_user_one AUTHORIZATION user_one;
CREATE SCHEMA IF NOT EXISTS schema_user_two AUTHORIZATION user_two;
CREATE SCHEMA IF NOT EXISTS schema_user_three AUTHORIZATION user_three;

-- Step 3: Restrict each user's access to only their own schema
REVOKE ALL ON SCHEMA public FROM user_one, user_two, user_three;

-- Grant usage on each specific schema for each user
GRANT USAGE ON SCHEMA schema_user_one TO user_one;
GRANT USAGE ON SCHEMA schema_user_two TO user_two;
GRANT USAGE ON SCHEMA schema_user_three TO user_three;

-- Prevent users from accessing other schemas
REVOKE ALL ON SCHEMA schema_user_two, schema_user_three FROM user_one;
REVOKE ALL ON SCHEMA schema_user_one, schema_user_three FROM user_two;
REVOKE ALL ON SCHEMA schema_user_three, schema_user_two FROM user_three;

-- Step 4: Create a demo_data table in each schema with restricted access
SELECT format('
    CREATE TABLE IF NOT EXISTS schema_user_one.demo_data (
        id SERIAL PRIMARY KEY,
        field1 VARCHAR(50),
        field2 VARCHAR(50)
    )'
)
WHERE NOT EXISTS (
    SELECT FROM information_schema.tables
    WHERE table_schema = 'schema_user_one'
    AND table_name = 'demo_data'
)\gexec

SELECT 'GRANT ALL PRIVILEGES ON TABLE schema_user_one.demo_data TO user_one'
WHERE EXISTS (
    SELECT FROM information_schema.tables
    WHERE table_schema = 'schema_user_one'
    AND table_name = 'demo_data'
)\gexec

SELECT format('
    CREATE TABLE IF NOT EXISTS schema_user_two.demo_data (
        id SERIAL PRIMARY KEY,
        field1 VARCHAR(50),
        field2 VARCHAR(50)
    )'
)
WHERE NOT EXISTS (
    SELECT FROM information_schema.tables
    WHERE table_schema = 'schema_user_two'
    AND table_name = 'demo_data'
)\gexec

SELECT 'GRANT ALL PRIVILEGES ON TABLE schema_user_two.demo_data TO user_two'
WHERE EXISTS (
    SELECT FROM information_schema.tables
    WHERE table_schema = 'schema_user_two'
    AND table_name = 'demo_data'
)\gexec

SELECT format('
    CREATE TABLE IF NOT EXISTS schema_user_three.demo_data (
        id SERIAL PRIMARY KEY,
        field1 VARCHAR(50),
        field2 VARCHAR(50)
    )'
)
WHERE NOT EXISTS (
    SELECT FROM information_schema.tables
    WHERE table_schema = 'schema_user_three'
    AND table_name = 'demo_data'
)\gexec

SELECT 'GRANT ALL PRIVILEGES ON TABLE schema_user_three.demo_data TO user_three'
WHERE EXISTS (
    SELECT FROM information_schema.tables
    WHERE table_schema = 'schema_user_three'
    AND table_name = 'demo_data'
)\gexec


-- Step 5: Set search_path for each user to their schema for easier access
ALTER ROLE user_one SET search_path = schema_user_one;
ALTER ROLE user_two SET search_path = schema_user_two;
ALTER ROLE user_three SET search_path = schema_user_three;

-- Step 6: Create a table for storing user credentials in the public schema, restricted to the postgres user
CREATE TABLE IF NOT EXISTS public.users (
username VARCHAR(50) PRIMARY KEY,
password VARCHAR(50) -- In production, store a hashed password instead of plain text
);

-- Insert credentials into the users table, if they do not already exist
INSERT INTO public.users (username, password)
SELECT 'user_one', 'password_one'
WHERE NOT EXISTS (SELECT 1 FROM public.users WHERE username = 'user_one');

INSERT INTO public.users (username, password)
SELECT 'user_two', 'password_two'
WHERE NOT EXISTS (SELECT 1 FROM public.users WHERE username = 'user_two');

INSERT INTO public.users (username, password)
SELECT 'user_three', 'password_three'
WHERE NOT EXISTS (SELECT 1 FROM public.users WHERE username = 'user_three');

-- Restrict access to the users table so only the postgres superuser can read it
REVOKE ALL ON TABLE public.users FROM PUBLIC;
GRANT SELECT ON TABLE public.users TO postgres;

INSERT INTO schema_user_one.demo_data (field1,field2) values('demo','data one');
INSERT INTO schema_user_two.demo_data (field1,field2) values('demo','data two');
INSERT INTO schema_user_three.demo_data (field1,field2) values('demo','data three');

EOSQL
