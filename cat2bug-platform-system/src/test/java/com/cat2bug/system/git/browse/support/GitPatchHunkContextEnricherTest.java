package com.cat2bug.system.git.browse.support;

import org.eclipse.jgit.diff.RawText;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitPatchHunkContextEnricherTest
{
    @Test
    public void enrich_appendsClassNameForMidFileHunk()
    {
        String newContent = ""
                + "package demo;\n"
                + "\n"
                + "public class DemoService {\n"
                + "  public void a() {}\n"
                + "  public void b() {\n"
                + "    int x = 1;\n"
                + "  }\n"
                + "}\n";
        RawText newText = new RawText(newContent.getBytes(StandardCharsets.UTF_8));
        String patch = ""
                + "diff --git a/DemoService.java b/DemoService.java\n"
                + "--- a/DemoService.java\n"
                + "+++ b/DemoService.java\n"
                + "@@ -5,3 +5,4 @@\n"
                + "   public void b() {\n"
                + "+    int y = 2;\n"
                + "     int x = 1;\n"
                + "   }\n";

        String enriched = GitPatchHunkContextEnricher.enrich(patch, RawText.EMPTY_TEXT, newText);

        // 向上最近的定义行是方法 a（与 Git xfuncname 扫描方向一致）
        assertTrue(enriched.contains("@@ -5,3 +5,4 @@ public void a() {}"));
    }

    @Test
    public void enrich_newFile_usesForwardScanForDefinition()
    {
        String newContent = ""
                + "package demo;\n"
                + "\n"
                + "public class NewThing {\n"
                + "  private int n;\n"
                + "}\n";
        RawText newText = new RawText(newContent.getBytes(StandardCharsets.UTF_8));
        String patch = ""
                + "diff --git a/NewThing.java b/NewThing.java\n"
                + "new file mode 100644\n"
                + "--- /dev/null\n"
                + "+++ b/NewThing.java\n"
                + "@@ -0,0 +1,5 @@\n"
                + "+package demo;\n"
                + "+\n"
                + "+public class NewThing {\n"
                + "+  private int n;\n"
                + "+}\n";

        String enriched = GitPatchHunkContextEnricher.enrich(patch, RawText.EMPTY_TEXT, newText);

        assertTrue(enriched.contains("@@ -0,0 +1,5 @@ public class NewThing {"));
    }

    @Test
    public void enrich_keepsExistingTrailingContext()
    {
        String patch = "@@ -1,2 +1,2 @@ already here\n-a\n+b\n";
        String enriched = GitPatchHunkContextEnricher.enrich(patch, RawText.EMPTY_TEXT, RawText.EMPTY_TEXT);
        assertEquals(patch, enriched);
    }

    @Test
    public void isFuncNameLine_rejectsImportsAndControlFlow()
    {
        assertFalse(GitPatchHunkContextEnricher.isFuncNameLine("import java.util.List;"));
        assertFalse(GitPatchHunkContextEnricher.isFuncNameLine("  if (x) {"));
        assertTrue(GitPatchHunkContextEnricher.isFuncNameLine("public class Foo {"));
        assertTrue(GitPatchHunkContextEnricher.isFuncNameLine("    public void run() {"));
    }
}
