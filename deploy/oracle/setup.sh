#!/usr/bin/env bash
set -euo pipefail

echo "[1/6] Updating packages..."
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg lsb-release git unzip

echo "[2/6] Installing Node.js 20 LTS..."
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

echo "[3/6] Installing PostgreSQL 17..."
sudo install -d /usr/share/postgresql-common/pgdg
curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo gpg --dearmor -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.gpg
echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.gpg] http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" | sudo tee /etc/apt/sources.list.d/pgdg.list
sudo apt-get update
sudo apt-get install -y postgresql-17

echo "[4/6] Creating database receptor and setting postgres password..."
sudo -u postgres psql -v ON_ERROR_STOP=1 -c "CREATE DATABASE receptor;" || true
sudo -u postgres psql -v ON_ERROR_STOP=1 -c "ALTER USER postgres WITH PASSWORD 'q1w2e3';"

echo "[5/6] Installing API dependencies..."
cd ~/receptor/server
npm install

echo "[6/6] Done. Next: import SQL and start the systemd service."
