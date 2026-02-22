#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Remote Log Fetcher
Connects to VM via SSH and downloads docker logs for all services.
Handles encoding to prevent mojibake.
"""

import subprocess
import os
import time

# Configuration pull
SSH_KEY = os.path.join(os.path.dirname(__file__), "..", "id_rsa_deploy")
SSH_USER = "root"
SSH_HOST = "isaliveqwq.me"
LOG_DIR = "docs/dev-logs/remote-logs"
TIMESTAMP = int(time.time())

SERVICES = [
    "gateway",
    "auth-service",
    "user-service",
    "product-service",
    "cart-service",
    "order-service",
    "payment-service",
    "coupon-service",
    "promotion-service",
    "search-service",
    "nginx-proxy",
    "nacos",
    "seata-server"
]

def fetch_log(service_name):
    print(f"Fetching logs for {service_name}...")
    
    # Ensure log directory exists
    os.makedirs(LOG_DIR, exist_ok=True)
    
    local_file = f"{LOG_DIR}/{service_name}-{TIMESTAMP}.log"
    
    # SSH command to fetch logs
    # Using 'docker logs --tail 2000' to get significant context
    ssh_cmd = [
        "ssh", "-i", SSH_KEY, 
        "-o", "StrictHostKeyChecking=no", 
        f"{SSH_USER}@{SSH_HOST}", 
        f"docker logs --tail 2000 {service_name}"
    ]
    
    try:
        # Use subprocess to capture stdout/stderr directly
        with open(local_file, "w", encoding="utf-8") as outfile:
            # Capture both stdout and stderr (logs usually go to stderr/stdout mixed)
            result = subprocess.run(
                ssh_cmd, 
                stdout=subprocess.PIPE, 
                stderr=subprocess.PIPE,
                text=True,  # Handle decoding automatically
                encoding='utf-8', 
                errors='replace' # Replace bad chars instead of crashing
            )
            
            # Write to file
            outfile.write(result.stdout)
            outfile.write(result.stderr) # Docker logs often go to stderr
            
        print(f"  -> Saved to {local_file}")
        
    except Exception as e:
        print(f"  -> Failed: {str(e)}")

def main():
    print(f"Starting Log Fetch from {SSH_HOST}...")
    for service in SERVICES:
        fetch_log(service)
    print("Done.")

if __name__ == "__main__":
    main()
