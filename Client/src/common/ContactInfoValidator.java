package common;

import java.util.regex.Pattern;

public final class ContactInfoValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[0-9][0-9\\-\\s()]{6,18}$"
    );

    private ContactInfoValidator() {
    }

    public static String requireName(String name, String fieldName) {
        String value = clean(name);
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value;
    }

    public static String requireEmail(String email) {
        String value = clean(email);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be empty.");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        return value;
    }

    public static String requirePhoneNumber(String phoneNumber) {
        String value = clean(phoneNumber);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Please enter a valid phone number.");
        }
        return value;
    }

    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isPlaceholderEmail(String email) {
        return clean(email).endsWith("@gonature.local");
    }
}
