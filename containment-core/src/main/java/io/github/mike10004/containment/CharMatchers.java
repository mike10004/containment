package io.github.mike10004.containment;

class CharMatchers {

    private CharMatchers(){}

    private static final CharMatcher DOT = CharMatcher.is('.');
    private static final CharMatcher US_ENGLISH_ALPHABET = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'));
    private static final CharMatcher US_ENGLISH_DIGIT = CharMatcher.inRange('0', '9');
    private static final CharMatcher US_ENGLISH_ALPHANUMERIC = US_ENGLISH_ALPHABET.or(US_ENGLISH_DIGIT);
    private static final CharMatcher UNDERSCORE = CharMatcher.is('_');
    private static final CharMatcher REGEX_WORD = US_ENGLISH_ALPHANUMERIC.or(UNDERSCORE);

    public static CharMatcher dot() {
        return DOT;
    }

    public static CharMatcher usEnglishAlphanumericOrUnderscore() {
        return REGEX_WORD;
    }
}
