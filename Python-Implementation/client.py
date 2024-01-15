import socket
from Crypto.Util.Padding import unpad, pad
from Crypto.Cipher import AES
import numpy as np
from ggh import *

def aes_encrypt(data, key):
    cipher = AES.new(key, AES.MODE_CBC)
    ct_bytes = cipher.encrypt(pad(data, AES.block_size))
    iv = cipher.iv
    return ct_bytes, iv

def aes_decrypt(ciphertext, iv, key):
    cipher = AES.new(key, AES.MODE_CBC, iv)
    original_data = unpad(cipher.decrypt(ciphertext), AES.block_size)
    return original_data

# Connect to server...
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Define the host and the port you want to connect to
host = '127.0.0.1'
port = 65432

client.connect((host, port))

# Receive GGH public key from server
publicB_bytes = client.recv(4096)
publicB = np.frombuffer(publicB_bytes).reshape((10, 10))

# Prepare fake message representing the exchanged AES key
aes_key = b'secretAESkey1234'  # 16 bytes for AES-128

# Encrypt AES key with GGH public key
encrypted_aes_key = encrypt(aes_key, publicB)

# Send the encrypted AES key to server
client.send(encrypted_aes_key)

try:
    while True:
        # Receive encrypted message from server
        encrypted_data = client.recv(4096)
        if not encrypted_data:
            break  # Break the loop if the server closes the connection

        iv = encrypted_data[:16]
        encrypted_message = encrypted_data[16:]
        print(f"Encrypted data received from server: {encrypted_message}")
        # Decrypt the message
        decrypted_message = aes_decrypt(encrypted_message, iv, aes_key)
        print(f"Decrypted message using AES key: {decrypted_message.decode()}")

        # Send a response to the server
        message_to_send = input("Enter message to send to the server (type 'exit' to quit): ")
        if message_to_send.lower() == 'exit':
            break  # Break the loop if the user wants to exit

        message = message_to_send.encode()
        encrypted_response, iv_response = aes_encrypt(message, aes_key)
        client.send(iv_response + encrypted_response)

except KeyboardInterrupt:
    pass
finally:
    client.close()
