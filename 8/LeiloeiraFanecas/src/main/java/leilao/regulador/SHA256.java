package leilao.regulador;

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256 {

    public static String generate(byte[] toHash) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(toHash);
        byte[] digest = messageDigest.digest();
        return new String(Hex.encodeHex(digest));
    }
}

