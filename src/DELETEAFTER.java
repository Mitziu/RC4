import java.util.Random;

/**
 * Created by Mitziu on 2/9/17.
 */
public class DELETEAFTER {

    public static void main(String[] args) {
        Random myRandom = new Random();

        byte[] IV = new byte[32];
        byte[] key = new byte[20];

        for(int i = 0; i < IV.length; i++) {
            IV[i] = (byte) myRandom.nextInt(256);
        }

        for(int i = 0; i < key.length; i++) {
            key[i] = (byte) myRandom.nextInt(256);
        }
        //displayBytes(key);
        displayBytes(IV);
        XOR(IV, key);
    }

    public static byte[] XOR (byte[] IV, byte[] key) {
        byte[] keyPadded = new byte[32];

        for(int i = 0; i < keyPadded.length; i++) {
            keyPadded[i] = (byte) 0;
        }

        for(int i = 0; i < key.length; i++) {
            keyPadded[keyPadded.length - key.length + i] = key[i];
        }


        displayBytes(keyPadded);

        byte[] XORed = new byte[32];

        for(int i = 0; i < XORed.length; i++) {
            XORed[i] = (byte) (IV[i] ^ (keyPadded[i]));
        }

        displayBytes(XORed);

        return null;
    }

    public static void displayBytes (byte[] byteArray) {
        for (Byte b: byteArray) {
            String binary = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            System.out.print(binary + " ");
        }

        System.out.println("");
    }


}
