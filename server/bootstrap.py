import ssl
import certifi
import urllib.request

# Save original
_original_urlopen = urllib.request.urlopen

# Custom SSL context with certifi CA
_certifi_context = ssl.create_default_context(cafile=certifi.where())

# Monkey-patch urlopen to always use certifi context
def patched_urlopen(*args, **kwargs):
    if 'context' not in kwargs:
        kwargs['context'] = _certifi_context
    return _original_urlopen(*args, **kwargs)

urllib.request.urlopen = patched_urlopen

import sys
import bs4 # Kind of a hack to ensure PyInstaller sees this lib

from src.main import main

main(sys.argv)
