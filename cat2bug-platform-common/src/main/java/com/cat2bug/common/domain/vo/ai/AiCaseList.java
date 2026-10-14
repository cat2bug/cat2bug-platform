package com.cat2bug.common.domain.vo.ai;

import com.cat2bug.common.ai.AIClass;
import com.cat2bug.common.ai.AIField;

import java.util.List;

@AIClass
public class AiCaseList extends AiResponseBody {

    @AIField(explain = "测试用例集合")
    private List<AiCase> cases;

    public List<AiCase> getCases() {
        return cases;
    }

    public void setCases(List<AiCase> cases) {
        this.cases = cases;
    }
}
