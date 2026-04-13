Oracle Always Free VM deployment (Receptor API + Postgres)

This guide sets up the API and database on a free Oracle Cloud VM so the app works without your PC.

What you need from Oracle:
- Free Tier account and a VM in your home region
- A public IP and SSH access to the VM
- A security list / VCN rule that allows inbound TCP 22 (SSH) and 8080 (API), and optionally 80/443 if you later add a reverse proxy

Recommended VM settings:
- Shape: Ampere A1 (Arm) with 1-2 OCPUs and 6-8 GB RAM
- Image: Ubuntu 22.04 (Arm)

Steps
1) Copy the repo to the VM
   - Option A: git clone
     git clone <your repo url> receptor
   - Option B: zip + scp
     scp -i <key> -r C:\AndroidStudio\Receptor ubuntu@<VM_IP>:/home/ubuntu/receptor

2) Run the bootstrap script
   - On the VM:
     cd ~/receptor/deploy/oracle
     chmod +x setup.sh
     ./setup.sh

3) Load schema + data into Postgres
   - On the VM:
     cd ~/receptor
     PGPASSWORD="q1w2e3" psql -h localhost -U postgres -d receptor -f db/schema.sql
     PGPASSWORD="q1w2e3" psql -h localhost -U postgres -d receptor -f db/import_food_ru.sql

4) Configure and start the API service
   - On the VM:
     sudo cp ~/receptor/deploy/oracle/receptor-api.service /etc/systemd/system/receptor-api.service
     sudo systemctl daemon-reload
     sudo systemctl enable --now receptor-api

5) Test
   - From your PC:
     curl http://<VM_IP>:8080/health

If you want HTTPS:
- Add Nginx + a free certificate later. I can set that up after the basic deploy is working.
