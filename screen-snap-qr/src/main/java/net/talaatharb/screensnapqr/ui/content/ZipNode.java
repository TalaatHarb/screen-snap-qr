package net.talaatharb.screensnapqr.ui.content;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lombok.Getter;

@Getter
public class ZipNode {

    private final String label;
    private final boolean directory;
    private final List<ZipNode> children = new ArrayList<>();
    private final String path;
    private final byte[] content;

    public ZipNode(String label, boolean directory) {
        this(label, directory, List.of(), label, null);
    }

    public ZipNode(String label, boolean directory, List<ZipNode> children) {
        this(label, directory, children, label, null);
    }

    public ZipNode(String label, boolean directory, List<ZipNode> children, String path, byte[] content) {
        this.label = label;
        this.directory = directory;
        if (children != null) {
            this.children.addAll(children);
        }
        this.path = path;
        this.content = content;
    }

    public String getTextContent() {
        if (content == null) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    public void addChild(ZipNode child) {
        children.add(child);
        children.sort(Comparator.comparing(ZipNode::isDirectory).reversed().thenComparing(ZipNode::getLabel));
    }

    @Override
    public String toString() {
        return label;
    }

}
