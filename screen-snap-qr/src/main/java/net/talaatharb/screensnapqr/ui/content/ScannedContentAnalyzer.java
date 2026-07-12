package net.talaatharb.screensnapqr.ui.content;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.talaatharb.screensnapqr.dtos.QRCodeResultDto;

public class ScannedContentAnalyzer {

    private static final Pattern HYPERLINK_PATTERN = Pattern.compile("^https?://\\S+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_TOKEN_PATTERN = Pattern.compile(
            "\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|\\btrue\\b|\\bfalse\\b|\\bnull\\b|[\\[\\]{}:,]");
    private static final Pattern XML_TOKEN_PATTERN = Pattern.compile("<!--.*?-->|<[^>]+>", Pattern.DOTALL);
    private static final Pattern YAML_LINE_PATTERN = Pattern.compile("^(\\s*(?:-\\s+)?)?([\\w.\\-\"']+)\\s*:(.*)$");
    private static final int ZIP_MAGIC_FIRST = 0x50;
    private static final int ZIP_MAGIC_SECOND = 0x4B;
    private static final int ZIP_LOCAL_HEADER_THIRD = 0x03;
    private static final int ZIP_LOCAL_HEADER_FOURTH = 0x04;
    private static final int ZIP_EMPTY_ARCHIVE_THIRD = 0x05;
    private static final int ZIP_EMPTY_ARCHIVE_FOURTH = 0x06;
    private static final int ZIP_SPANNED_THIRD = 0x07;
    private static final int ZIP_SPANNED_FOURTH = 0x08;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ScannedContentViewModel analyze(QRCodeResultDto result) {
        final String sourceText = Objects.requireNonNullElse(result.getText(), "");
        final String trimmed = sourceText.trim();
        final byte[] rawBytes = result.getRawBytes();

        if (isZipBytes(rawBytes)) {
            return zipContent(rawBytes);
        }

        final byte[] textSignatureZip = decodeZipFromSignature(sourceText);
        if (textSignatureZip != null) {
            return zipContent(textSignatureZip);
        }

        final byte[] base64Zip = decodeBase64Zip(trimmed);
        if (base64Zip != null) {
            return zipContent(base64Zip);
        }

        final String prettyJson = parseJson(trimmed);
        if (prettyJson != null) {
            return ScannedContentViewModel.builder().contentType(ScannedContentType.JSON)
                    .tokens(tokenizeJson(prettyJson)).copyableText(prettyJson).build();
        }

        final String prettyXml = parseXml(trimmed);
        if (prettyXml != null) {
            return ScannedContentViewModel.builder().contentType(ScannedContentType.XML)
                    .tokens(tokenizeXml(prettyXml)).copyableText(prettyXml).build();
        }

        if (HYPERLINK_PATTERN.matcher(trimmed).matches()) {
            return ScannedContentViewModel.builder().contentType(ScannedContentType.HYPERLINK)
                    .tokens(List.of(new ContentToken(trimmed, "token-link"))).copyableText(trimmed).build();
        }

        if (isLikelyYaml(trimmed)) {
            return ScannedContentViewModel.builder().contentType(ScannedContentType.YAML)
                    .tokens(tokenizeYaml(trimmed)).copyableText(trimmed).build();
        }

        return ScannedContentViewModel.builder().contentType(ScannedContentType.TEXT)
                .tokens(List.of(new ContentToken(sourceText, "token-text"))).copyableText(sourceText).build();
    }

    private static ScannedContentViewModel zipContent(byte[] zipBytes) {
        final ZipNode zipRoot = buildZipTree(zipBytes);
        return ScannedContentViewModel.builder().contentType(ScannedContentType.ZIP)
                .tokens(List.of(new ContentToken("ZIP archive payload detected.", "token-zip")))
                .copyableText("ZIP archive payload").zipNode(zipRoot).build();
    }

    private static byte[] decodeBase64Zip(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            final byte[] decoded = Base64.getDecoder().decode(value);
            return isZipBytes(decoded) ? decoded : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static byte[] decodeZipFromSignature(String sourceText) {
        if (sourceText == null || sourceText.length() < 2 || !sourceText.startsWith("PK")) {
            return null;
        }

        final byte[] latin1Bytes = sourceText.getBytes(StandardCharsets.ISO_8859_1);
        if (isZipBytes(latin1Bytes)) {
            return latin1Bytes;
        }

        final byte[] utf8Bytes = sourceText.getBytes(StandardCharsets.UTF_8);
        if (isZipBytes(utf8Bytes)) {
            return utf8Bytes;
        }

        return latin1Bytes;
    }

    private static boolean isZipBytes(byte[] value) {
        if (value == null || value.length < 4) {
            return false;
        }

        if (Byte.toUnsignedInt(value[0]) != ZIP_MAGIC_FIRST || Byte.toUnsignedInt(value[1]) != ZIP_MAGIC_SECOND) {
            return false;
        }

        final int third = Byte.toUnsignedInt(value[2]);
        final int fourth = Byte.toUnsignedInt(value[3]);
        return (third == ZIP_LOCAL_HEADER_THIRD && fourth == ZIP_LOCAL_HEADER_FOURTH)
                || (third == ZIP_EMPTY_ARCHIVE_THIRD && fourth == ZIP_EMPTY_ARCHIVE_FOURTH)
                || (third == ZIP_SPANNED_THIRD && fourth == ZIP_SPANNED_FOURTH);
    }

    private static String parseJson(String value) {
        if (value == null || value.isBlank() || !(value.startsWith("{") || value.startsWith("["))) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(OBJECT_MAPPER.readTree(value));
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private static String parseXml(String value) {
        if (value == null || value.isBlank() || !value.startsWith("<")) {
            return null;
        }

        try {
            final var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            final var documentBuilder = factory.newDocumentBuilder();
            final var document = documentBuilder.parse(new InputSource(new StringReader(value)));

            final var transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            final var writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException ex) {
            return null;
        }
    }

    private static boolean isLikelyYaml(String value) {
        if (value == null || value.isBlank() || value.startsWith("<") || value.startsWith("{") || value.startsWith("[")) {
            return false;
        }

        final String[] lines = value.split("\\R");
        int yamlLikeLines = 0;
        for (String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (YAML_LINE_PATTERN.matcher(line).matches()) {
                yamlLikeLines++;
            }
        }
        return yamlLikeLines > 0;
    }

    private static List<ContentToken> tokenizeJson(String content) {
        return tokenizeWithPattern(content, JSON_TOKEN_PATTERN, token -> {
            if (token.startsWith("\"")) {
                return "token-string";
            }
            if (token.equals("true") || token.equals("false")) {
                return "token-boolean";
            }
            if (token.equals("null")) {
                return "token-null";
            }
            if ("[]{}:,".contains(token)) {
                return "token-punctuation";
            }
            return "token-number";
        });
    }

    private static List<ContentToken> tokenizeXml(String content) {
        return tokenizeWithPattern(content, XML_TOKEN_PATTERN, token -> token.startsWith("<!--") ? "token-comment" : "token-tag");
    }

    private static List<ContentToken> tokenizeYaml(String content) {
        final List<ContentToken> tokens = new ArrayList<>();
        final String[] lines = content.split("\\R", -1);

        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            final String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                tokens.add(new ContentToken(line, "token-comment"));
            } else {
                final Matcher matcher = YAML_LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    final String indent = Objects.requireNonNullElse(matcher.group(1), "");
                    final String key = matcher.group(2);
                    final String value = Objects.requireNonNullElse(matcher.group(3), "");
                    tokens.add(new ContentToken(indent, "token-text"));
                    tokens.add(new ContentToken(key, "token-key"));
                    tokens.add(new ContentToken(":", "token-punctuation"));
                    tokens.add(new ContentToken(value, "token-text"));
                } else {
                    tokens.add(new ContentToken(line, "token-text"));
                }
            }
            if (i < lines.length - 1) {
                tokens.add(new ContentToken(System.lineSeparator(), "token-text"));
            }
        }
        return tokens;
    }

    private static List<ContentToken> tokenizeWithPattern(String content, Pattern pattern,
            java.util.function.Function<String, String> styleResolver) {
        final List<ContentToken> tokens = new ArrayList<>();
        final Matcher matcher = pattern.matcher(content);
        int currentIndex = 0;

        while (matcher.find()) {
            if (matcher.start() > currentIndex) {
                tokens.add(new ContentToken(content.substring(currentIndex, matcher.start()), "token-text"));
            }
            final String token = matcher.group();
            tokens.add(new ContentToken(token, styleResolver.apply(token)));
            currentIndex = matcher.end();
        }

        if (currentIndex < content.length()) {
            tokens.add(new ContentToken(content.substring(currentIndex), "token-text"));
        }

        return tokens;
    }

    private static ZipNode buildZipTree(byte[] zipBytes) {
        final InternalZipNode root = new InternalZipNode("archive.zip", true);

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                final String entryName = entry.getName().replace('\\', '/');
                if (!entryName.isBlank()) {
                    root.insert(entryName, entry.isDirectory());
                }
            }
        } catch (IOException ex) {
            return new ZipNode("archive.zip (unreadable)", true);
        }

        return root.toZipNode();
    }

    private static final class InternalZipNode {
        private final String name;
        private final boolean directory;
        private final TreeMap<String, InternalZipNode> children = new TreeMap<>();

        InternalZipNode(String name, boolean directory) {
            this.name = name;
            this.directory = directory;
        }

        void insert(String entryName, boolean isDirectory) {
            final String[] parts = entryName.split("/");
            InternalZipNode current = this;
            for (int i = 0; i < parts.length; i++) {
                final String part = parts[i];
                if (part.isBlank()) {
                    continue;
                }
                final boolean nodeDirectory = i < parts.length - 1 || isDirectory;
                current.children.putIfAbsent(part, new InternalZipNode(part, nodeDirectory));
                current = current.children.get(part);
            }
        }

        ZipNode toZipNode() {
            final List<ZipNode> mappedChildren = children.values().stream().map(InternalZipNode::toZipNode).toList();
            return new ZipNode(name, directory, mappedChildren);
        }
    }
}
