package net.talaatharb.screensnapqr.ui.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class ZipNodeTest {

    @Test
    void testConstructorsAndGetters() {
        ZipNode node1 = new ZipNode("test.txt", false);
        assertEquals("test.txt", node1.getLabel());
        assertFalse(node1.isDirectory());
        assertEquals("test.txt", node1.getPath());
        assertNull(node1.getContent());
        assertEquals("", node1.getTextContent());
        assertEquals("test.txt", node1.toString());

        ZipNode child = new ZipNode("child.txt", false);
        ZipNode node2 = new ZipNode("dir", true, List.of(child));
        assertEquals("dir", node2.getLabel());
        assertTrue(node2.isDirectory());
        assertEquals(1, node2.getChildren().size());

        byte[] bytes = "Hello World".getBytes(StandardCharsets.UTF_8);
        ZipNode node3 = new ZipNode("file.json", false, null, "path/to/file.json", bytes);
        assertEquals("file.json", node3.getLabel());
        assertEquals("path/to/file.json", node3.getPath());
        assertEquals(bytes, node3.getContent());
        assertEquals("Hello World", node3.getTextContent());
    }

    @Test
    void testAddChildSortOrder() {
        ZipNode parent = new ZipNode("root", true);
        ZipNode fileB = new ZipNode("b.txt", false);
        ZipNode fileA = new ZipNode("a.txt", false);
        ZipNode dirZ = new ZipNode("z_dir", true);
        ZipNode dirA = new ZipNode("a_dir", true);

        parent.addChild(fileB);
        parent.addChild(fileA);
        parent.addChild(dirZ);
        parent.addChild(dirA);

        List<ZipNode> children = parent.getChildren();
        assertEquals(4, children.size());
        assertEquals("a_dir", children.get(0).getLabel());
        assertTrue(children.get(0).isDirectory());
        assertEquals("z_dir", children.get(1).getLabel());
        assertTrue(children.get(1).isDirectory());
        assertEquals("a.txt", children.get(2).getLabel());
        assertFalse(children.get(2).isDirectory());
        assertEquals("b.txt", children.get(3).getLabel());
        assertFalse(children.get(3).isDirectory());
    }
}
