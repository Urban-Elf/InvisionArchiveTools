import sys
import bs4 # Kind of a hack to ensure PyInstaller sees this lib
from src.main import main

if __name__ == "__main__":
    main(sys.argv)
