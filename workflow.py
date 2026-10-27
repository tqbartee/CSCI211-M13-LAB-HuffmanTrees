#!/usr/bin/env python3
import os
import re
import sys
import time
import shutil
import subprocess
from pathlib import Path
import argparse

def set_project_name():
    """Finds settings.gradle.kts in the current working directory and updates 
    the rootProject.name parameter to match the active folder name."""
    current_dir = Path.cwd()
    settings_file = current_dir / 'settings.gradle.kts'

    if not settings_file.is_file():
        print(f"Error: Could not find settings.gradle.kts in the current directory: {current_dir}", file=sys.stderr)
        return False

    try:
        content = settings_file.read_text(encoding='utf-8')
        pattern = r'rootProject\.name\s*=\s*["\']([^"\']+)["\']'
        replacement = f'rootProject.name = "{current_dir.name}"'

        if not re.search(pattern, content):
            print("Error: Could not find a valid 'rootProject.name' assignment string in settings.gradle.kts.", file=sys.stderr)
            return False

        content = re.sub(pattern, replacement, content, count=1)
        settings_file.write_text(content, encoding='utf-8', newline='')
        print(f"\033[92mUpdated rootProject.name to '{current_dir.name}' in settings.gradle.kts\033[0m")
        return True
    except Exception as e:
        print(f"Error: Failed to update settings file: {e}", file=sys.stderr)
        return False


def export_codegrade_zip(destination_path):
    """Uses git archive to extract a pristine zip file from the active 
    git repository ledger, omitting tracking files and loose build variables."""
    if not Path('.git').is_dir():
        print("Error: Cannot export archive! Active context directory is not a Git repository ledger.", file=sys.stderr)
        return False

    try:
        print("\033[96mCompiling pristine CodeGrade template package archive...\033[0m")
        result = subprocess.run(["git", "archive", "--format=zip", "-o", str(destination_path), "HEAD"], capture_output=True, text=True)
        
        if result.returncode == 0:
            print(f"\033[92mCodeGrade template successfully created at: {destination_path}\033[0m")
            return True
        else:
            print(f"Error: Git archive execution failure encountered.\n{result.stderr}", file=sys.stderr)
            return False
    except Exception as e:
        print(f"Error: Failed to execute template archiving sequence: {e}", file=sys.stderr)
        return False


def generate_repo(deadline="2026-09-01T23:59:59+00:00", grace_days=1, max_late_days=30):
    """Standardizes project naming, runs the Gradle distribution block, 
    initializes a clean local git repository inside the student folder,
    generates a clean zip distribution archive, and writes codegrade-config.txt."""
    root_dir = Path.cwd()

    # 0. Safety Purge: Clear out stale build and deployment outputs from previous runs
    print("\033[96mClearing historical workspace artifacts...\033[0m")
    stale_zip = root_dir / "codegrade-template.zip"
    stale_config = root_dir / "codegrade-config.txt"
    stale_dist = root_dir / "dist"

    if stale_zip.is_file():
        stale_zip.unlink()
    if stale_config.is_file():
        stale_config.unlink()
    if stale_dist.is_dir():
        shutil.rmtree(stale_dist)

    # 1. Standardize rootProject.name parameter alignment
    print("\033[96mStandardizing local rootProject.name parameter alignment...\033[0m")
    if not set_project_name():
        print("Error: Project naming configuration failed! Aborting generation.", file=sys.stderr)
        return False

    # 2. Run Gradle task to scrub solution tags and build distribution
    print("\033[96mRunning Gradle distribution engine...\033[0m")
    gradle_cmd = "gradlew.bat" if sys.platform == "win32" else "./gradlew"
    if Path(gradle_cmd).is_file() or shutil.which(gradle_cmd):
        gradle_result = subprocess.run([gradle_cmd if sys.platform == "win32" else "./gradlew", "createStudentDistribution"])
        if gradle_result.returncode != 0:
            print("Error: Gradle distribution compilation failed!", file=sys.stderr)
            return False
    else:
        print("Error: gradlew.bat not found in current execution path!", file=sys.stderr)
        return False

    # 3. Assert student project output path constraints
    student_project_dir = root_dir / "dist/student-assignment"
    if not student_project_dir.is_dir():
        print(f"Error: Expected student template output folder does not exist at: {student_project_dir}", file=sys.stderr)
        return False

    # 3b. Purge student target files marked with "// //DELETE FOR STUDENTS"
    print("\033[96mScanning generated student files for explicit deletion tags...\033[0m")
    for student_file in student_project_dir.rglob("*.java"):
        try:
            content = student_file.read_text(encoding='utf-8', errors='ignore')
            if re.search(r'//\s*//DELETE FOR STUDENTS', content):
                student_file.unlink()
                print(f" \033[33m-> Dropped restricted class from student layout: {student_file.name}\033[0m")
        except Exception:
            pass

    # 4. Synchronize main .gitignore rule filters
    root_gitignore = root_dir / ".gitignore"
    if root_gitignore.is_file():
        shutil.copy2(root_gitignore, student_project_dir / ".gitignore")
        print("\033[92mSynchronized .gitignore file into distribution area.\033[0m")

    # 6. Discover files that contain instructional tags (STUB or TODO) or are deleted for students
    print("\033[96mScanning source files for assignment submission targets...\033[0m")
    master_src_main = root_dir / "src/main"
    relative_paths = []

    if master_src_main.is_dir():
        for file_path in master_src_main.rglob("*.java"):
            try:
                file_content = file_path.read_text(encoding='utf-8', errors='ignore')
                rel_path = file_path.relative_to(root_dir).as_posix()

                if re.search(r'//\s*//DELETE FOR STUDENTS', file_content):
                    relative_paths.append(rel_path)
                    print(f" \033[90m-> Indexed assignment file (Required submission / Student deleted): {rel_path}\033[0m")
                    continue

                if re.search(r'STUB_START|STUB_END|TODO', file_content):
                    relative_paths.append(rel_path)
                    print(f" \033[90m-> Indexed assignment file (Stub markers detected): {rel_path}\033[0m")
            except Exception:
                pass
    else:
        print("Warning: Master src/main directory not detected. No assignment paths indexed.", file=sys.stderr)

    # 7. Purge stale local historical git traces inside student dir
    student_git = student_project_dir / ".git"
    if student_git.is_dir():
        shutil.rmtree(student_git)
        print("\033[92mCleared trace history items from stale distribution targets.\033[0m")

    # 8. Initialize crisp local git ledger area inside student dir
    subprocess.run(["git", "init"], cwd=student_project_dir, check=True, capture_output=True)
    subprocess.run(["git", "add", "."], cwd=student_project_dir, check=True, capture_output=True)
    subprocess.run(["git", "commit", "-m", "Initial assignment template"], cwd=student_project_dir, check=True, capture_output=True)
    subprocess.run(["git", "branch", "-M", "main"], cwd=student_project_dir, check=True, capture_output=True)

    # 9. Compile pristine CodeGrade archive directly into parent workspace directory
    zip_target_file = root_dir / "codegrade-template.zip"
    os.chdir(student_project_dir)
    zip_success = export_codegrade_zip(zip_target_file)
    os.chdir(root_dir)

    if not zip_success:
        print("Error: CodeGrade target archive distribution package compilation failed!", file=sys.stderr)
        return False

    # 10. Write the CodeGrade configuration fixture block structure
    config_file_content = [
        f'NEW_DEADLINE="{deadline}"',
        f'GRACE_DAYS="{grace_days}"',
        f'MAX_DAYS_LATE_DEDUCT="{max_late_days}"',
        "STUDENT_SUBMIT"
    ] + relative_paths + ["STUDENT_SUBMIT_END"]

    joined_content = "\r\n".join(config_file_content)
    config_target_file = root_dir / "codegrade-config.txt"
    config_target_file.write_text(joined_content, encoding='utf-8', newline='')
    print("\033[92mGenerated dynamic CodeGrade config fixture file layout configuration block.\033[0m")

    return True


def send_repo():
    """Verifies system path availability, validates GitHub active authentication,
    wipes any existing remote target repository matching the current 
    assignment string profile, pushes the local student template up, and
    verifies CodeGrade bundle alignment layouts."""
    
    # 1. Advanced Safeguard: System Path Verification
    if not shutil.which("gh"):
        print("Error: GitHub CLI ('gh') is not accessible in this terminal window!", file=sys.stderr)
        print("Verify that GitHub CLI is installed and your system environment PATH includes it.", file=sys.stderr)
        return

    # 2. Advanced Safeguard: Active Authentication Status Check
    print("\033[96mVerifying GitHub authentication status...\033[0m")
    auth_result = subprocess.run(["gh", "auth", "status"], capture_output=True, text=True)
    if auth_result.returncode != 0:
        print("Error: You are not logged into GitHub via the GitHub CLI!", file=sys.stderr)
        print("Please execute 'gh auth login' in your terminal before running Send-Repo.", file=sys.stderr)
        return
    print("\033[92mGitHub path and authentication verified successfully.\033[0m")

    root_dir = Path.cwd()
    master_folder_name = root_dir.name
    student_repo_name = re.sub(r'-solution|-master', '', master_folder_name)
    repo = f"tqbartee/{student_repo_name}"

    # 3. Target the student assignment folder path
    student_project_dir = root_dir / "dist/student-assignment"
    if not student_project_dir.is_dir():
        print(f"Error: Cannot deploy! Generate-Repo must be run successfully first. Folder missing at: {student_project_dir}", file=sys.stderr)
        return

    # 4. Assert active tracking state constraints for CodeGrade bundle files
    zip_target_file = root_dir / "codegrade-template.zip"
    config_target_file = root_dir / "codegrade-config.txt"
    if not zip_target_file.is_file() or not config_target_file.is_file():
        print("Warning: CodeGrade bundle assets missing from workspace layout context. Re-generating package repository configuration...")

    # 5. Guard track constraints against uninitialized repository states
    if not (student_project_dir / ".git").is_dir():
        print("Error: Active directory is un-initialized by Git framework rules! Run Generate-Repo first.", file=sys.stderr)
        return

    # 7. Cleanse remote endpoint configurations if collision records exist
    view_result = subprocess.run(["gh", "repo", "view", repo], capture_output=True, text=True)
    if view_result.returncode == 0:
        print(f"\033[33mWiping existing server target instance profile: {repo}...\033[0m")
        del_result = subprocess.run(["gh", "repo", "delete", repo, "--confirm"], capture_output=True, text=True)
        if del_result.returncode == 0:
            print(f"\033[92mRepository instance target {repo} systematically purged from GitHub.\033[0m")
            time.sleep(3)
        else:
            print("Error: Target server configuration write lock exception! Check permissions.", file=sys.stderr)
            return

    # 8. Provision crisp cloud placeholder parameters and upload template
    print(f"\033[96mProvisioning new private platform repository framework: {student_repo_name}...\033[0m")
    create_result = subprocess.run([
        "gh", "repo", "create", student_repo_name, "--private", "--source=.", "--remote=origin", "--push"
    ], cwd=student_project_dir)

    if create_result.returncode != 0:
        print("Error: GitHub repository creation/push failed.", file=sys.stderr)
        return

    # 9. Summary output
    print(f"\033[92mDeployment workflow finished successfully. Restored to master root: {root_dir}\033[0m")
    if zip_target_file.is_file():
        print("\033[96mReady for CodeGrade upload!\033[0m")
        print(f" -> Fixture Archive: {zip_target_file}")
        print(f" -> Fixture Config: {config_target_file}")


def main():
    parser = argparse.ArgumentParser(description="Manage CodeGrade student distribution templates and GitHub deployments.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    # generate command
    gen_parser = subparsers.add_parser("generate", help="Run the repository generation and CodeGrade packaging pipeline.")
    gen_parser.add_argument("--deadline", default="2026-09-01T23:59:59+00:00", help="Assignment submission deadline timestamp.")
    gen_parser.add_argument("--grace-days", type=int, default=1, help="Grace period days.")
    gen_parser.add_argument("--max-late-days", type=int, default=30, help="Maximum late penalty days.")

    # send command
    subparsers.add_parser("send", help="Verify environment, wipe/create remote GitHub repo, and push template.")

    # set-projectname command
    subparsers.add_parser("set-projectname", help="Update settings.gradle.kts rootProject.name to match the active folder name.")

    args = parser.parse_args()

    if args.command == "generate":
        success = generate_repo(deadline=args.deadline, grace_days=args.grace_days, max_late_days=args.max_late_days)
        sys.exit(0 if success else 1)
    elif args.command == "send":
        send_repo()
    elif args.command == "set-projectname":
        success = set_project_name()
        sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()