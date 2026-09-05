# Secure File Encryption & Steganography Utility

A Java-based command-line utility for securely encrypting files and hiding them within images (steganography) to ensure both confidentiality and obscurity of sensitive data.

## 🚀 Features

- **Robust Encryption**: Utilizes `AES-256-CBC` symmetric encryption with `PBKDF2WithHmacSHA256` key derivation for maximum security.
- **Steganography (LSB)**: Embeds your encrypted payload directly into the Least Significant Bits (LSB) of the Red, Green, and Blue channels of a PNG cover image, making it completely undetectable to the naked eye.
- **Data Integrity Validation**: Calculates and prepends a `SHA-256` hash of the target file prior to encryption. During extraction, this hash ensures the extracted data has not been tampered with or corrupted.
- **Secure Memory Management**: Passwords are wiped from memory immediately after deriving encryption keys to mitigate memory scraping attacks.
- **User-Friendly CLI**: Simple command-line arguments to hide and extract files seamlessly.

## 📋 Prerequisites

- **Java Development Kit (JDK)**: Version 8 or higher must be installed and added to your system's PATH.
- **Cover Image**: A lossless image format (like `.png`) to be used as the cover. Lossy formats like `.jpg` or `.jpeg` will corrupt the steganography payload due to their compression algorithms.

## 🛠️ Compilation

To compile the source code, open your terminal/command prompt, navigate to the `src` directory, and run the following command:

```bash
javac *.java
```

This will generate the `.class` files needed to run the application.

## 💻 Usage

Run the program from the directory containing your compiled `.class` files.

### 1. Hide Data (Encrypt & Embed)

To hide a secret file inside an image:

```bash
java MainCLI --hide --target <secret_file> --cover <cover_image.png> --out <stego_image.png>
```

- `--target`: The file you want to hide (e.g., `secret.txt`).
- `--cover`: The cover image that will hold the hidden data (e.g., `test_image.png`).
- `--out`: The resulting output image containing the hidden data (e.g., `hidden.png`).

**Example:**
```bash
java MainCLI --hide --target secret.txt --cover test_image.png --out hidden.png
```
*You will be prompted to enter a password securely.*

### 2. Extract Data (Decrypt & Verify)

To extract your hidden file from a stego image:

```bash
java MainCLI --extract --image <stego_image.png> --out <extracted_file>
```

- `--image`: The image containing the hidden data (e.g., `hidden.png`).
- `--out`: The path/filename where the extracted file should be saved (e.g., `output.txt`).

**Example:**
```bash
java MainCLI --extract --image hidden.png --out output.txt
```
*You will be prompted to enter the password you used during encryption.*

## 🏗️ Architecture & Security Workflow

1. **Hashing (Integrity)**: A SHA-256 hash of the target file is generated and attached to the data to prevent tampering.
2. **Encryption (Confidentiality)**: The file data and hash are encrypted using a 256-bit AES key derived from the user's password using PBKDF2. A unique Salt and IV are generated and attached to the ciphertext.
3. **Embedding (Obscurity)**: The total payload length (4 bytes) and the encrypted payload are embedded bit-by-bit into the RGB channels of the image pixels using Least Significant Bit (LSB) steganography.
4. **Extraction**: The process is reversed. If the password is correct, the file decrypts properly. The embedded SHA-256 hash is then verified against a newly generated hash of the decrypted file to guarantee data integrity.

## ⚠️ Important Notes

- **Capacity Limit**: The cover image must be large enough to hold the target file. The maximum payload an image can hold is `(Width × Height × 3) / 8` bytes.
- **Use PNG Format**: Never use JPEG for the output stego image, as its compression will destroy the hidden bits. Always use PNG.
- **Forgotten Passwords**: Since the system uses strong AES-256 encryption, if you forget your password, it is mathematically impossible to recover the hidden data.
