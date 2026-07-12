package net.talaatharb.screensnapqr.ui.content;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScannedContentViewModel {

    private final ScannedContentType contentType;
    private final List<ContentToken> tokens;
    private final String copyableText;
    private final ZipNode zipNode;
}
