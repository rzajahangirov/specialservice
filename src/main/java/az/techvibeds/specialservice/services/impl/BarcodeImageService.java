package az.techvibeds.specialservice.services.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.EAN13Writer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Barkod şəklini (PNG formatında) generasiya edən servis.
 * Malın üzərinə yapışdırmaq üçün stiker çap etmək məqsədilə istifadə olunur.
 */
@Service
public class BarcodeImageService {

    /**
     * Verilən barkod nömrəsi üçün PNG şəkli yaradır.
     * Şəklin altında barkod rəqəmləri, məhsul adı və satış qiyməti yazılır.
     *
     * @param barcodeText Barkod nömrəsi (EAN-13: 13 rəqəm)
     * @param productName Məhsulun adı (etiketin üstündə yazılacaq)
     * @param price Məhsulun satış qiyməti (etiketin altında yazılacaq)
     * @return PNG formatında şəklin byte massivi
     */
    public byte[] generateBarcodeImage(String barcodeText, String productName, java.math.BigDecimal price) throws Exception {
        int width = 300;
        int barcodeHeight = 100;

        // EAN-13 barkod şəkli yarat
        EAN13Writer writer = new EAN13Writer();
        Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.MARGIN, 10);
        BitMatrix bitMatrix = writer.encode(barcodeText, BarcodeFormat.EAN_13, width, barcodeHeight, hints);
        BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        // Etiket: barkod şəkli + altında nömrə + üstündə ad + altında qiymət
        int labelHeight = barcodeHeight + 100; // Qiymət üçün yer əlavə etdik
        BufferedImage label = new BufferedImage(width, labelHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = label.createGraphics();

        // Ağ arxa fon
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, labelHeight);

        // Məhsul adı (üstdə)
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fmName = g.getFontMetrics();
        String displayName = productName != null && productName.length() > 35
                ? productName.substring(0, 35) + "..."
                : (productName != null ? productName : "");
        int nameX = (width - fmName.stringWidth(displayName)) / 2;
        g.drawString(displayName, Math.max(5, nameX), 18);

        // Barkod şəkli (ortada)
        g.drawImage(barcodeImage, 0, 22, null);

        // Barkod nömrəsi (altda)
        g.setFont(new Font("Courier New", Font.PLAIN, 14));
        FontMetrics fm = g.getFontMetrics();
        int textX = (width - fm.stringWidth(barcodeText)) / 2;
        g.drawString(barcodeText, textX, barcodeHeight + 42);

        // Satış qiyməti (ən altda, böyük və qalın)
        g.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fmPrice = g.getFontMetrics();
        String priceText = (price != null ? price.setScale(2, java.math.RoundingMode.HALF_UP).toString() + " AZN" : "0.00 AZN");
        int priceX = (width - fmPrice.stringWidth(priceText)) / 2;
        g.drawString(priceText, priceX, labelHeight - 12);

        g.dispose();

        // PNG formatında byte array qaytarırıq
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(label, "PNG", out);
        return out.toByteArray();
    }
}
