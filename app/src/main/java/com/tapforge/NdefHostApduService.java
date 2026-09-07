package com.tapforge;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NdefHostApduService extends HostApduService {
    private static final String TAG = "TapForgeHCE";

    private static final byte[] NDEF_AID_V2 = hex("D2760000850101");
    private static final byte[] NDEF_AID_V1 = hex("D2760000850100");
    private static final byte[] CC_FILE_ID = hex("E103");
    private static final byte[] NDEF_FILE_ID = hex("E104");

    private static final byte[] CC_V2 = hex("000F2000FF00FF0406E1047FFF00FF");
    private static final byte[] CC_V1 = hex("000F1000FF00FF0406E1047FFF00FF");

    private static final byte[] SW_OK = hex("9000");
    private static final byte[] SW_FILE_NOT_FOUND = hex("6A82");
    private static final byte[] SW_WRONG_PARAMS = hex("6B00");
    private static final byte[] SW_INS_NOT_SUPPORTED = hex("6D00");
    private static final byte[] SW_UNKNOWN = hex("6F00");

    private enum SelectedFile { NONE, CC, NDEF }
    private SelectedFile selectedFile = SelectedFile.NONE;
    private boolean legacyV1 = false;
    private byte[] ndefFile = new byte[]{0x00, 0x00};

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        SharedPreferences statePrefs = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        if (!statePrefs.getBoolean(MainActivity.KEY_PROFILE_ENABLED, false)) return hex("6A82");
        if (apdu == null || apdu.length < 4) return SW_UNKNOWN;
        if (BuildConfig.DEBUG) Log.d(TAG, "NDEF RX: " + toHex(apdu));

        try {
            if (isSelectAid(apdu)) {
                byte[] aid = commandData(apdu);
                if (Arrays.equals(aid, NDEF_AID_V2) || Arrays.equals(aid, NDEF_AID_V1)) {
                    legacyV1 = Arrays.equals(aid, NDEF_AID_V1);
                    selectedFile = SelectedFile.NONE;
                    rebuildNdefFile();
                    return respond(SW_OK);
                }
                return respond(SW_FILE_NOT_FOUND);
            }

            if (isSelectFile(apdu)) {
                byte[] fileId = commandData(apdu);
                if (Arrays.equals(fileId, CC_FILE_ID)) {
                    selectedFile = SelectedFile.CC;
                    return respond(SW_OK);
                }
                if (Arrays.equals(fileId, NDEF_FILE_ID)) {
                    selectedFile = SelectedFile.NDEF;
                    rebuildNdefFile();
                    return respond(SW_OK);
                }
                return respond(SW_FILE_NOT_FOUND);
            }

            if ((apdu[1] & 0xFF) == 0xB0) { // READ BINARY
                if (apdu.length < 5) return respond(SW_WRONG_PARAMS);
                int offset = ((apdu[2] & 0xFF) << 8) | (apdu[3] & 0xFF);
                int le = apdu[4] & 0xFF;
                if (le == 0) le = 256;

                byte[] source;
                if (selectedFile == SelectedFile.CC) source = legacyV1 ? CC_V1 : CC_V2;
                else if (selectedFile == SelectedFile.NDEF) source = ndefFile;
                else return respond(SW_FILE_NOT_FOUND);

                if (offset > source.length) return respond(SW_WRONG_PARAMS);
                int count = Math.min(le, source.length - offset);
                return respond(concat(Arrays.copyOfRange(source, offset, offset + count), SW_OK));
            }

            return respond(SW_INS_NOT_SUPPORTED);
        } catch (Exception e) {
            Log.e(TAG, "NDEF APDU error", e);
            return respond(SW_UNKNOWN);
        }
    }

    @Override
    public void onDeactivated(int reason) {
        selectedFile = SelectedFile.NONE;
        legacyV1 = false;
        if (BuildConfig.DEBUG) Log.d(TAG, "NDEF deactivated, reason=" + reason);
    }

    private void rebuildNdefFile() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        String type = prefs.getString(MainActivity.KEY_NDEF_TYPE, MainActivity.TYPE_URL);
        NdefRecord record;

        if (MainActivity.TYPE_RAW_NDEF.equals(type)) {
            try {
                byte[] payload = Base64.decode(prefs.getString(MainActivity.KEY_RAW_NDEF, ""), Base64.DEFAULT);
                new NdefMessage(payload); // validate before serving
                if (payload.length > 0x7FFF) payload = Arrays.copyOf(payload, 0x7FFF);
                ndefFile = new byte[payload.length + 2];
                ndefFile[0] = (byte)((payload.length >> 8) & 0xFF);
                ndefFile[1] = (byte)(payload.length & 0xFF);
                System.arraycopy(payload,0,ndefFile,2,payload.length);
                return;
            } catch (Exception ignored) { record = NdefRecord.createTextRecord("en", "TapForge"); }
        } else if (MainActivity.TYPE_TEXT.equals(type)) {
            String text = prefs.getString(MainActivity.KEY_NDEF_VALUE, "TapForge");
            record = NdefRecord.createTextRecord(LocaleHelper.activeLanguageCode(this), text == null ? "" : text);
        } else if (MainActivity.TYPE_EMAIL.equals(type)) {
            String email = prefs.getString(MainActivity.KEY_NDEF_VALUE, "");
            record = NdefRecord.createUri("mailto:" + (email == null ? "" : email.trim()));
        } else if (MainActivity.TYPE_PHONE.equals(type)) {
            String phone = prefs.getString(MainActivity.KEY_NDEF_VALUE, "");
            record = NdefRecord.createUri("tel:" + (phone == null ? "" : phone.trim()));
        } else if (MainActivity.TYPE_MIME.equals(type)) {
            String mime = safe(prefs.getString(MainActivity.KEY_MIME_TYPE, "text/plain"));
            String value = safe(prefs.getString(MainActivity.KEY_NDEF_VALUE, ""));
            record = NdefRecord.createMime(mime, value.getBytes(StandardCharsets.UTF_8));
        } else if (MainActivity.TYPE_VCARD.equals(type)) {
            String card = VCardBuilder.build(
                    prefs.getString(MainActivity.KEY_VCARD_NAME, ""),
                    prefs.getString(MainActivity.KEY_VCARD_PHONE, ""),
                    prefs.getString(MainActivity.KEY_VCARD_EMAIL, ""),
                    prefs.getString(MainActivity.KEY_VCARD_ORG, ""),
                    prefs.getString(MainActivity.KEY_VCARD_TITLE, ""),
                    prefs.getString(MainActivity.KEY_VCARD_ADDRESS, ""),
                    prefs.getString(MainActivity.KEY_VCARD_WEB, ""));
            record = NdefRecord.createMime("text/vcard", card.getBytes(StandardCharsets.UTF_8));
        } else {
            String url = prefs.getString(MainActivity.KEY_NDEF_VALUE, MainActivity.DEFAULT_URL);
            record = NdefRecord.createUri(url == null ? MainActivity.DEFAULT_URL : url.trim());
        }

        byte[] payload = new NdefMessage(new NdefRecord[]{record}).toByteArray();
        if (payload.length > 0x7FFF) payload = Arrays.copyOf(payload, 0x7FFF);
        ndefFile = new byte[payload.length + 2];
        ndefFile[0] = (byte) ((payload.length >> 8) & 0xFF);
        ndefFile[1] = (byte) (payload.length & 0xFF);
        System.arraycopy(payload, 0, ndefFile, 2, payload.length);
        if (BuildConfig.DEBUG) Log.d(TAG, "Serving NDEF type=" + type + ", bytes=" + payload.length + ", v1=" + legacyV1);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private static boolean isSelectAid(byte[] apdu) {
        return apdu.length >= 5 && (apdu[0] & 0xFF) == 0x00 && (apdu[1] & 0xFF) == 0xA4
                && (apdu[2] & 0xFF) == 0x04;
    }

    private static boolean isSelectFile(byte[] apdu) {
        return apdu.length >= 7 && (apdu[0] & 0xFF) == 0x00 && (apdu[1] & 0xFF) == 0xA4
                && (((apdu[2] & 0xFF) == 0x00) || ((apdu[2] & 0xFF) == 0x02));
    }

    private static byte[] commandData(byte[] apdu) {
        if (apdu.length < 5) return new byte[0];
        int lc = apdu[4] & 0xFF;
        if (apdu.length < 5 + lc) return new byte[0];
        return Arrays.copyOfRange(apdu, 5, 5 + lc);
    }

    private byte[] respond(byte[] response) {
        if (BuildConfig.DEBUG) Log.d(TAG, "NDEF TX: " + toHex(response));
        return response;
    }

    static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    static byte[] hex(String value) {
        String s = value == null ? "" : value.replace(" ", "");
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }
}
