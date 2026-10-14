package com.cat2bug.admin.service.im;

import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysReport;
import com.cat2bug.common.domain.SysUser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImMessageTemplatesTest {

    private static final String LINK = "http://h/team/proj/defect?projectId=1&defectId=9";

    @Test
    void defectMarkdownKeepsPreviousSiteNoticeFormat() {
        String expected = "## #12 登录失败 \n\n"
                + "[查看缺陷详情](" + LINK + ") \n\n"
                + "**描述:** 点击无响应 \n\n"
                + "**处理人:** 张三, lisi \n\n";

        assertEquals(expected, ImMessageTemplates.defect(sampleDefect("登录失败"), LINK).markdown());
    }

    @Test
    void defectStateLabelIsLocalized() {
        assertEquals("处理中", ImMessageTemplates.defectStateLabel(0));
        assertEquals("待验证", ImMessageTemplates.defectStateLabel(1));
        assertEquals("已驳回", ImMessageTemplates.defectStateLabel(3));
        assertEquals("已关闭", ImMessageTemplates.defectStateLabel(4));
    }

    @Test
    void allDefectFormatsContainNumberAndName() {
        ImMessageBody body = ImMessageTemplates.defect(sampleDefect("登录失败"), LINK);

        for (String content : List.of(body.text(), body.markdown(), body.html())) {
            assertTrue(content.contains("#12"), content);
            assertTrue(content.contains("登录失败"), content);
        }
        assertTrue(body.text().contains("状态: 处理中"));
        assertTrue(body.text().contains("处理人: 张三, lisi"));
        assertTrue(body.html().contains("href=\"http://h/team/proj/defect?projectId=1&amp;defectId=9\""));
    }

    @Test
    void htmlEscapesUserInput() {
        SysDefect defect = sampleDefect("<script>alert('x')</script>");
        defect.setDefectDescribe("a & b\n<img src=x onerror=1>");

        String html = ImMessageTemplates.defect(defect, LINK).html();

        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<img"));
        assertTrue(html.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"));
        assertTrue(html.contains("a &amp; b<br />&lt;img"));
    }

    @Test
    void reportFormatsContainTitleAndLink() {
        SysReport report = new SysReport();
        report.setReportId(7L);
        report.setProjectId(1L);
        report.setReportTitle("周报 <1>");
        String link = "http://h/team/proj/report?projectId=1&reportId=7";

        ImMessageBody body = ImMessageTemplates.report(report, link);

        assertEquals("## 周报 <1> \n\n点击查看 [" + link + "](" + link + ") \n\n", body.markdown());
        assertTrue(body.text().contains("报告: 周报 <1>"));
        assertTrue(body.html().contains("周报 &lt;1&gt;"));
    }

    @Test
    void noticeKeepsMarkdownAndEscapesHtml() {
        ImMessageBody body = ImMessageTemplates.notice("发布", "**上线**\n<b>今晚</b>");

        assertEquals("**上线**\n<b>今晚</b>", body.markdown());
        assertEquals("发布\n**上线**\n<b>今晚</b>", body.text());
        assertEquals("<h3>发布</h3><p>**上线**<br />&lt;b&gt;今晚&lt;/b&gt;</p>", body.html());
    }

    private static SysDefect sampleDefect(String name) {
        SysDefect defect = new SysDefect();
        defect.setDefectId(9L);
        defect.setProjectId(1L);
        defect.setProjectNum(12L);
        defect.setDefectName(name);
        defect.setDefectState(0);
        defect.setDefectDescribe("点击无响应");
        SysUser zhang = new SysUser();
        zhang.setNickName("张三");
        SysUser li = new SysUser();
        li.setUserName("lisi");
        defect.setHandleByList(List.of(zhang, li));
        return defect;
    }
}
