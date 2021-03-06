package com.diplomskirad.celestin.diplomskiv2;

import java.nio.ByteBuffer;

/**
 * Created by celes on 27/01/2017.
 */

public class Utils {
    public static int parseUnsignedInt(String s, int radix)
            throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("null");
        }

        int len = s.length();
        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar == '-') {
                throw new
                        NumberFormatException(String.format("Illegal leading minus sign " +
                        "on unsigned string %s.", s));
            } else {
                if (len <= 5 || // Integer.MAX_VALUE in Character.MAX_RADIX is 6 digits
                        (radix == 10 && len <= 9)) { // Integer.MAX_VALUE in base 10 is 10 digits
                    return Integer.parseInt(s, radix);
                } else {
                    long ell = Long.parseLong(s, radix);
                    if ((ell & 0xffffffff00000000L) == 0) {
                        return (int) ell;
                    } else {
                        throw new
                                NumberFormatException(String.format("String value %s exceeds " +
                                "range of unsigned int.", s));
                    }
                }
            }
        } else {
            throw new NumberFormatException(" String is wrong");
        }
    }


    public static int acsTransparentToInt(int trasparent) {

        byte[] b = ByteBuffer.allocate(4).putInt(trasparent).array();
        return ByteBuffer.wrap(b).getInt();
    }


    public static int oneIntToTransparent(int reg) {

        byte[] b = ByteBuffer.allocate(4).putInt(reg).array();
        String prefixZero = "0000000000000000";
        String prefixOne = "1111111111111111";
        int helper = ByteBuffer.wrap(b).getInt();
        String binary = Integer.toBinaryString(helper);
        StringBuilder sb = new StringBuilder();
        int lenghtEmpty = 16 - binary.length();

        for (int i = 0; i < lenghtEmpty; i++) {
            sb.append(0);
        }

        sb.append(binary);
        String complete = sb.toString();

        if (complete.charAt(0) == '1') {
            return parseUnsignedInt((prefixOne + complete), 2);

        } else {
            return parseUnsignedInt((prefixZero + complete), 2);
        }
    }


    public static float twoIntsToACSTransparent(int reg1, int reg2, int scaleValue) {

        int numberHelper;
        byte[] b1 = ByteBuffer.allocate(4).putInt(reg1).array();
        byte[] b2 = ByteBuffer.allocate(4).putInt(reg2).array();
        byte[] b32bit = {b2[2], b2[3], b1[2], b1[3]};
        numberHelper = ByteBuffer.wrap(b32bit).getInt();
        String helper = Integer.toBinaryString(numberHelper);
        return (float) parseUnsignedInt(helper, 2) / scaleValue;
    }


    public static boolean getBitState(int offset, int regValue) {
        String binary = Integer.toBinaryString(regValue);
        StringBuilder sb = new StringBuilder();
        int lenghtEmpty = 16 - binary.length();

        for (int i = 0; i < lenghtEmpty; i++) {
            sb.append(0);
        }

        sb.append(binary);
        String complete = sb.toString();
        //Log.d("cele", "Status word is " + complete);
        char[] binaryArray = complete.toCharArray();

        return binaryArray[15 - offset] != '0';
    }

}