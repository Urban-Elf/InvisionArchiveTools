#!/usr/bin/env python3
import subprocess
import sys
import os
import json
import glob

MODULE_NAME = "server"
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
VENV_DIR = os.path.join(os.path.dirname(__file__), ".venv")
REQUIREMENTS = os.path.join(os.path.dirname(__file__), "requirements.txt")

# Determine venv python path
venv_python = os.path.join(VENV_DIR, "Scripts", "python.exe") if os.name == "nt" else os.path.join(VENV_DIR, "bin", "python")

def run(args, **kwargs):
    print(f"> {' '.join(args)}")
    subprocess.check_call(args, **kwargs)

def ensure_venv():
    if not os.path.isdir(VENV_DIR):
        print("Creating virtual environment...")
        run([sys.executable, "-m", "venv", VENV_DIR])

    # Upgrade pip/setuptools/wheel and install requirements
    if os.path.isfile(REQUIREMENTS):
        print("Installing dependencies...")
        run([venv_python, "-m", "pip", "install", "--upgrade", "pip", "setuptools", "wheel"])
        run([venv_python, "-m", "pip", "install", "-r", REQUIREMENTS])
    else:
        print(f"Warning: requirements file not found at {REQUIREMENTS}")

def fix_macos_extensions():
    """Ensure all .so/.dylib C-extensions are universal2 on macOS."""
    if sys.platform != "darwin":
        return

    print("Checking macOS C-extension binaries for universal2 slices...")
    site_packages = os.path.join(VENV_DIR, "lib", f"python{sys.version_info.major}.{sys.version_info.minor}", "site-packages")
    for ext in glob.glob(f"{site_packages}/**/*.[sd]o", recursive=True):
        try:
            out = subprocess.check_output(["lipo", "-info", ext], text=True).strip()
            if "x86_64" not in out or "arm64" not in out:
                # Reinstall the package containing this extension
                pkg_dir = os.path.basename(os.path.dirname(ext))
                print(f"⚡ Reinstalling {pkg_dir} as universal2 (missing slice)")
                run([venv_python, "-m", "pip", "install",
                     "--force-reinstall", "--no-cache-dir", "--only-binary=:all:", pkg_dir])
        except subprocess.CalledProcessError:
            continue  # Skip non-lipo-compatible binaries

def build():
    # Read version
    with open(os.path.join(PROJECT_ROOT, "VERSION")) as f:
        version = f.read().strip()

    # Fix macOS C-extensions if needed
    fix_macos_extensions()

    # Build with PyInstaller
    args = [
        venv_python, "-m", "PyInstaller", "bootstrap.py",
        "--name", MODULE_NAME,
        "--hidden-import", "bs4",
        "--collect-all", "bs4",
        "--hidden-import", "certifi",
        "--collect-all", "certifi",
        "--log-level", "WARN",
        "--noconfirm"
    ]
    if sys.platform == "darwin":
        args += ["--target-arch", "universal2"]

    run(args)

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
