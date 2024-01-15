import math
import random
import numpy as np
import base64
import binascii
from scipy import linalg

#########################################
# GGH Algorithm Implementation
#########################################

def keyGeneration(n):
    privateB = np.identity(n)
    # print("Private Key (Orthogonal Basis):\n", privateB)

    publicB = None
    while True:
        badVector = rand_unimod(n)
        temp = np.matmul(privateB, badVector)
        ratio = hamdamard_ratio(temp, n)
        if ratio <= .1:
            publicB = temp
            break
    # print("Public Key (Nearly Parallel Basis):\n", publicB)
    return privateB, publicB, badVector

def encrypt(message: bytes, publicB: np.ndarray):
    # Directly encode the message in Base64
    key_matrix = np.frombuffer(message, dtype=np.uint8).reshape((4, 4))
    padded_key_matrix = np.zeros((10, 10))
    padded_key_matrix[:4, :4] = key_matrix
    encrypted = np.matmul(padded_key_matrix, publicB)
    return encrypted

def decrypt(encrypted, privateBasis, unimodular):
    A = privateBasis
    x = encrypted
    B_PRIME = np.linalg.inv(A)
    BB = np.matmul(B_PRIME, x)
    unimodular_inverse = np.linalg.inv(unimodular)
    m = np.round(np.matmul(BB, unimodular_inverse)).astype(np.uint8)
    original_matrix = m[:4, :4]
    original_bytes = original_matrix.tobytes()

    return original_bytes

def hamdamard_ratio(basis, dimension):
    det_of_lattice = np.linalg.det(basis)
    mult = np.prod([np.linalg.norm(v) for v in basis])
    h_ratio = (det_of_lattice / mult) ** (1.0 / dimension)
    return h_ratio

def rand_unimod(n):
    random_matrix = [[np.random.randint(-10, 10) for _ in range(n)] for _ in range(n)]
    upper_tri = np.triu(random_matrix, 0)
    lower_tri = [[np.random.randint(-10, 10) if x < y else 1 if x == y else 0 for x in range(n)] for y in range(n)]
    for i in range(n):
        if bool(random.getrandbits(1)):
            upper_tri[i][i] = lower_tri[i][i] = 1
        else:
            upper_tri[i][i] = lower_tri[i][i] = -1
    unimodular = np.matmul(upper_tri, lower_tri)
    return unimodular

#########################################
# Utility and Debugging Methods
#########################################

def show_message(message):
    print("Message:")
    for value in message:
        letter = chr(abs(value))
        print(letter, end="")
    print()

def write_public_key_to_file(publicB, filename='ggh_public_key.txt'):
    with open(filename, 'w') as file:
        file.write("------BEGIN GGH PUBLIC KEY BLOCK-----\n")
        for row in publicB:
            file.write(' '.join(str(x) for x in row) + '\n')
        file.write("------END GGH PUBLIC KEY BLOCK-------\n")

def read_public_key_from_file(filename='ggh_public_key.txt'):
    with open(filename, 'r') as file:
        lines = file.readlines()
        lines = lines[1:-1]  # Remove the first and last lines (headers)
        publicB = np.array([[int(num) for num in line.split()] for line in lines])
    return publicB

def display_matrix(matrix):
    for row in matrix:
        print(" ".join(str(col) for col in row))
