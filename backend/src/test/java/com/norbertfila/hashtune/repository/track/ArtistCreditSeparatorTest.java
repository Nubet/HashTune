package com.norbertfila.hashtune.repository.track;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ArtistCreditSeparatorTest {
    private static final Pattern COLLABORATOR_SEPARATOR = Pattern.compile("\\s*(?:&|,)\\s*");

    static Stream<Arguments> collaboratingCredits() {
        return Stream.of(
                Arguments.of("Bedoes 2115 & Kubi Producent", List.of("Bedoes 2115", "Kubi Producent")),
                Arguments.of("Chivas & White 2115", List.of("Chivas", "White 2115")),
                Arguments.of("Adi Nowak & Sarnula", List.of("Adi Nowak", "Sarnula")),
                Arguments.of("Diho & Josef Bratan", List.of("Diho", "Josef Bratan")),
                Arguments.of(
                        "Bonus RPK, DJ Gondek, Paluch, Nizioł", List.of("Bonus RPK", "DJ Gondek", "Paluch", "Nizioł")),
                Arguments.of("Bedoes 2115, Lanek", List.of("Bedoes 2115", "Lanek")),
                Arguments.of(
                        "Bonus RPK, Sokół, Hinol Polska Wersja, Ciemna Strefa",
                        List.of("Bonus RPK", "Sokół", "Hinol Polska Wersja", "Ciemna Strefa")));
    }

    @ParameterizedTest
    @MethodSource("collaboratingCredits")
    void separatesCollaboratingArtistsWithoutChangingTheirNames(String credit, List<String> expectedArtists) {
        assertThat(List.of(COLLABORATOR_SEPARATOR.split(credit))).containsExactlyElementsOf(expectedArtists);
    }
}
