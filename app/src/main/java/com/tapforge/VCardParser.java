package com.tapforge;

import java.util.Locale;

public final class VCardParser {
    private VCardParser() {}
    public static final class Contact { public String name="", phone="", email="", org="", title="", address="", web=""; }
    public static Contact parse(String card) {
        Contact c = new Contact();
        String normalized = card == null ? "" : card.replace("\r\n ", "").replace("\n ", "");
        for (String line : normalized.split("\\r?\\n")) {
            int colon=line.indexOf(':'); if(colon<0) continue;
            String key=line.substring(0,colon).toUpperCase(Locale.ROOT); String val=unescape(line.substring(colon+1));
            if(key.startsWith("FN") && c.name.isEmpty()) c.name=val;
            else if(key.startsWith("TEL") && c.phone.isEmpty()) c.phone=val;
            else if(key.startsWith("EMAIL") && c.email.isEmpty()) c.email=val;
            else if(key.startsWith("ORG") && c.org.isEmpty()) c.org=val.replace(";", " · ");
            else if(key.startsWith("TITLE") && c.title.isEmpty()) c.title=val;
            else if(key.startsWith("ADR") && c.address.isEmpty()) c.address=val.replace(";", " ").trim();
            else if(key.startsWith("URL") && c.web.isEmpty()) c.web=val;
        }
        return c;
    }
    public static String summary(String card) {
        Contact c=parse(card); StringBuilder b=new StringBuilder();
        append(b,c.name); append(b,c.phone); append(b,c.email); append(b,c.org); append(b,c.title); append(b,c.address); append(b,c.web);
        return b.length()==0 ? card : b.toString();
    }
    private static void append(StringBuilder b,String s){if(s==null||s.isEmpty())return;if(b.length()>0)b.append('\n');b.append(s);}
    private static String unescape(String s){return s.replace("\\n","\n").replace("\\N","\n").replace("\\;",";").replace("\\,",",").replace("\\\\","\\");}
}
