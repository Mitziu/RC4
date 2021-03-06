import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;

public class RC4 {

    private byte[] PermutationOfBytes = new byte[256];

    private String OPTION;
    private String KEY_FILE;
    private String CIPHERTEXT_FILE;
    private String PLAINTEXT_FILE;

    public RC4 (String OPTION, String KEY_FILE, String CIPHERTEXT_FILE, String PLAINTEXT_FILE) {
        this.OPTION = OPTION;
        this.KEY_FILE = KEY_FILE;
        this.CIPHERTEXT_FILE = CIPHERTEXT_FILE;
        this.PLAINTEXT_FILE = PLAINTEXT_FILE;
    }

    /**
     * EncryptsOrDecrypts depending on the option
     */
    public void convert() {
        byte[] key = readFile(KEY_FILE);
        switch (OPTION) {
            case "encrypt":
                System.out.println("Encrypt");
                byte[] plaintext = readFile(PLAINTEXT_FILE);
                encrypt(plaintext, key);
                break;
            case "decrypt":
                System.out.println("Decrypt");
                byte[] cipher = readFile(CIPHERTEXT_FILE);
                decrypt(cipher, key);
                break;
            default:
                throw new IllegalArgumentException("OPTION can only be encrypt / decrypt");
        }
    }

    /**
     * Method to encrypt the plaintext from the ciphertext file
     * @param bytesFromFile Array bytes from file
     * @param keyFile Array bytes from key file
     * @return Message of Cypher text + IV
     */
    public byte[] encrypt (byte[] bytesFromFile, byte[] keyFile) {
        ByteBuffer buffer = ByteBuffer.wrap(bytesFromFile);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] plainText = new byte[buffer.getInt()];

        for (int i = 0; i < plainText.length; i++) {
            plainText[i] = buffer.get();
        }

        buffer = ByteBuffer.wrap(keyFile);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] key = new byte[buffer.getInt()];

        for (int i = 0; i < key.length; i++) {
            key[i] = buffer.get();
        }

        System.out.println("Plaintext: ");
        displayBytes(plainText);

        System.out.println("Key: ");
        displayBytes(key);

        byte[] IV = generateIV();

        byte[] seed = XORKeyIV(key, IV);

        initialization(seed);

        byte[] keyStream = generateKeyStream(plainText.length);
        byte[] cipher_Text = new byte[plainText.length];

        for (int i = 0; i < plainText.length; i++) {
            cipher_Text[i] = (byte) (plainText[i] ^ keyStream[i]);
        }

        return writeOutCipher(cipher_Text, IV);
    }

    /**
     * Scrambles up the 256 possible bytes
     * @param key
     */
    private void initialization (byte[] key) {
        for (int i = 0; i < PermutationOfBytes.length; i++) {
            PermutationOfBytes[i] = (byte) i;
        }

        int j = 0;
        for (int i = 0; i < PermutationOfBytes.length; i++) {
            j = (j + Byte.toUnsignedInt(PermutationOfBytes[i]) + Byte.toUnsignedInt(key[i % key.length])) % 256;

            Byte temp = PermutationOfBytes[i];
            PermutationOfBytes[i] = PermutationOfBytes[j];
            PermutationOfBytes[j] = temp;
        }
    }

    /**
     * Generates the IV using the SecureRandom class
     * @return IV pseudo randomly generated by SecureRandom class
     */
    private byte[] generateIV () {
        SecureRandom secureRandom = new SecureRandom();
        byte[] IV = new byte[32];
        Integer i = 0;
        for(byte b: secureRandom.generateSeed(32)) {
            IV[i++] = b;
        }

        return IV;
    }

    /**
     * XORs KEY AND IV
     * @param Key Byte of key
     * @param IV Byte of IV
     * @return byte array ( KEY XOR IV)
     */
    private byte[] XORKeyIV(byte[] Key , byte[] IV) {
        byte[] keyPadded = new byte[32];
        for(int i = 0; i < keyPadded.length; i++) {
            keyPadded[i] = (byte) 0;
        }

        for(int i = 0; i < Key.length; i++) {
            keyPadded[keyPadded.length - Key.length + i] = Key[i];
        }

        byte[] XORed = new byte[32];

        for(int i = 0; i < XORed.length; i++) {
            XORed[i] = (byte) (IV[i] ^ (keyPadded[i]));
        }

       return XORed;
    }

    /**
     * Writes the cipher text with the IV to allow for later decryption
     * @param cipher_Text Cipher Text to write out
     * @param IV IV to write in the cipher text
     */
    private byte[] writeOutCipher (byte[] cipher_Text, byte[] IV) {
        System.out.println("Cipher Text");
        displayBytes(cipher_Text);

        System.out.println("IV");
        displayBytes(IV);

        ByteBuffer buffer = ByteBuffer.
                allocate(4 + cipher_Text.length + IV.length);

        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(cipher_Text.length + IV.length);

        for (Byte b : IV) {
            buffer.put(b);
        }

        for (Byte b: cipher_Text) {
            buffer.put(b);
        }

        buffer.flip();

        File cipherFile = new File(CIPHERTEXT_FILE);

        try (FileChannel channel = new FileOutputStream(cipherFile, false).getChannel()) {
            channel.write(buffer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer.array();
    }


    /**
     * Decrypts plaintext from the bytes in the cipher text
     * @param bytesFromFile Array of bytes read from the cipher text file
     * @param keyFile array of bytes read from the key
     * @return Cipher_Text array of bytes
     */
    public byte[] decrypt (byte[] bytesFromFile, byte[] keyFile) {
        ByteBuffer buffer = ByteBuffer.wrap(bytesFromFile);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int size = buffer.getInt();

        byte[] IV = new byte[32];
        byte[] cipherText = new byte[size - 32];

        for (int i = 0; i < IV.length; i++) {
            IV[i] = buffer.get();
        }

        for (int i = 0; i < cipherText.length; i++) {
            cipherText[i] = buffer.get();
        }

        buffer = ByteBuffer.wrap(keyFile);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] key = new byte[buffer.getInt()];

        for (int i = 0; i < key.length; i++) {
            key[i] = buffer.get();
        }

        System.out.println("Cipher Text");
        displayBytes(cipherText);

        System.out.println("Key: ");
        displayBytes(key);

        System.out.println("IV");
        displayBytes(IV);

        byte[] seed = XORKeyIV(key, IV);

        initialization(seed);

        byte[] keyStream = generateKeyStream(cipherText.length);
        byte[] plaintext = new byte[cipherText.length];

        for (int i = 0; i < cipherText.length; i++) {
            plaintext[i] = (byte) (cipherText[i] ^ keyStream[i]);
        }

        /**
         * Write method to printout plaintext
         */

        return writeOutPlaintext(plaintext);
    }


    private byte[] writeOutPlaintext (byte[] plaintext) {
        System.out.println("Plaintext: ");
        displayBytes(plaintext);
        ByteBuffer buffer = ByteBuffer.allocate(plaintext.length + 4);

        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(plaintext.length);

        for (Byte b: plaintext) {
            buffer.put(b);
        }
        buffer.flip();

        File plaintextFile = new File(PLAINTEXT_FILE);

        try (FileChannel channel = new FileOutputStream(plaintextFile, false).getChannel()) {
            channel.write(buffer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer.array();
    }

    /**
     * Method to generate the key stream
     * @param messageSize Size of message to decrypt/encrypt
     * @return keystream
     */
    private byte[] generateKeyStream (Integer messageSize) {
        byte[] keyStream = new byte[messageSize];
        int i = 0;
        int j = 0;

        for (int c = 0; c < 3072; c++) {
            j = (i + 1) % 256;
            j = (j + Byte.toUnsignedInt(PermutationOfBytes[i])) % 256;
            Byte temp = PermutationOfBytes[j];
            PermutationOfBytes[j] = PermutationOfBytes[i];
            PermutationOfBytes[i] = temp;
        }

        for (int ctr = 0; ctr < messageSize; ctr++) {
            i = (i + 1) % 256;
            j = (j + Byte.toUnsignedInt(PermutationOfBytes[i])) % 256;

            Byte temp = PermutationOfBytes[i];
            PermutationOfBytes[i] = PermutationOfBytes[j];
            PermutationOfBytes[j] = temp;

            Byte k = PermutationOfBytes[(Byte.toUnsignedInt(PermutationOfBytes[i]) + Byte.toUnsignedInt(PermutationOfBytes[j]))% 256];
            keyStream[ctr] = k;
        }

        return keyStream;
    }


    /**
     * DEBUG: Displays the bytes
     * @param byteArray Array of bytes to display
     */
    public static void displayBytes (byte[] byteArray) {
        for (Byte b: byteArray) {
            String binary = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            System.out.print(binary + " ");
        }

        System.out.println("");
    }

    /**
     * Reads the file and returns a byte array of all of them
     * @param FILE_NAME
     * @return
     */
    public byte[] readFile (String FILE_NAME) {
        Path path = Paths.get(FILE_NAME);

        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    /**
     * @param args OPTION, KEY , CIPHERTEXT_FILE , PLAINTEXT_FILE
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            throw new IllegalArgumentException("Must have 4 arguments. OPTION , KEY_FILE , CIPHERTEXT FILE , PLAINTEXT FILE");
        }

        RC4 rc4 = new RC4(args[0], args[1], args[2], args[3]);
        rc4.convert();

    }
}
