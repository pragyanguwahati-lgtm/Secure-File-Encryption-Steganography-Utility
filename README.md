# Secure File Encryption & Steganography Utility (SecTool)

A modular, production-grade Java command-line utility for securely encrypting arbitrary files and embedding them invisibly within digital images using Least Significant Bit (LSB) steganography.

---

## 🚀 Features

* **Military-Grade Cryptography**: Symmetric `AES-256-CBC` encryption combined with `PBKDF2WithHmacSHA256` key derivation (65,536 iterations, cryptographically secure 16-byte random salt, and 16-byte random IV).
* **LSB Steganography**: Sequential bit embedding into the Least Significant Bits of Red, Green, and Blue channels of lossless PNG cover images, remaining imperceptible to the human visual system.
* **Cryptographic Integrity Guard**: Computes and encapsulates a `SHA-256` digest prior to encryption. Verified in constant time upon extraction to reliably catch bit flips, corruption, or intentional tampering (`TamperedFileException`).
* **Magic Header Validation**: Uses a 4-byte `STEG` file signature to immediately reject non-stego carrier images or incompatible payloads before memory allocation.
* **Cover Image Capacity Inspector (`--info`)**: Probes cover images directly from the CLI to report dimensions, total pixel counts, maximum usable payload capacity, and signature presence without altering files.
* **Defensive Memory Sanitization**: Sensitive password buffers (`char[]`) and key specifications are immediately wiped from memory with zeroes post-derivation to thwart memory scraping/dump attacks.
* **Automated Validation Test Suite**: Standalone, zero-dependency test runner (`TestRunner`) testing roundtrip fidelity, invalid password rejection, tamper detection, capacity bounds, and non-stego handling.
* **100% CLI Executable**: Fully operable via command line in headless environments (Windows, Linux, macOS) without GUI dependencies.

---

## 📋 Prerequisites & Technologies

* **Java Development Kit (JDK)**: Version 8 or higher (`java` and `javac` added to system PATH).
* **Carrier Medium**: Lossless image format (`.png`). Lossy compression formats (e.g., JPEG) will alter LSB bit values and destroy hidden payloads.
* **Core Libraries Used**:
  * `javax.crypto`: AES-256 cipher, PBKDF2 key generation, IV specifications.
  * `java.security`: `MessageDigest` (SHA-256), `SecureRandom`.
  * `java.awt.image` & `javax.imageio`: Pixel-level raster manipulation and lossless image I/O.

---

## 📁 Project Architecture

```
Secure-File-Encryption-Steganography-Utility/
├── src/
│   ├── com/
│   │   └── stego/
│   │       ├── cli/
│   │       │   └── MainCLI.java                 # Entry point, argument parsing, CLI logging
│   │       ├── core/
│   │       │   └── StegoService.java            # LSB bitwise embedding/extraction & STEG header
│   │       ├── crypto/
│   │       │   ├── CryptoService.java           # AES-256-CBC & PBKDF2 key derivation
│   │       │   └── IntegrityValidator.java      # SHA-256 hashing & constant-time validation
│   │       ├── exception/
│   │       │   ├── CapacityExceededException.java
│   │       │   ├── InvalidPasswordException.java
│   │       │   └── TamperedFileException.java
│   │       ├── test/
│   │       │   ├── GenerateTestImage.java       # Utility to generate clean cover image
│   │       │   ├── TamperImage.java             # Utility to inject bit-flip tampering
│   │       │   └── TestRunner.java              # Automated test suite
│   │       └── util/
│   │           └── FileHandlerUtil.java         # Byte-level and image file I/O
│   └── MainCLI.java                             # Root forwarder for backward compatibility
├── build.bat                                    # Windows automated build script
├── build.sh                                     # Linux/macOS automated build script
├── statement.md                                 # Project statement & scope specification
└── README.md                                    # Project documentation
```

---

## 🛠️ Build & Compilation

### Option 1: Automated Script (Recommended)

* **Windows**:
  ```cmd
  build.bat
  ```
* **Linux / macOS**:
  ```bash
  chmod +x build.sh
  ./build.sh
  ```

### Option 2: Manual Terminal Compilation

Compile all source files into the `bin` directory and package into `SecTool.jar`:

```bash
# Compile
javac -d bin src/com/stego/exception/*.java src/com/stego/crypto/*.java src/com/stego/util/*.java src/com/stego/core/*.java src/com/stego/cli/*.java src/com/stego/test/*.java src/MainCLI.java

# Package executable JAR
jar cfe SecTool.jar com.stego.cli.MainCLI -C bin .
```

---

## 💻 Usage & CLI Guide

You can run the utility using either `java -jar SecTool.jar` or `java -cp bin com.stego.cli.MainCLI`.

### 1. Hide Data (Encrypt & Embed)

```bash
java -jar SecTool.jar --hide -f <target_file> -i <cover_image.png> -o <stego_image.png>
```
*Aliases: `-f` / `--target`, `-i` / `--cover`, `-o` / `--out`*

**Example Session:**
```bash
$ java -jar SecTool.jar --hide -f secret.txt -i test_image.png -o hidden.png
[INFO] Reading target file...
[INFO] Generating SHA-256 integrity hash...
Enter encryption password: 
[INFO] Encrypting data with AES-256...
[INFO] Reading cover image...
[INFO] Embedding encrypted payload into image...
[INFO] Saving stego image...
[SUCCESS] Data hidden successfully in hidden.png
```

---

### 2. Extract Data (Decrypt & Verify)

```bash
java -jar SecTool.jar --extract -i <stego_image.png> -o <extracted_file>
```
*Aliases: `-i` / `--image`, `-o` / `--out`*

**Example Session:**
```bash
$ java -jar SecTool.jar --extract -i hidden.png -o output.txt
[INFO] Reading stego image...
[INFO] Extracting payload from image...
Enter decryption password: 
[INFO] Decrypting data...
[INFO] Verifying SHA-256 integrity hash...
[INFO] Writing restored file...
[SUCCESS] Hash matched. Data extracted to output.txt
```

---

### 3. Analyze Image Capacity (`--info`)

Inspect dimensions, byte capacity, and steganography signature presence without modifying files:

```bash
java -jar SecTool.jar --info -i <image.png>
```

**Example Output:**
```
=================================================
           COVER IMAGE ANALYSIS REPORT           
=================================================
 Image Path          : test_image.png
 Dimensions          : 800 x 600 pixels
 Total Pixels        : 480,000
 Total LSB Capacity  : 180,000 bytes (175.78 KB)
 Usable Payload Max  : 179,992 bytes (175.77 KB)
 Stego Signature     : [NONE] Clean cover image
=================================================
```

---

## 🧪 Automated Validation Testing

The repository includes a comprehensive automated test runner to validate security invariants and functionality across 5 key dimensions:

```bash
java -cp bin com.stego.test.TestRunner
```

**Test Execution Output:**
```
=============================================================
           SECTOOL AUTOMATED VALIDATION TEST SUITE           
=============================================================
[PASS] Roundtrip Encryption & Steganography          : PASSED
[PASS] Invalid Password Rejection                    : PASSED
[PASS] Bit Tampering & Integrity Verification        : PASSED
[PASS] Capacity Limit Bounds Check                   : PASSED
[PASS] Non-Stego Clean Image Header Rejection        : PASSED
-------------------------------------------------------------
Summary: 5 / 5 Tests Passed (100.0% Success Rate)
=============================================================
```

### Manual Negative Tests:

1. **Incorrect Password**:
   ```bash
   java -jar SecTool.jar --extract -i hidden.png -o output.txt
   # Enter wrong password -> Triggers [ERROR] Decryption failed. Incorrect password or corrupted payload.
   ```
2. **Tampering with Stego Image**:
   ```bash
   java -cp bin com.stego.test.TamperImage hidden.png tampered.png
   java -jar SecTool.jar --extract -i tampered.png -o output.txt
   # Decryption or integrity verification catches the altered bits and aborts.
   ```

---

## 🔒 Security Architecture Summary

```
[ Target Plaintext File ]
           │
           ▼
[ SHA-256 Integrity Hash ] ── Prepend ──► [ 32-Byte Digest + Plaintext ]
                                                         │
                                                         ▼
[ AES-256-CBC Encryption ] ◄── PBKDF2 (Password + 16B Salt, 65,536 Iterations)
           │
           ▼
[ Salt (16B) ] + [ IV (16B) ] + [ Ciphertext ]
           │
           ▼
[ STEG Magic Header (4B) ] + [ Payload Length (4B) ] + [ Encrypted Payload ]
           │
           ▼
[ LSB Bitwise Embedding into RGB Channels of Cover PNG ]
```

---

## 📄 License & Academic Integrity
Developed for academic evaluation in **CSE2006: Programming in Java** at **VIT Bhopal University**.
All cryptographic logic and byte-level steganography algorithms are natively implemented using standard Java SE APIs.
