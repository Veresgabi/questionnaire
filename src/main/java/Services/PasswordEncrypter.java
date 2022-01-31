package Services;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.security.Key;
import java.security.spec.KeySpec;

@Service
public class PasswordEncrypter {

    private static final String TRIPLE_DES_KEY_SPEC = "DESede";
    private static final String TRIPLE_DES = "DESede/ECB/PKCS5Padding";
    private static final String KEY_STRING =
        "21-199-217-127-162-182-251-137-227-56-131-242-191-224-21-97-146-158-152-21-124-70-127-91";

    public static String encrypt(String plainText) throws Exception {
        String encryptedString = null;
        try {
            Key key = getKey();
            Cipher desCipher = Cipher.getInstance(TRIPLE_DES);
            desCipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cleartext = plainText.getBytes();
            byte[] ciphertext = desCipher.doFinal(cleartext);
            encryptedString = new String(Base64.encodeBase64(ciphertext));
        } catch (Throwable t) {
            throw new Exception("Error detcted while encoding a string", t);
        }
        return encryptedString;
    }

    public static String decrypt(String source){
        try {
            Key key=getKey();
            Cipher desCipher=Cipher.getInstance(TRIPLE_DES);
            byte[] dec= Base64.decodeBase64(source.getBytes());
            desCipher.init(Cipher.DECRYPT_MODE,key);
            byte[] cleartext=desCipher.doFinal(dec);
            return new String(cleartext);
        }
        catch (  Throwable t) {
            throw new RuntimeException("Error decrypting string",t);
        }
    }

    public static Key getKey() {
        try {
            byte[] bytes = KEY_STRING.getBytes();
            DESedeKeySpec pass = new DESedeKeySpec(bytes);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(TRIPLE_DES_KEY_SPEC);
            SecretKey s = skf.generateSecret(pass);
            return s;
        } catch (Throwable t) {
            throw new RuntimeException("Error creating key", t);
        }
    }
}
