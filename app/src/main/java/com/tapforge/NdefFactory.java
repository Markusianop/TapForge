package com.tapforge;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import java.net.URLEncoder;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Small NDEF builder shared by physical-tag writing and HCE-facing tools. */
public final class NdefFactory {
    public static final String URL = "url";
    public static final String TEXT = "text";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String SMS = "sms";
    public static final String LOCATION = "location";
    public static final String CONTACT = "contact";
    public static final String WIFI = "wifi";
    public static final String APP = "app";
    public static final String MIME = "mime";
    public static final String EXTERNAL = "external";
    public static final String RAW = "raw";

    private NdefFactory() { }

    public static NdefMessage build(String type, String value, String extra,
                                    String name, String phone, String email,
                                    String org, String title, String address, String web) throws Exception {
        NdefRecord record;
        if (URL.equals(type)) {
            record = NdefRecord.createUri(normalizeUrl(value));
        } else if (TEXT.equals(type)) {
            record = NdefRecord.createTextRecord(Locale.getDefault().getLanguage(), value == null ? "" : value);
        } else if (EMAIL.equals(type)) {
            record = NdefRecord.createUri("mailto:" + safe(value));
        } else if (PHONE.equals(type)) {
            record = NdefRecord.createUri("tel:" + safe(value));
        } else if (SMS.equals(type)) {
            String number = safe(value).trim();
            String body = safe(extra);
            String uri = "sms:" + number;
            if (!body.isEmpty()) uri += "?body=" + URLEncoder.encode(body, "UTF-8");
            record = NdefRecord.createUri(uri);
        } else if (LOCATION.equals(type)) {
            String coordinates = safe(value).trim().replace(" ", "");
            if (!coordinates.contains(",")) throw new IllegalArgumentException("location");
            record = NdefRecord.createUri("geo:" + coordinates);
        } else if (CONTACT.equals(type)) {
            String vcard = VCardBuilder.build(name, phone, email, org, title, address, web);
            record = NdefRecord.createMime("text/vcard", vcard.getBytes(StandardCharsets.UTF_8));
        } else if (WIFI.equals(type)) {
            record = NdefRecord.createMime("application/vnd.wfa.wsc", buildWifiWsc(safe(value), safe(extra)));
        } else if (APP.equals(type)) {
            String pkg = safe(value).trim();
            if (pkg.isEmpty() || !pkg.contains(".")) throw new IllegalArgumentException("package");
            record = NdefRecord.createApplicationRecord(pkg);
        } else if (MIME.equals(type)) {
            String mime = safe(extra).trim();
            if (mime.isEmpty() || !mime.contains("/")) throw new IllegalArgumentException("mime");
            record = NdefRecord.createMime(mime, safe(value).getBytes(StandardCharsets.UTF_8));
        } else if (EXTERNAL.equals(type)) {
            String spec = safe(extra).trim().toLowerCase(Locale.ROOT);
            int colon = spec.indexOf(':');
            if (colon <= 0 || colon == spec.length() - 1) throw new IllegalArgumentException("external");
            record = NdefRecord.createExternal(spec.substring(0, colon), spec.substring(colon + 1),
                    safe(value).getBytes(StandardCharsets.UTF_8));
        } else if (RAW.equals(type)) {
            byte[] raw = hexToBytes(value);
            return new NdefMessage(raw);
        } else {
            throw new IllegalArgumentException("type");
        }
        return new NdefMessage(new NdefRecord[]{record});
    }

    public static NdefMessage emptyMessage() {
        NdefRecord empty = new NdefRecord(NdefRecord.TNF_EMPTY, new byte[0], new byte[0], new byte[0]);
        return new NdefMessage(new NdefRecord[]{empty});
    }

    private static byte[] buildWifiWsc(String ssid, String password) throws Exception {
        ssid = safe(ssid).trim();
        if (ssid.isEmpty()) throw new IllegalArgumentException("ssid");
        password = safe(password);
        boolean open = password.isEmpty();
        ByteArrayOutputStream credential = new ByteArrayOutputStream();
        writeAttr(credential, 0x1026, new byte[]{0x01}); // Network Index
        writeAttr(credential, 0x1045, ssid.getBytes(StandardCharsets.UTF_8));
        writeAttr(credential, 0x1003, new byte[]{0x00, (byte)(open ? 0x01 : 0x20)}); // OPEN / WPA2-PSK
        writeAttr(credential, 0x100F, new byte[]{0x00, (byte)(open ? 0x01 : 0x08)}); // NONE / AES
        if (!open) writeAttr(credential, 0x1027, password.getBytes(StandardCharsets.UTF_8));
        writeAttr(credential, 0x1020, new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeAttr(out, 0x100E, credential.toByteArray()); // Credential
        return out.toByteArray();
    }

    private static void writeAttr(ByteArrayOutputStream out, int id, byte[] data) throws Exception {
        out.write((id >> 8) & 0xff); out.write(id & 0xff);
        out.write((data.length >> 8) & 0xff); out.write(data.length & 0xff);
        out.write(data);
    }

    private static String normalizeUrl(String value) {
        String v = safe(value).trim();
        if (v.isEmpty()) return "https://example.com";
        if (!v.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) v = "https://" + v;
        return v;
    }

    public static byte[] hexToBytes(String value) {
        String s = safe(value).replaceAll("[^0-9A-Fa-f]", "");
        if (s.length() == 0 || (s.length() & 1) != 0) throw new IllegalArgumentException("hex");
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return out;
    }

    public static String bytesToHex(byte[] data) {
        if (data == null) return "";
        StringBuilder b = new StringBuilder(data.length * 2);
        for (byte x : data) b.append(String.format(Locale.ROOT, "%02X", x & 0xff));
        return b.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
