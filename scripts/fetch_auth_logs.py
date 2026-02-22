#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import subprocess
import os
import time

# Configuration
SSH_KEY = "id_rsa_deploy"
SSH_USER = "root"
SSH_HOST = "isaliveqwq.me"
LOG_DIR = "docs/dev-logs/remote-logs"
TIMESTAMP = int(time.time())

SERVICES = [
    "auth-service",
    "user-service",
    "gateway"
]

def fetch_log(service_name):
    print(f"Fetching logs for {service_name}...")
    
    os.makedirs(LOG_DIR, exist_ok=True)
    local_file = f"{LOG_DIR}/{service_name}-{TIMESTAMP}.log"
    
    ssh_cmd = [
        "ssh", "-i", SSH_KEY, 
        "-o", "StrictHostKeyChecking=no", 
        f"{SSH_USER}@{SSH_HOST}", 
        f"docker logs --tail 300 {service_name}"
    ]
    
    try:
        with open(local_file, "w", encoding="utf-8") as outfile:
            result = subprocess.run(
                ssh_cmd, 
                stdout=subprocess.PIPE, 
                stderr=subprocess.PIPE,
                text=True,
                encoding='utf-8', 
                errors='replace'
            )
            outfile.write(result.stdout)
            outfile.write(result.stderr)
            
        print(f"  -> Saved to {local_file}")
        
    except Exception as e:
        print(f"  -> Failed: {str(e)}")

if __name__ == "__main__":
    print(f"Starting Log Fetch from {SSH_HOST}...")
    for service in SERVICES:
        fetch_log(service)
    print("Done.")
