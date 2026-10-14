package com.cat2bug.system.open;

import com.cat2bug.common.domain.open.ApiCase;
import com.cat2bug.common.domain.open.ApiDeliverable;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mapper.OpenApiCaseMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class OpenApiCaseService {

    @Inject
    OpenApiCaseMapper openApiCaseMapper;

    @Inject
    OpenApiDeliverableService openApiDeliverableService;

    public List<ApiCase> selectApiCaseList(Long projectId, ApiCase apiCase) {
        ApiCase filter = apiCase != null ? apiCase : new ApiCase();
        List<ApiDeliverable> deliverableList = openApiDeliverableService.selectApiDeliverablePathList(projectId);

        if (StringUtils.isNotBlank(filter.getDeliverableName())) {
            Optional<ApiDeliverable> matched = ApiDeliverablePathMatch.find(
                    deliverableList, filter.getDeliverableName());
            if (matched.isPresent()) {
                filter.setDeliverableId(matched.get().getDeliverableId());
            } else {
                filter.setDeliverableId(-1L);
            }
        }

        Map<Long, ApiDeliverable> pathMap = deliverableList.stream()
                .collect(Collectors.toMap(ApiDeliverable::getDeliverableId, d -> d));

        return openApiCaseMapper.selectApiCaseList(projectId, filter).stream()
                .map(c -> enrichDeliverableName(c, pathMap))
                .collect(Collectors.toList());
    }

    public ApiCase selectApiCaseByCaseNum(Long projectId, Long caseNum) {
        ApiCase c = openApiCaseMapper.selectApiCaseByCaseNum(projectId, caseNum);
        if (c == null) {
            return null;
        }
        Map<Long, ApiDeliverable> pathMap = openApiDeliverableService.selectApiDeliverablePathList(projectId).stream()
                .collect(Collectors.toMap(ApiDeliverable::getDeliverableId, d -> d));
        return enrichDeliverableName(c, pathMap);
    }

    private ApiCase enrichDeliverableName(ApiCase c, Map<Long, ApiDeliverable> pathMap) {
        if (c.getDeliverableId() != null && pathMap.containsKey(c.getDeliverableId())) {
            c.setDeliverableName(pathMap.get(c.getDeliverableId()).getDeliverablePath());
        }
        return c;
    }
}
