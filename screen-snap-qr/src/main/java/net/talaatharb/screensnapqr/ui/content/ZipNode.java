package net.talaatharb.screensnapqr.ui.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ZipNode {

    private final String label;
    private final boolean directory;
    private final List<ZipNode> children = new ArrayList<>();

    public ZipNode(String label, boolean directory, List<ZipNode> children) {
        this.label = label;
        this.directory = directory;
        this.children.addAll(children);
    }

    public void addChild(ZipNode child) {
        children.add(child);
        children.sort(Comparator.comparing(ZipNode::isDirectory).reversed().thenComparing(ZipNode::getLabel));
    }

}
