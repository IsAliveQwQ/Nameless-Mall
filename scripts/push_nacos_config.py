
import os
import requests
import argparse
from pathlib import Path

# ============================================
# Nacos Batch Config Pusher (Refined Version)
# ============================================

def push_configs(nacos_host, config_dir, group):
    nacos_url = f"http://{nacos_host}/nacos/v1/cs/configs"
    login_url = f"http://{nacos_host}/nacos/v1/auth/login"
    
    config_path = Path(config_dir)
    if not config_path.exists():
        print(f"Error: Directory {config_dir} not found.")
        return

    # 1. 獲取 Auth Token (預設 Nacos 帳密)
    print(f"Step 1: Logging into Nacos at {nacos_host}...")
    token = None
    try:
        login_resp = requests.post(login_url, data={"username": "nacos", "password": "nacos"}, timeout=5)
        if login_resp.status_code == 200:
            token = login_resp.json().get("accessToken")
            print("Successfully logged in.")
    except Exception as e:
        print(f"Login failed (Nacos might not have auth enabled): {e}")

    # 2. 批量推送
    print(f"Step 2: Scanning {config_dir} for .yml files...")
    for file_path in config_path.glob("*.yml"):
        data_id = file_path.name
        print(f"Processing {data_id}...", end=" ")
        
        try:
            # 使用 'r' 模式讀取會自動將 \r\n 轉換為 \n，維持格式純淨
            with open(file_path, "r", encoding="utf-8-sig") as f:
                content = f.read()
            
            payload = {
                "dataId": data_id,
                "group": group,
                "content": content,
                "type": "yaml"
            }
            
            params = {}
            if token:
                params["accessToken"] = token
            
            resp = requests.post(nacos_url, data=payload, params=params, timeout=10)
            
            if resp.status_code == 200 and resp.text.strip() == "true":
                print("✅ Success")
            else:
                print(f"❌ Failed ({resp.status_code}): {resp.text}")
                
        except Exception as e:
            print(f"❌ Error: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Batch push configs to Nacos")
    parser.add_argument("--host", default="localhost:8848", help="Nacos host (default: localhost:8848)")
    parser.add_argument("--dir", default="nacos-config", help="Config directory (default: nacos-config)")
    parser.add_argument("--group", default="DEFAULT_GROUP", help="Nacos group (default: DEFAULT_GROUP)")
    
    args = parser.parse_args()
    
    # 強制將工作目錄移動到專案根目錄，確保路徑正確
    # (此部分視執行環境而定，暫以絕對/相對路徑相容為主)
    
    push_configs(args.host, args.dir, args.group)

