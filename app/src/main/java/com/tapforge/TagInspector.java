package com.tapforge;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class TagInspector {
    private TagInspector() { }

    public static String uid(Tag tag) { return NdefFactory.bytesToHex(tag == null ? null : tag.getId()); }

    public static String techs(Tag tag) {
        if (tag == null || tag.getTechList() == null) return "";
        StringBuilder b = new StringBuilder();
        for (String t : tag.getTechList()) {
            int i = t.lastIndexOf('.');
            if (b.length() > 0) b.append(" · ");
            b.append(i >= 0 ? t.substring(i + 1) : t);
        }
        return b.toString();
    }

    public static String describeMessage(NdefMessage message) {
        if (message == null) return "—";
        StringBuilder b = new StringBuilder();
        NdefRecord[] records = message.getRecords();
        for (int i = 0; i < records.length; i++) {
            if (i > 0) b.append("\n\n");
            b.append("#").append(i + 1).append("  ").append(describeRecord(records[i]));
        }
        return b.toString();
    }

    public static String describeRecord(NdefRecord r) {
        try {
            short tnf = r.getTnf();
            byte[] type = r.getType();
            byte[] payload = r.getPayload();
            if (tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(type, NdefRecord.RTD_URI)) {
                return "URI\n" + r.toUri();
            }
            if (tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(type, NdefRecord.RTD_TEXT)) {
                if (payload.length == 0) return "Text\n";
                int langLen = payload[0] & 0x3f;
                boolean utf16 = (payload[0] & 0x80) != 0;
                Charset cs = utf16 ? StandardCharsets.UTF_16 : StandardCharsets.UTF_8;
                int start = Math.min(payload.length, 1 + langLen);
                return "Text\n" + new String(payload, start, payload.length - start, cs);
            }
            if (tnf == NdefRecord.TNF_MIME_MEDIA) {
                String mime = new String(type, StandardCharsets.US_ASCII);
                if ("application/vnd.wfa.wsc".equalsIgnoreCase(mime)) return describeWifi(payload);
                return "MIME  " + mime + "\n" + printable(payload);
            }
            if (tnf == NdefRecord.TNF_EXTERNAL_TYPE) {
                return "External  " + new String(type, StandardCharsets.US_ASCII) + "\n" + printable(payload);
            }
            if (tnf == NdefRecord.TNF_EMPTY) return "Empty";
            return "TNF " + tnf + " · " + payload.length + " B\n" + NdefFactory.bytesToHex(payload);
        } catch (Exception e) {
            return "NDEF record";
        }
    }

    private static String describeWifi(byte[] payload) {
        try {
            byte[] body = findWpsAttr(payload, 0x100E); // Credential container
            if (body == null) body = payload;
            byte[] ssid = findWpsAttr(body, 0x1045);
            byte[] key = findWpsAttr(body, 0x1027);
            byte[] auth = findWpsAttr(body, 0x1003);
            StringBuilder b = new StringBuilder("Wi-Fi");
            if (ssid != null) b.append("\nSSID  ").append(new String(ssid, StandardCharsets.UTF_8));
            if (auth != null && auth.length >= 2) {
                int a = ((auth[0]&0xff)<<8)|(auth[1]&0xff);
                String label = a==0x0001?"Open":a==0x0002?"WPA-PSK":a==0x0020?"WPA2-PSK":a==0x0008?"WPA":"0x"+Integer.toHexString(a).toUpperCase(Locale.ROOT);
                b.append("\nSecurity  ").append(label);
            }
            if (key != null && key.length > 0) b.append("\nPassword  ").append(new String(key, StandardCharsets.UTF_8));
            return b.toString();
        } catch (Exception e) { return "Wi-Fi WSC\n" + NdefFactory.bytesToHex(payload); }
    }

    private static byte[] findWpsAttr(byte[] p, int wanted) {
        if (p == null) return null;
        int i=0;
        while (i+4<=p.length) {
            int id=((p[i]&0xff)<<8)|(p[i+1]&0xff); int len=((p[i+2]&0xff)<<8)|(p[i+3]&0xff); i+=4;
            if (len<0 || i+len>p.length) return null;
            if (id==wanted) return Arrays.copyOfRange(p,i,i+len);
            i+=len;
        }
        return null;
    }

    private static String printable(byte[] p) {
        if (p == null || p.length == 0) return "";
        String s = new String(p, StandardCharsets.UTF_8);
        int control = 0;
        for (int i = 0; i < s.length(); i++) if (Character.isISOControl(s.charAt(i)) && !Character.isWhitespace(s.charAt(i))) control++;
        if (control > Math.max(1, s.length() / 8)) return NdefFactory.bytesToHex(p);
        return s;
    }

    public static String tagSummary(Tag tag, Ndef ndef, NdefMessage message) {
        StringBuilder b = new StringBuilder();
        b.append("UID  ").append(uid(tag)).append('\n');
        b.append("Tech  ").append(techs(tag));
        if (ndef != null) {
            b.append("\nNDEF  ").append(ndef.getType());
            b.append("\nCapacity  ").append(ndef.getMaxSize()).append(" B");
            b.append("\nWritable  ").append(ndef.isWritable() ? "yes" : "no");
            if (message != null) {
                int used = message.toByteArray().length;
                b.append("\nMessage  ").append(used).append(" B");
                if (ndef.getMaxSize() > 0) b.append(" · ").append(Math.min(100, Math.round(used * 100f / ndef.getMaxSize()))).append("% used");
                b.append("\nFingerprint  ").append(NfcAnalysis.shortFingerprint(message));
                b.append("\nRecords  ").append(message.getRecords().length);
            }
        } else {
            b.append("\nNDEF  not formatted / unavailable");
        }
        return b.toString();
    }
}
