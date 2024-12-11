package com.omkar.Zoho_CRM.util;

public class PhoneNumberUtil {

    public static String normalizeNumber(String number) {
        if (number == null || number.isEmpty()) {
            return null;
        }
        // Remove non-numeric characters
        String cleanedNumber = number.replaceAll("\\D", "");

        // Remove leading 0 if present
        if (cleanedNumber.startsWith("0")) {
            cleanedNumber = cleanedNumber.substring(1);
        }
        return cleanedNumber;
    }
}
