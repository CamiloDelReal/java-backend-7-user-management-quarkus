@echo off
@rem User Management Service
echo [!] Creating keys
openssl req -newkey rsa:2048 -new -nodes -keyout privatekey.pem -out csr.pem
openssl rsa -in privatekey.pem -pubout > publickey.pem
echo [+] Keys created