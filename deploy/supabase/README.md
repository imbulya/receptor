Supabase deployment (database-only backend)

This guide puts the Postgres database in Supabase. You can then choose:
Option A: Use Supabase REST (PostgREST) directly from the app (no custom API server).
Option B: Keep our custom API and host it elsewhere (Supabase does not host Node.js).

Steps
1) Create a Supabase project (Free plan is OK).

2) Get connection details in the Supabase dashboard:
   - Project URL (REST base)
   - Anon public key
   - Database connection string (host, port, db, user, password)

3) Import schema + data using psql (from your PC):
   - Open PowerShell in repo root
   - Set env vars, then run:
     $env:PGHOST = "<DB_HOST>"
     $env:PGPORT = "5432"
     $env:PGDATABASE = "postgres"
     $env:PGUSER = "postgres"
     $env:PGPASSWORD = "<DB_PASSWORD>"
     psql -f db\schema.sql
     psql -f db\import_food_ru.sql

If you want the DB name to be "receptor", create it first:
  psql -c "CREATE DATABASE receptor;"
  $env:PGDATABASE = "receptor"
  psql -f db\schema.sql
  psql -f db\import_food_ru.sql

Next:
- If you pick Option A, I will switch the Android app to Supabase REST and add the minimal policies.
- If you pick Option B, I will adapt the API to Supabase DB and provide a hosting target.
