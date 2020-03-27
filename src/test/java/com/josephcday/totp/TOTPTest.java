package com.josephcday.totp;

import org.junit.Test;
import static org.junit.Assert.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TOTPTest {
    @Test
    public void testSecret() {
        assertEquals(TOTP.BASE32_CHAR_ARRAY.length, 32);
        String b32Secret = TOTP.b32Secret();
        assertEquals(b32Secret.length(), 16);
        b32Secret.chars().forEach(charInt -> {
            char[] chars = Character.toChars(charInt);
            assertEquals(TOTP.BASE32_CHARS.contains(String.valueOf(chars)), true);
        });
    }

    @Test
    public void testValidate() throws InvalidKeyException, NoSuchAlgorithmException {
        // http://localhost:8080/check?secret=QB5UDBW7OQKYYDZU&token=937384&unixtime=158524245
        assertEquals(TOTP.validate("QB5UDBW7OQKYYDZU", 111111, 158524245000L, 0), false);
        assertEquals(TOTP.validate("QB5UDBW7OQKYYDZU", 937384, 158524245000L, 0), true);
        assertEquals(TOTP.validate("QB5UDBW7OQKYYDZU", 937384, 158524200000L, 2), true);
        assertEquals(TOTP.validate("QB5UDBW7OQKYYDZU", 937384, 158524300000L, 5), true);

        assertEquals(TOTP.validate("QB5UDBW7OQKYYDZU", 111111, 158524245, 0), false);
        assertEquals(TOTP.validate("QB5UDBW7OQKYYDZU", 937384, 158524245, 0), true);
        assertEquals(TOTP.validate("QB5UDBW7OQKYYDZU", 937384, 158524200, 2), true);
        assertEquals(TOTP.validate("QB5UDBW7OQKYYDZU", 937384, 158524300, 5), true);
    }

    @Test
    public void testToken () throws InvalidKeyException, NoSuchAlgorithmException
    {
        assertEquals(TOTP.getToken("QB5UDBW7OQKYYDZU", 158524245000L), "937384");
        assertEquals(TOTP.getToken("QB5UDBW7OQKYYDZU", 158524245), "937384");
    }
}