package ezcloud.ezMovie.payment.config;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VNPAYConfig {
    public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_Returnurl = "https://ezmovie-iota.vercel.app/booking/";
//    public static String vnp_TmnCode = "0BWKW86F";
//    public static String vnp_HashSecret = "R7QQQIQPPFTIUJ44PB10HS0WNY1ZRU7K";
    public static String vnp_TmnCode = "DSDVRDUB";
    public static String vnp_HashSecret = "ZQZYPSBOO4IB26MAHAWT0LQZ4ZS6CSTR";

    public static String vnp_apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    public static String hashAllFields(Map fields) {
        // Remove vnp_SecureHash if exists
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        
        List fieldNames = new ArrayList(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                try {
                    // URL encode the field value
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());
                    // Replace special characters according to VNPAY requirements
                    encodedValue = encodedValue.replaceAll("\\+", "%20");
                    sb.append(encodedValue);
                } catch (Exception e) {
                    sb.append(fieldValue);
                }
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }
        String hashData = sb.toString();
        System.out.println("Hash data: " + hashData); // Debug log
        String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hashData);
        System.out.println("Secure hash: " + vnp_SecureHash); // Debug log
        return vnp_SecureHash;
    }

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            System.out.println("Error generating HMAC: " + ex.getMessage()); // Debug log
            return "";
        }
    }

    public static String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getLocalAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;
    }

    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}