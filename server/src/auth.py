import struct
from pathlib import Path

from .file_tree import ROOT_PATH

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

SALT_LENGTH = 16
NONCE_LENGTH = 12
TAG_LENGTH = 16
HEADER_LENGTH = 4 + 4 + 4 + SALT_LENGTH + NONCE_LENGTH


def load_password_file(path: str | Path, key_password: str) -> tuple[str, str]:
    data = Path(path).read_bytes()

    if len(data) < HEADER_LENGTH + TAG_LENGTH:
        raise ValueError("Invalid or truncated encrypted file")

    # Header integers are Java ByteBuffer's default big-endian format.
    iterations, username_length, password_length = struct.unpack(">III", data[:12])

    if not 1 <= iterations <= 5_000_000:
        raise ValueError("Invalid iteration count")
    if username_length + password_length + HEADER_LENGTH + TAG_LENGTH != len(data):
        raise ValueError("Invalid encrypted-file length")

    salt_start = 12
    nonce_start = salt_start + SALT_LENGTH
    ciphertext_start = nonce_start + NONCE_LENGTH

    salt = data[salt_start:nonce_start]
    nonce = data[nonce_start:ciphertext_start]
    ciphertext = data[ciphertext_start:]
    header = data[:HEADER_LENGTH]  # Same bytes Java authenticates as AAD.

    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        iterations=iterations,
    )
    key = kdf.derive(key_password.encode("utf-8"))

    # Raises InvalidTag if the key, header, or ciphertext is incorrect or altered.
    plaintext = AESGCM(key).decrypt(nonce, ciphertext, header)

    if len(plaintext) != username_length + password_length:
        raise ValueError("Decrypted data has an invalid length")

    username_bytes = plaintext[:username_length]
    password_bytes = plaintext[username_length:]

    return (
        username_bytes.decode("utf-8"),
        password_bytes.decode("utf-8"),
    )

def load_debug_credentials():
    creds_file = ROOT_PATH / 'auth.txt'
    if not creds_file.is_file():
        raise FileNotFoundError()

    username = None
    password = None
    with creds_file.open() as f:
        for line in f:
            line = line.strip()
            if line.startswith("username: "):
                username = line[len("username: "):].strip()
            elif line.startswith("password: "):
                password = line[len("password: "):].strip()
    if username is None or password is None:
        return None
    return (
        username,
        password,
    )
