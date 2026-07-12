package net.talaatharb.screensnapqr.ui.content;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScannedContentType {
    TEXT("Text"), HYPERLINK("Hyperlink"), JSON("JSON"), XML("XML"), YAML("YAML"), ZIP("ZIP");

    private final String displayName;
}
