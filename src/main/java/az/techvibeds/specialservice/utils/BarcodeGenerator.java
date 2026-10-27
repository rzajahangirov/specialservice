package az.techvibeds.specialservice.utils;

import java.security.SecureRandom;

/**
 * EAN-13 formatında standart barkod generasiya edən yardımçı sinif.
 * EAN-13: 13 rəqəmli beynəlxalq standart barkod formatıdır.
 * "2" ilə başlayan kodlar daxili/mağaza istifadəsi üçün ayrılmışdır.
 *
 * Format: 2 + [companyId 4 rəqəm] + [random 6 rəqəm] + [check digit]
 */
public class BarcodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * EAN-13 formatında unikal barkod generasiya edir.
     * @param companyId Şirkət ID-si (barkodun daxilində kodlanır)
     * @return 13 rəqəmli EAN-13 barkod
     */
    public static String generateEAN13(Long companyId) {
        // Prefix: "2" (daxili istifadə üçün ayrılmış)
        String prefix = "2";

        // Şirkət ID-sini 4 rəqəmə formatla
        String companyPart = String.format("%04d", companyId % 10000);

        // 6 rəqəmli random hissə
        String randomPart = String.format("%06d", RANDOM.nextInt(1000000));

        // İlk 12 rəqəm
        String first12 = prefix + companyPart + randomPart + generateRandomDigit();

        // EAN-13 check digit hesabla
        int checkDigit = calculateEAN13CheckDigit(first12);

        return first12 + checkDigit;
    }

    /**
     * EAN-13 check digit hesablayır.
     * Standart alqoritm: tek pozisiyalarda x1, cüt pozisiyalarda x3 vururuq,
     * cəmi 10-a tam bölünən ən yaxın ədədə tamamlayırıq.
     *
     * @param first12Digits Barkodun ilk 12 rəqəmi
     * @return Check digit (0-9)
     */
    public static int calculateEAN13CheckDigit(String first12Digits) {
        if (first12Digits.length() != 12) {
            throw new IllegalArgumentException("EAN-13 check digit üçün 12 rəqəm lazımdır, verildi: " + first12Digits.length());
        }

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(first12Digits.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }

        int remainder = sum % 10;
        return (remainder == 0) ? 0 : (10 - remainder);
    }

    /**
     * Verilən barkodun keçərli EAN-13 formatında olub-olmadığını yoxlayır.
     */
    public static boolean isValidEAN13(String barcode) {
        if (barcode == null || barcode.length() != 13) {
            return false;
        }

        for (char c : barcode.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        String first12 = barcode.substring(0, 12);
        int expectedCheck = calculateEAN13CheckDigit(first12);
        int actualCheck = Character.getNumericValue(barcode.charAt(12));

        return expectedCheck == actualCheck;
    }

    private static String generateRandomDigit() {
        return String.valueOf(RANDOM.nextInt(10));
    }
}
