package com.affiliate.rentals.gydi.shared.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Utility class for generating SEO-friendly URL slugs from property titles.
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Converts to lowercase</li>
 *   <li>Removes accents/diacritics</li>
 *   <li>Replaces spaces with hyphens</li>
 *   <li>Removes special characters</li>
 *   <li>Handles multiple consecutive hyphens</li>
 *   <li>Trims leading/trailing hyphens</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * String title = "Beautiful Beach House in Málaga!";
 * String slug = slugGenerator.generateSlug(title);
 * // Result: "beautiful-beach-house-in-malaga"
 * }</pre>
 *
 * @author GYDI Development Team
 */
@Component
public class SlugGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_HYPHENS = Pattern.compile("(^-|-$)");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-{2,}");

    /**
     * Generates a URL-safe slug from the given text.
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Convert to lowercase</li>
     *   <li>Remove accents (é → e, ñ → n)</li>
     *   <li>Replace spaces with hyphens</li>
     *   <li>Remove special characters</li>
     *   <li>Remove multiple consecutive hyphens</li>
     *   <li>Trim edge hyphens</li>
     * </ol>
     *
     * @param text the text to convert to slug
     * @return URL-safe slug
     * @throws IllegalArgumentException if text is null or empty
     */
    public String generateSlug(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        // Convert to lowercase
        String slug = text.toLowerCase(Locale.ENGLISH);

        // Normalize to remove accents (NFD = decompose, then remove marks)
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = slug.replaceAll("\\p{M}", ""); // Remove diacritical marks

        // Replace spaces with hyphens
        slug = WHITESPACE.matcher(slug).replaceAll("-");

        // Remove all non-alphanumeric characters except hyphens
        slug = NON_LATIN.matcher(slug).replaceAll("");

        // Replace multiple consecutive hyphens with single hyphen
        slug = MULTIPLE_HYPHENS.matcher(slug).replaceAll("-");

        // Remove hyphens from edges
        slug = EDGES_HYPHENS.matcher(slug).replaceAll("");

        return slug;
    }

    /**
     * Generates a slug with a maximum length.
     *
     * <p>Truncates at word boundaries to avoid breaking words.</p>
     *
     * @param text the text to convert to slug
     * @param maxLength maximum length of the slug
     * @return URL-safe slug with max length
     * @throws IllegalArgumentException if text is null/empty or maxLength < 10
     */
    public String generateSlug(String text, int maxLength) {
        if (maxLength < 10) {
            throw new IllegalArgumentException("Max length must be at least 10 characters");
        }

        String slug = generateSlug(text);

        if (slug.length() <= maxLength) {
            return slug;
        }

        // Truncate at word boundary (last hyphen before maxLength)
        slug = slug.substring(0, maxLength);
        int lastHyphen = slug.lastIndexOf('-');

        if (lastHyphen > 0) {
            slug = slug.substring(0, lastHyphen);
        }

        return slug;
    }

    /**
     * Generates a unique slug by appending a short identifier.
     *
     * <p>Format: {slug}-{shortId}</p>
     * <p>Example: beach-house-malibu-x7k2m</p>
     *
     * @param text the text to convert to slug
     * @param shortId unique short identifier (e.g., from Hashids)
     * @return unique slug with identifier
     */
    public String generateUniqueSlug(String text, String shortId) {
        if (shortId == null || shortId.trim().isEmpty()) {
            throw new IllegalArgumentException("Short ID cannot be null or empty");
        }

        String slug = generateSlug(text, 50); // Max 50 chars for text part
        return slug + "-" + shortId.toLowerCase();
    }

    /**
     * Extracts the short ID from a unique slug.
     *
     * <p>Example: "beach-house-malibu-x7k2m" → "x7k2m"</p>
     *
     * @param uniqueSlug the unique slug containing short ID
     * @return the extracted short ID
     * @throws IllegalArgumentException if slug format is invalid
     */
    public String extractShortId(String uniqueSlug) {
        if (uniqueSlug == null || uniqueSlug.trim().isEmpty()) {
            throw new IllegalArgumentException("Unique slug cannot be null or empty");
        }

        int lastHyphen = uniqueSlug.lastIndexOf('-');
        if (lastHyphen < 0 || lastHyphen == uniqueSlug.length() - 1) {
            throw new IllegalArgumentException("Invalid slug format: no short ID found");
        }

        return uniqueSlug.substring(lastHyphen + 1);
    }
}