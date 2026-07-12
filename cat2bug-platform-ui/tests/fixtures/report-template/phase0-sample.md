# 测试报告

## 1. 项目信息
项目名称: ${api.project.projectName}
$image["style":"width:200px;height:200px;"]{api.project.projectIcon}

## 2. 测试概述
用例总数: ${api.case.total}

## 3. 附录：缺陷清单
$table{api.defect.list[projectNum,defectTypeName,defectLevel,defectName,defectStateName,moduleName,moduleVersion,defectDescribe,createBy,createTime,updateBy,updateTime,imgList,annexList]}

## 4. 计划指标
缺陷总数: ${api.plan.defectCount[1]}
