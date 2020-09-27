package com.ins.bot.common;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class InstagramAes {
	
	public static String enc(int key, String pkey, String password) throws GeneralSecurityException {
		String time = String.valueOf(System.currentTimeMillis() / 1000);
        int overheadLength = 48;
        byte[] pkeyArray = new byte[pkey.length() / 2];
        for (int i = 0; i < pkeyArray.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(pkey.substring(index, index + 2), 16);
            pkeyArray[i] = (byte) j;
        }

        byte [] y = new byte[password.length()+36+16+overheadLength];

        int f = 0;
        y[f] = 1;
        y[f += 1] = (byte)key;
        f += 1;

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);

        // Generate Key
        SecretKey secretKey = keyGenerator.generateKey();
        byte[] IV = new byte[12];

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        cipher.updateAAD(time.getBytes());

        byte [] sealed = SealedBoxUtility.crypto_box_seal(secretKey.getEncoded(),pkeyArray);
        byte[] cipherText = cipher.doFinal(password.getBytes());
        y[f] = (byte) (255 & sealed.length);
        y[f + 1] = (byte) (sealed.length >> 8 & 255);
        f += 2;
        for(int j=f;j<f+sealed.length;j++){
            y[j] = sealed[j-f];
        }
        f += 32;
        f += overheadLength;

        byte [] c = Arrays.copyOfRange(cipherText,cipherText.length -16,cipherText.length);
        byte [] h = Arrays.copyOfRange(cipherText,0,cipherText.length - 16);

        for(int j=f;j<f+c.length;j++){
            y[j] = c[j-f];
        }
        f += 16;
        for(int j=f;j<f+h.length;j++){
            y[j] = h[j-f];
        }
        return "#PWD_INSTAGRAM_BROWSER:10:"+time+":" + Base64.getEncoder().encodeToString(y);
    }
}
