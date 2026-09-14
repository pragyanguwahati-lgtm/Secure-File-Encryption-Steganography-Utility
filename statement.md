# Project Statement: Secure File Encryption & Steganography Utility

## 1. Problem Statement
In digital communications, transmitting confidential data across untrusted networks poses significant security and surveillance challenges. Standard encryption protocols (such as AES or RSA) secure file contents cryptographically; however, transmitting encrypted ciphertext openly advertises that sensitive data is being shared, making it an immediate target for interception, traffic analysis, and cryptanalysis.

Conversely, steganography conceals the very existence of a communication by embedding secret payloads within innocuous carrier files, such as digital images. However, traditional steganography applied alone lacks cryptographic confidentiality—if an adversary intercepts the cover medium and suspects or identifies the embedding algorithm, the hidden content is completely exposed. Furthermore, data embedded inside carrier media is susceptible to transmission corruption or active bit tampering without any mechanism to alert the receiver.

There is a critical need for a unified, defense-in-depth utility that combines:
1. **Cryptographic Confidentiality**: Transforming plaintext into undecipherable ciphertext.
2. **Steganographic Obscurity**: Hiding the ciphertext within carrier media to eliminate suspicion.
3. **Data Integrity Verification**: Detecting transmission corruption, incomplete extraction, or intentional tampering before payload delivery.

---

## 2. Scope of the Project
The **Secure File Encryption & Steganography Utility** is developed as a lightweight, cross-platform Command-Line Interface (CLI) application in Java (compatible with JDK 8 and above).

### In-Scope:
* **Carrier Format**: 24-bit and 32-bit lossless image formats (specifically PNG), ensuring that image compression algorithms do not alter or destroy embedded bitstreams.
* **Payload Flexibility**: Any arbitrary file format (plain text, PDFs, documents, binaries) can be hidden, provided its size does not exceed the carrier image's bit capacity.
* **Cryptographic Standards**: Industry-standard symmetric encryption (`AES-256-CBC`) combined with robust password-based key derivation (`PBKDF2WithHmacSHA256` with 65,536 iterations, dynamic 16-byte salt, and 16-byte random IV).
* **LSB Steganography**: Sequential bit embedding into the Least Significant Bits (LSB) across the Red, Green, and Blue color channels.
* **Data Integrity**: Cryptographic pre-encryption hashing with `SHA-256` and constant-time digest comparison on recovery.
* **Payload Identification & Safety**: 4-byte magic signature identification (`STEG`) ensuring non-steganographic images are rejected immediately without memory overload.
* **Capacity Inspection**: On-demand CLI analysis of cover image dimensions, maximum embedding capacity, and steganography signature presence.
* **Headless / Terminal Execution**: 100% terminal-executable without reliance on graphical windowing environments (GUI), ensuring compatibility with servers, scripts, and automated evaluation environments.

### Out-of-Scope:
* Lossy image carriers (such as JPEG/WebP) due to transform compression altering LSB bit values.
* Audio/video carrier steganography.
* Network socket transport layer (the utility operates as an end-to-end file transformation tool).

---

## 3. Target Users
* **Privacy-Conscious Individuals**: Users seeking to safeguard personal documents, sensitive credentials, or private records before cloud backup or transmission.
* **Security Auditors & Penetration Testers**: Professionals testing steganographic exfiltration defenses, security controls, and forensic detection mechanisms.
* **Journalists & Whistleblowers**: Individuals operating in heavily monitored environments where the mere transmission of encrypted data can attract targeted interception.
* **Academic Students & Researchers**: Students learning applied cryptography, Java Cryptography Architecture (JCA), bitwise binary manipulation, and defensive programming in academic settings.

---

## 4. High-Level Features

| Feature Module | Description | Technical Implementation |
| :--- | :--- | :--- |
| **Dual-Layer Hide (`--hide`)** | Combines integrity hashing, symmetric encryption, and LSB embedding in a single workflow. | Computes SHA-256, encrypts payload via `AES-256-CBC` with PBKDF2 key derivation, prepends magic header + length, and embeds bits across RGB channels. |
| **Extract & Verify (`--extract`)** | Extracts hidden bitstream, checks signature, decrypts ciphertext, and verifies integrity. | Validates `STEG` magic signature, decrypts with provided password, recalculates SHA-256 in constant time, and exports the restored file. |
| **Capacity Inspector (`--info`)** | Inspects cover images and evaluates payload suitability without modifying files. | Reads image metadata, computes maximum allowable payload bytes, and probes for existing stego signatures. |
| **Integrity Guard** | Prevents delivery of corrupted or modified files. | Throws `TamperedFileException` if even a single bit of the carrier image's payload is altered. |
| **Secure Memory Sanitation** | Mitigates memory dump and scraping vulnerabilities. | Uses `char[]` password arrays and securely zeroes out memory buffers immediately after cryptographic key derivation. |
| **Automated Test Harness** | Self-contained validation suite verifying functional and negative security test cases. | Validates roundtrip hiding, invalid passwords, bit tampering, capacity overflow, and non-stego detection. |
