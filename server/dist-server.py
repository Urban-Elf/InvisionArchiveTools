#!/usr/bin/env python3
import subprocess
import sys
import os
import json

MODULE_NAME = "server"
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
VENV_DIR = os.path.join(os.path.dirname(__file__), ".venv")
REQUIREMENTS = os.path.join(os.path.dirname(__file__), "requirements.txt")
#SPEC_FILE = os.path.join(os.path.dirname(__file__), f"{MODULE_NAME}.spec")

# Determine venv python path
venv_python = os.path.join(VENV_DIR, "Scripts", "python.exe") if os.name == "nt" else os.path.join(VENV_DIR, "bin", "python")

def run(args, **kwargs):
    print(f"> {' '.join(args)}")
    subprocess.check_call(args, **kwargs)

def ensure_venv():
    if not os.path.isdir(VENV_DIR):
        print("Creating virtual environment...")
        run([sys.executable, "-m", "venv", VENV_DIR])

    # Install requirements
    if os.path.isfile(REQUIREMENTS):
        print("Installing dependencies...")
        run([venv_python, "-m", "pip", "install", "--upgrade", "pip"])
        run([venv_python, "-m", "pip", "install", "-r", REQUIREMENTS])
    else:
        print(f"Warning: requirements file not found at {REQUIREMENTS}")

def build():
    # Read version
    with open(os.path.join(PROJECT_ROOT, "VERSION")) as f:
        version = f.read().strip()

    # Build with PyInstaller
    run([venv_python, "-m", "PyInstaller", "bootstrap.py", "--name", MODULE_NAME, "--hidden-import \"bs4\"", "--noconfirm"])
    #run([venv_python, "-m", "PyInstaller", SPEC_FILE, "--noconfirm"])

    # Write metadata.json
    metadata_dir = os.path.join("dist", MODULE_NAME)
    os.makedirs(metadata_dir, exist_ok=True)
    metadata_path = os.path.join(metadata_dir, "metadata.json")
    with open(metadata_path, "w") as f:
        json.dump({"version": version}, f)
    print(f"Wrote {metadata_path}")

def main():
    ensure_venv()
    build()
    print("Build complete.")

if __name__ == "__main__":
    main()
