import os
import platform
import requests

def get_latest_brave_release():
    url = "https://api.github.com/repos/brave/brave-browser/releases/latest"
    response = requests.get(url)
    if response.status_code != 200:
        raise Exception(f"Failed to fetch release info: {response.status_code}")
    return response.json()

def get_download_url(release_data, os_name, arch):
    assets = release_data.get("assets", [])
    for asset in assets:
        asset_name = asset["name"].lower()
        if os_name == "linux":
            # Look for .deb or .rpm packages for the correct architecture
            if (".deb" in asset_name or ".rpm" in asset_name) and arch in asset_name:
                return asset["browser_download_url"]
        elif os_name == "win":
            # Look for Windows installer
            if "setup" in asset_name and asset_name.endswith(".exe"):
                return asset["browser_download_url"]
        elif os_name == "mac":
            # Look for macOS .dmg package
            if asset_name.endswith(".dmg"):
                return asset["browser_download_url"]
    raise Exception(f"No matching release found for OS: {os_name}, Arch: {arch}")

def download_file(url, output_path):
    response = requests.get(url, stream=True)
    if response.status_code == 200:
        with open(output_path, "wb") as file:
            for chunk in response.iter_content(chunk_size=8192):
                file.write(chunk)
    else:
        raise Exception(f"Failed to download file: {response.status_code}")

def main():
    # Determine OS and architecture
    system = platform.system().lower()
    arch = platform.machine().lower()

    if system == "linux":
        os_name = "linux"
    elif system == "windows":
        os_name = "win"
    elif system == "darwin":
        os_name = "mac"
    else:
        raise Exception(f"Unsupported OS: {system}")

    if "x86_64" in arch or "amd64" in arch:
        arch = "amd64"  # Use "amd64" for Linux package naming
    elif "arm64" in arch or "aarch64" in arch:
        arch = "arm64"
    else:
        raise Exception(f"Unsupported architecture: {arch}")

    # Fetch latest release data
    release_data = get_latest_brave_release()

    # Get download URL
    download_url = get_download_url(release_data, os_name, arch)

    # Define output path
    downloads_folder = os.path.join(os.path.expanduser("~"), "Downloads")
    os.makedirs(downloads_folder, exist_ok=True)
    file_name = download_url.split("/")[-1]
    output_path = os.path.join(downloads_folder, file_name)

    # Download the file
    print(f"Downloading {file_name} to {output_path}...")
    download_file(download_url, output_path)
    print("Download completed!")

if __name__ == "__main__":
    main()