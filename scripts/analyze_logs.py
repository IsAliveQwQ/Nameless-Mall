#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Log Analysis Tool
Scans local log files fetched from remote, extracts ERROR/Exception blocks,
and generates a comprehensive report file.
"""

import glob
import re
import os
import time

LOG_DIR = 'docs/dev-logs/remote-logs'
REPORT_DIR = 'docs/dev-logs/analysis-reports'
os.makedirs(REPORT_DIR, exist_ok=True)
TIMESTAMP = int(time.time())
REPORT_FILE = f"{REPORT_DIR}/analysis-{TIMESTAMP}.txt"

LOG_PATTERN = r'^\d{4}-\d{2}-\d{2}' # Log start pattern (YYYY-MM-DD)

def get_latest_logs():
    """Groups log files by timestamp suffix and finds the latest group."""
    all_logs = glob.glob(f'{LOG_DIR}/*.log')
    if not all_logs:
        return []
    
    # Try to group by timestamp in filename (service-TIMESTAMP.log)
    latest_ts = 0
    for log in all_logs:
        try:
            ts = int(re.search(r'-(\d+)\.log$', log).group(1))
            if ts > latest_ts:
                latest_ts = ts
        except:
            continue
            
    if latest_ts > 0:
        return glob.glob(f'{LOG_DIR}/*-{latest_ts}.log')
    else:
        # Fallback: just take all files if no timestamp pattern found
        return all_logs

def analyze_logs():
    log_files = get_latest_logs()
    if not log_files:
        print("No log files found to analyze.")
        return

    print(f"Analyzing {len(log_files)} log files...")
    print(f"Report will be saved to: {REPORT_FILE}")

    with open(REPORT_FILE, 'w', encoding='utf-8') as report:
        report.write(f"=== Analysis Report - {time.ctime(TIMESTAMP)} ===\n")
        report.write(f"Source Logs: {LOG_DIR}\n\n")

        for log_file in sorted(log_files):
            service_name = os.path.basename(log_file).split('-')[0]
            report.write(f"\n{'='*30}\n")
            report.write(f"SERVICE: {service_name}\n")
            report.write(f"FILE: {os.path.basename(log_file)}\n")
            report.write(f"{'='*30}\n")
            
            error_count = 0
            
            with open(log_file, 'r', encoding='utf-8', errors='replace') as f:
                lines = f.readlines()
                
                in_error_block = False
                error_block = []
                
                # Scan entire file or last N lines if too large.
                # Let's scan last 5000 lines to be safe.
                scan_lines = lines[-5000:] if len(lines) > 5000 else lines
                
                for line in scan_lines:
                    # Check for ERROR level
                    if ' ERROR ' in line or 'Exception' in line:
                        if not in_error_block:
                            # Start of a new error block
                             in_error_block = True
                             error_count += 1
                        error_block.append(line)
                        continue
                    
                    # If we are in an error block
                    if in_error_block:
                        # If line starts with a date, it might be a new log entry
                        # If it is a new log entry (and not indented stack trace), we end the block
                        if re.match(LOG_PATTERN, line):
                             # Write the previous block
                             report.write(''.join(error_block))
                             report.write('-' * 20 + '\n')
                             error_block = [] # Clear
                             
                             # Check if this new line is ALSO an error
                             if ' ERROR ' in line:
                                 error_block.append(line)
                                 error_count += 1
                             else:
                                 in_error_block = False
                        else:
                             # Likely part of stack trace
                             error_block.append(line)
                
                # Flush last block
                if error_block:
                    report.write(''.join(error_block))
                    report.write('-' * 20 + '\n')
            
            if error_count == 0:
                report.write("(No ERRORs found in tail)\n")
            else:
                report.write(f"\nFound {error_count} error blocks.\n")

    print(f"Analysis complete. Report generated.")

if __name__ == "__main__":
    analyze_logs()
