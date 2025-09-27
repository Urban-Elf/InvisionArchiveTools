import multiprocessing
import sys
import bs4  # ensure PyInstaller sees this
from src.main import main

if __name__ == "__main__":
    multiprocessing.freeze_support()
    multiprocessing.set_start_method("spawn", force=True)
    main(sys.argv)
