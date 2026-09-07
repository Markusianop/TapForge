package com.tapforge;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

public class CustomHostApduService extends HostApduService {
    private static final String TAG = "TapForgeHCE";
    private static final byte[] SW_AID_NOT_FOUND = NdefHostApduService.hex("6A82");
    private static final byte[] SW_INS_NOT_SUPPORTED = NdefHostApduService.hex("6D00");
    private static final byte[] SW_UNKNOWN = NdefHostApduService.hex("6F00");

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        SharedPreferences statePrefs = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
        if (!statePrefs.getBoolean(MainActivity.KEY_PROFILE_ENABLED, false)) return NdefHostApduService.hex("6A82");
        if (apdu == null || apdu.length < 4) return SW_UNKNOWN;
        if (BuildConfig.DEBUG) Log.d(TAG, "CUSTOM RX: " + NdefHostApduService.toHex(apdu));

        try {
            SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
            String aidHex = prefs.getString(MainActivity.KEY_CUSTOM_AID, MainActivity.DEFAULT_CUSTOM_AID);
            String responseHex = prefs.getString(MainActivity.KEY_CUSTOM_RESPONSE, MainActivity.DEFAULT_CUSTOM_RESPONSE);
            String statusHex = prefs.getString(MainActivity.KEY_CUSTOM_STATUS, MainActivity.DEFAULT_CUSTOM_STATUS);

            if (isSelectAid(apdu)) {
                byte[] requestedAid = commandData(apdu);
                byte[] configuredAid = NdefHostApduService.hex(MainActivity.normalizeHex(aidHex));
                if (!Arrays.equals(requestedAid, configuredAid)) return respond(SW_AID_NOT_FOUND);

                byte[] data = NdefHostApduService.hex(MainActivity.normalizeHex(responseHex));
                byte[] status = NdefHostApduService.hex(MainActivity.normalizeHex(statusHex));
                return respond(NdefHostApduService.concat(data, status));
            }

            return respond(SW_INS_NOT_SUPPORTED);
        } catch (Exception e) {
            Log.e(TAG, "Custom APDU error", e);
            return respond(SW_UNKNOWN);
        }
    }

    @Override
    public void onDeactivated(int reason) {
        if (BuildConfig.DEBUG) Log.d(TAG, "CUSTOM deactivated, reason=" + reason);
    }

    private static boolean isSelectAid(byte[] apdu) {
        return apdu.length >= 5 && (apdu[0] & 0xFF) == 0x00 && (apdu[1] & 0xFF) == 0xA4
                && (apdu[2] & 0xFF) == 0x04;
    }

    private static byte[] commandData(byte[] apdu) {
        if (apdu.length < 5) return new byte[0];
        int lc = apdu[4] & 0xFF;
        if (apdu.length < 5 + lc) return new byte[0];
        return Arrays.copyOfRange(apdu, 5, 5 + lc);
    }

    private byte[] respond(byte[] response) {
        if (BuildConfig.DEBUG) Log.d(TAG, "CUSTOM TX: " + NdefHostApduService.toHex(response));
        return response;
    }
}
