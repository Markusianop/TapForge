package com.tapforge;

import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;

import java.security.MessageDigest;
import java.util.Arrays;

public final class NfcAnalysis {
    private NfcAnalysis() { }

    public static String sha256(byte[] data) {
        try { MessageDigest md=MessageDigest.getInstance("SHA-256"); byte[] d=md.digest(data==null?new byte[0]:data); StringBuilder b=new StringBuilder(); for(byte x:d)b.append(String.format("%02X",x&0xff)); return b.toString(); }
        catch(Exception e){ return ""; }
    }

    public static String shortFingerprint(NdefMessage m) {
        if(m==null) return "—"; String s=sha256(m.toByteArray()); return s.length()>16?s.substring(0,8)+"…"+s.substring(s.length()-8):s;
    }

    public static boolean sameMessage(NdefMessage a, NdefMessage b) {
        return a!=null && b!=null && Arrays.equals(a.toByteArray(),b.toByteArray());
    }

    public static String compatibility(Tag tag, NdefMessage intended) {
        Ndef n=Ndef.get(tag); NdefFormatable f=NdefFormatable.get(tag); int bytes=intended==null?0:intended.toByteArray().length;
        StringBuilder b=new StringBuilder();
        b.append("UID  ").append(TagInspector.uid(tag)).append('\n');
        b.append("Tech  ").append(TagInspector.techs(tag)).append('\n');
        if(n!=null) {
            b.append("NDEF  ready\n"); b.append("Writable  ").append(n.isWritable()?"yes":"no").append('\n'); b.append("Capacity  ").append(n.getMaxSize()).append(" B\n");
            if(intended!=null) { int left=n.getMaxSize()-bytes; b.append("Profile  ").append(bytes).append(" B · ").append(left>=0?left+" B free":"too large by "+(-left)+" B").append('\n'); }
            b.append("Result  ").append(n.isWritable() && (intended==null || bytes<=n.getMaxSize())?"ready to write":"not writable as requested");
        } else if(f!=null) {
            b.append("NDEF  formatable\nWritable  yes after format\n"); if(intended!=null)b.append("Profile  ").append(bytes).append(" B\n"); b.append("Result  can be initialized by TapForge");
        } else {
            b.append("NDEF  unavailable\nResult  Android does not expose standard NDEF writing for this tag");
        }
        return b.toString();
    }
}
