package com.cat2bug.system.mapper;

import com.cat2bug.common.domain.SysCaseReview;
import com.cat2bug.common.domain.SysCaseReviewItem;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysCaseReviewMapper {

    int insertReview(SysCaseReview review);

    int insertItem(SysCaseReviewItem item);

    SysCaseReview selectReview(@Param("reviewId") Long reviewId);

    List<SysCaseReview> selectByProject(@Param("projectId") Long projectId);

    List<SysCaseReviewItem> selectItems(@Param("reviewId") Long reviewId);

    SysCaseReviewItem selectItem(@Param("reviewId") Long reviewId, @Param("caseId") Long caseId);

    int updateItem(SysCaseReviewItem item);

    int completeReview(SysCaseReview review);

    int updateReport(@Param("reviewId") Long reviewId, @Param("reportMarkdown") String reportMarkdown);

    Long selectOpenReviewIdByCase(@Param("caseId") Long caseId);

    List<Long> selectOpenCaseIds(@Param("caseIds") List<Long> caseIds);

    int updateCaseReviewStatus(@Param("caseId") Long caseId, @Param("reviewStatus") String reviewStatus);

    int deleteOpenItemsByCase(@Param("caseId") Long caseId);

    int countItems(@Param("reviewId") Long reviewId);

    int deleteReview(@Param("reviewId") Long reviewId);

    List<Long> selectOpenReviewIdsByCase(@Param("caseId") Long caseId);
}
