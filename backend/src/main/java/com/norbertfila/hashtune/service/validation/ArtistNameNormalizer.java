package com.norbertfila.hashtune.service.validation;

import java.text.Normalizer;
import java.util.Locale;

public final class ArtistNameNormalizer {
    private ArtistNameNormalizer() {}

    public static String normalize(String artistName) {
        if (artistName == null) {
            return "";
        }
        return Normalizer.normalize(artistName, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
