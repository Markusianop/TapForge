package com.tapforge;

public final class VCardBuilder {
    private VCardBuilder() { }

    public static String build(String name, String phone, String email, String organization,
                               String title, String address, String website) {
        name = clean(name);
        phone = clean(phone);
        email = clean(email);
        organization = clean(organization);
        title = clean(title);
        address = clean(address);
        website = clean(website);

        String display = !name.isEmpty() ? name : (!organization.isEmpty() ? organization :
                (!phone.isEmpty() ? phone : (!email.isEmpty() ? email : "Contact")));

        StringBuilder card = new StringBuilder();
        append(card, "BEGIN:VCARD");
        append(card, "VERSION:3.0");
        append(card, "FN:" + escape(display));
        append(card, structuredName(name));
        if (!phone.isEmpty()) append(card, "TEL;TYPE=CELL:" + escape(phone));
        if (!email.isEmpty()) append(card, "EMAIL;TYPE=INTERNET:" + escape(email));
        if (!organization.isEmpty()) append(card, "ORG:" + escape(organization));
        if (!title.isEmpty()) append(card, "TITLE:" + escape(title));
        if (!address.isEmpty()) append(card, "ADR;TYPE=HOME:;;" + escape(address) + ";;;;");
        if (!website.isEmpty()) append(card, "URL:" + escape(website));
        append(card, "END:VCARD");
        return card.toString();
    }

    private static String structuredName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "N:;;;;";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return "N:" + escape(parts[0]) + ";;;;";
        String family = parts[parts.length - 1];
        StringBuilder given = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) given.append(' ');
            given.append(parts[i]);
        }
        return "N:" + escape(family) + ";" + escape(given.toString()) + ";;;";
    }

    private static void append(StringBuilder out, String line) {
        // vCard 3.0 folding is defined in octets, not Java chars. Keep UTF-8
        // code points intact while folding long values for strict parsers.
        int bytesOnLine = 0;
        for (int i = 0; i < line.length();) {
            int cp = line.codePointAt(i);
            String chunk = new String(Character.toChars(cp));
            int chunkBytes = chunk.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            int limit = bytesOnLine == 0 ? 75 : 75;
            if (bytesOnLine > 0 && bytesOnLine + chunkBytes > limit) {
                out.append("\r\n ");
                bytesOnLine = 1; // continuation space counts toward the folded line
            }
            out.append(chunk);
            bytesOnLine += chunkBytes;
            i += Character.charCount(cp);
        }
        out.append("\r\n");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String escape(String value) {
        return clean(value)
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}
