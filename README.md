# Applied Cryptography Project: Quantum Safe Chat App
This project is about the proposed implementation of a Quantum Safe Chat app which can be used to mitigate the risk of Store Now Decrypt Later attacks on RSA and other prime factorization/logarithmic cryptographic methods of Key Gen/Encryption. 

This project uses GGH, a quantum safe, latice based cryptography method to encrypt the AES Keys for a secure communication channel.

## How to run Python Implementation
- Run server.py

```sh
python3 server.py
```

- Run client.py

```sh
python3 client.py
```

## How to build and install QChat Android App.
- You will need Android Studio installed. 
- Open the project in Android Studio 
- Click on build 
- Install by sending the apk over to phone or use adb

```sh
adb install app-release.apk
```
