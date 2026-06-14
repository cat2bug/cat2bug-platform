package com.cat2bug.web.vo;

import com.cat2bug.ai.annotaion.AIClass;
import com.cat2bug.ai.annotaion.AIField;
import com.cat2bug.ai.vo.AiResponseBody;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysCaseStep;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-13 23:09
 * @Version: 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@AIClass
public class AiCaseList extends AiResponseBody {
    /** 测试用例列表 */
    @AIField(explain = "测试用例集合")
    private List<AiCase> cases;
}
