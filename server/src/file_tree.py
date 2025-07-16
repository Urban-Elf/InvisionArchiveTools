from pathlib import Path
import platform

# Determine platform
system = platform.system()
home_dir = Path.home()

if system == "Windows":
    ROOT_PATH = home_dir / "AppData" / "Roaming" / "IAT"
elif system == "Darwin":  # macOS
    ROOT_PATH = home_dir / "Library" / "Application Support" / "IAT"
else:  # Linux or unknown
    ROOT_PATH = home_dir / ".iat"

LOG_PATH = ROOT_PATH / "logs"
SERVER_PATH = ROOT_PATH / "server"
EXPORT_PATH = ROOT_PATH / "export"

# Create directories
for path in [LOG_PATH, SERVER_PATH, EXPORT_PATH]:
    path.mkdir(parents=True, exist_ok=True)
