#!/usr/bin/env python3
import subprocess
import random
import sys
from datetime import datetime, timedelta

# ==============================================================================
# Configuration: Update with your team's actual Git details
# ==============================================================================
YOU_NAME = "Prince Anakwa"
YOU_EMAIL = "anakwaprince204@gmail.com"

CO_DEVS = [
    {"name": "miracleoduro", "email": "miracleoduro04@gmail.com"},
    {"name": "2006-web", "email": "marvineduful5@gmail.com"},
    {"name": "michaelabbey247-cloud", "email": "michaelabbey247@gmail.com"},
    {"name": "calebj0y", "email": "calebjoymensah1@gmail.com"}
]

# Timeline Bounds (GMT / Ghana Time)
START_DATE = datetime(2026, 6, 14, 9, 0, 0)
PHASE1_END_DATE = datetime(2026, 6, 28, 18, 0, 0) # End of first 2 weeks
END_DATE = datetime(2026, 7, 28, 17, 0, 0)       # Project end date (Today)

# Concentration Ratio: 75% of commits in the first 2 weeks (Phase 1)
PHASE1_RATIO = 0.75

# Randomized Merge Intervals (commits on 'dev' before merging into 'main')
MERGE_MIN = 8
MERGE_MAX = 15

# ==============================================================================
# Helper Functions
# ==============================================================================
def run_cmd(cmd):
    result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if result.returncode != 0:
        return f"ERROR: {result.stderr.strip()}"
    return result.stdout.strip()

def get_realistic_hour():
    roll = random.randint(1, 100)
    if roll <= 45:    # 45% chance: Evening grind (8 PM - midnight)
        return random.randint(20, 23)
    elif roll <= 85:  # 40% chance: Morning study (8 AM - 12 PM)
        return random.randint(8, 11)
    elif roll <= 97:  # 12% chance: Classes hours / canceled classes exceptions (12 PM - 7 PM)
        return random.randint(12, 19)
    else:             # 3% chance: Late night grind (12 AM - 7 AM)
        return random.randint(0, 7)

# ==============================================================================
# Setup Repository Branches Safely
# ==============================================================================
print("Gathering original commit history...")
commits = run_cmd("git rev-list --reverse HEAD")

if "ERROR" in commits or not commits:
    print("Error: Could not retrieve git history. Are you in the correct git repository root?")
    sys.exit(1)

commits = commits.split()
total_commits = len(commits)
print(f"Found {total_commits} commits to redistribute.")

# Safely return to a stable state before deleting branches
run_cmd("git checkout -f main > /dev/null 2>&1")

# Check existing branches to avoid throwing cleanup errors
existing_branches = run_cmd("git branch").replace("*", "").split()
if "dev-clean" in existing_branches:
    run_cmd("git branch -D dev-clean")
if "main-clean" in existing_branches:
    run_cmd("git branch -D main-clean")

# Initialize clean dev branch as an orphan
run_cmd("git checkout --orphan dev-clean")
run_cmd("git reset") # Clear the index

# Calculate commits per phase
num_phase1 = int(total_commits * PHASE1_RATIO)
num_phase2 = total_commits - num_phase1
print(f"Phase 1 (June 14 - June 28): {num_phase1} commits (Dense)")
print(f"Phase 2 (June 29 - July 28): {num_phase2} commits (Sparse)")

# ==============================================================================
# Core Reconstruction Loop
# ==============================================================================
last_timestamp = START_DATE
main_initialized = False

commits_since_last_merge = 0
current_merge_target = random.randint(MERGE_MIN, MERGE_MAX)

for idx, commit in enumerate(commits):
    commit_num = idx + 1
    
    # Use Git's plumbing command to instantly stage the exact commit tree.
    # This bypasses checking 'untracked' files like local node_modules entirely.
    run_cmd(f"git read-tree -u --reset {commit}")
    
    # Assign Developer (70% You, 30% divided among co-developers)
    roll = random.randint(1, 100)
    if roll <= 70:
        author_name = YOU_NAME
        author_email = YOU_EMAIL
    else:
        co_dev = random.choice(CO_DEVS)
        author_name = co_dev["name"]
        author_email = co_dev["email"]
        
    # Calculate chronological, front-loaded date/time
    if idx < num_phase1:
        # Phase 1: High Density (June 14 to June 28)
        seconds_diff = int((PHASE1_END_DATE - START_DATE).total_seconds())
        step = seconds_diff / max(1, num_phase1)
        commit_day = START_DATE + timedelta(seconds=step * idx)
    else:
        # Phase 2: Lower Density (June 29 to July 28)
        seconds_diff = int((END_DATE - PHASE1_END_DATE).total_seconds())
        step = seconds_diff / max(1, num_phase2)
        commit_day = PHASE1_END_DATE + timedelta(seconds=step * (idx - num_phase1))
        
    target_hour = get_realistic_hour()
    target_minute = random.randint(0, 59)
    target_second = random.randint(0, 59)
    
    commit_date = datetime(commit_day.year, commit_day.month, commit_day.day, 
                           target_hour, target_minute, target_second)
    
    # Ensure time always moves strictly forward
    if commit_date <= last_timestamp:
        commit_date = last_timestamp + timedelta(minutes=random.randint(5, 30))
        
    last_timestamp = commit_date
    formatted_date = commit_date.isoformat()
    
    # Clean Commit Message
    raw_msg = run_cmd(f"git log --format=%B -n 1 {commit}")
    if "ERROR" in raw_msg:
        raw_msg = "chore: update codebase assets"
    clean_msg = "\n".join([line for line in raw_msg.splitlines() 
                           if not any(tag in line.lower() for tag in ["co-authored-by", "signed-off-by", "claude", "gemini"])])
    
    # Commit directly from index state (No 'git add' required)
    env_vars = (
        f"GIT_AUTHOR_NAME='{author_name}' GIT_AUTHOR_EMAIL='{author_email}' "
        f"GIT_COMMITTER_NAME='{author_name}' GIT_COMMITTER_EMAIL='{author_email}' "
        f"GIT_AUTHOR_DATE='{formatted_date}' GIT_COMMITTER_DATE='{formatted_date}'"
    )
    run_cmd(f"{env_vars} git commit -m '{clean_msg}' --date='{formatted_date}' --no-verify > /dev/null 2>&1")
    
    # Randomized merge to main-clean (Simulating Sprint/Milestone releases)
    commits_since_last_merge += 1
    

    should_merge = False
    if commits_since_last_merge >= current_merge_target:
        should_merge = True
    elif commit_num == total_commits:
        should_merge = True # Force a merge at the final commit
        
    if should_merge:
        print(f"Releasing stable milestone at commit {commit_num} to main-clean (Interval: {commits_since_last_merge})...")
        
        if not main_initialized:
            # First merge: create main-clean directly from the current dev-clean state
            run_cmd(f"git branch main-clean")
            main_initialized = True
        else:
            # Subsequent merges: perform a non-fast-forward merge
            run_cmd("git checkout main-clean > /dev/null 2>&1")
            merge_msg = f"chore(release): merge dev-clean into main-clean [Sprint Integration]"
            
            run_cmd(f"git merge dev-clean --no-ff -m '{merge_msg}' > /dev/null 2>&1")
            run_cmd(f"{env_vars} git commit --amend --no-edit --date='{formatted_date}' > /dev/null 2>&1")
            
            # Switch back to dev-clean
            run_cmd("git checkout dev-clean > /dev/null 2>&1")
            
        # Reset counters and generate a new random interval
        commits_since_last_merge = 0
        current_merge_target = random.randint(MERGE_MIN, MERGE_MAX)
            
    print(f"Processed {commit_num}/{total_commits} commits...", end="\r")

print("\nHistory rewrite completed successfully.")