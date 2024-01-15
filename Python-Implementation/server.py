import socket
import sys
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad, unpad
import threading
from ggh import *
import pickle


def aes_encrypt(data, key):
    cipher = AES.new(key, AES.MODE_CBC)
    ct_bytes = cipher.encrypt(pad(data, AES.block_size))
    iv = cipher.iv
    return ct_bytes, iv

def aes_decrypt(data, key, iv):
    cipher = AES.new(key, AES.MODE_CBC, iv=iv)
    decrypted_data = unpad(cipher.decrypt(data), AES.block_size)
    return decrypted_data

def client_thread(conn, address, privateB, badVector):
    print(f"Connected to {address}")
    try:
        publicB = np.matmul(privateB, badVector)
        write_public_key_to_file(publicB)
        conn.send(publicB.tobytes())

        while True:
            # Wait for client to send encrypted AES key
            encrypted_aes_key = conn.recv(4096)
            matrix = np.frombuffer(encrypted_aes_key).reshape((10,10))

            # Decrypt AES key
            aes_key = decrypt(matrix, privateB, badVector)
            print(f"AES key received and decrypted: {aes_key}")

            # Now send and receive encrypted messages using AES
            while True:
                message_to_send = input("Enter message to send to the client: ")
                if message_to_send.lower() == 'exit':
                    break

                message = message_to_send.encode()
                encrypted_message, iv = aes_encrypt(message, aes_key)
                conn.send(iv + encrypted_message)
                print("Encrypted message sent to the client.")

                # Receive and decrypt the client's response
                received_data = conn.recv(4096)
                print(f"The received data is: {received_data[AES.block_size:]}")
                received_iv = received_data[:AES.block_size]
                received_message = aes_decrypt(received_data[AES.block_size:], aes_key, received_iv)
                print(f"Decrypted message from the client: {received_message.decode()}")

    except Exception as e:
        print("An error occurred:", e)
    finally:
        conn.close()

# Setup server...
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Define host and port
host = '127.0.0.1'
port = 65432

# Bind the socket to the host and port
server.bind((host, port))

# Listen for incoming connections
server.listen()

# Generate private B and badVector (assumed to be GGH keypair)
with open('key.pkl', 'rb') as file:
    loaded_variable = pickle.load(file)
privateB, _, badVector = loaded_variable

print("Server up and listening for connections...")

try:
    while True:
        client_conn, client_address = server.accept()
        client_thread = threading.Thread(target=client_thread, args=(client_conn, client_address, privateB, badVector))
        client_thread.start()

except KeyboardInterrupt:
    print("Server shutting down...")
    server.close()
    sys.exit()
