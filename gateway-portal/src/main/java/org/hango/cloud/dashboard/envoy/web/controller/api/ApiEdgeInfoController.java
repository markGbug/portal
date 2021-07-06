package org.hango.cloud.ncegdashboard.envoy.web.controller.api;

import org.apache.commons.lang3.StringUtils;
import org.hango.cloud.gdashboard.api.dto.ApiExampleDto;
import org.hango.cloud.gdashboard.api.dto.OperationDto;
import org.hango.cloud.gdashboard.api.meta.ApiInfo;
import org.hango.cloud.gdashboard.api.meta.OperationLog;
import org.hango.cloud.gdashboard.api.service.IApiInfoService;
import org.hango.cloud.gdashboard.api.service.IOperationLogService;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.util.CommonUtil;
import org.hango.cloud.ncegdashboard.envoy.util.Const;
import org.hango.cloud.ncegdashboard.envoy.web.controller.AbstractController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API 边缘信息，包括示例、修改记录等
 */
@RestController
@RequestMapping(value = Const.ENVOY_GATEWAY_PREFIX, params = {"Version=2019-09-01"})
@Validated
public class ApiEdgeInfoController extends AbstractController {

	private static Logger logger = LoggerFactory.getLogger(ApiEdgeInfoController.class);

	@Autowired
	private IApiInfoService apiInfoService;

	@Autowired
	private IOperationLogService operationLogService;

	@RequestMapping(params = {"Action=DescribeExample"}, method = RequestMethod.GET)
	public Object getExample(@RequestParam(value = "ApiId") long apiId) {
		logger.info("查询apiId:{}下的example", apiId);
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		String requestExampleValue = apiInfo.getRequestExampleValue();
		String responseExampleValue = apiInfo.getResponseExampleValue();
		Map<String, Object> result = new HashMap<>();
		result.put("RequestExample", requestExampleValue);
		result.put("ResponseExample", responseExampleValue);
		return apiReturn(CommonErrorCode.Success, result);
	}

	@RequestMapping(params = {"Action=CreateApiExample"}, method = RequestMethod.POST)
	public Object addExample(@Validated @RequestBody ApiExampleDto apiExampleDto) {

		ApiInfo apiInfo = apiInfoService.getApiById(apiExampleDto.getId());
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		//避免null，如果接口传null，转换为空字符串
		if (StringUtils.isBlank(apiExampleDto.getRequestExample())) {
			apiExampleDto.setRequestExample("");
		}
		if (StringUtils.isBlank(apiExampleDto.getResponseExample())) {
			apiExampleDto.setResponseExample("");
		}
		apiInfo.setRequestExampleValue(apiExampleDto.getRequestExample());
		apiInfo.setResponseExampleValue(apiExampleDto.getResponseExample());
		apiInfoService.updateApi(apiInfo);
		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * 分页查询修改记录
	 */
	@RequestMapping(params = {"Action=DescribeOperationList"}, method = RequestMethod.GET)
	public Object getOperationlist(@RequestParam(value = "Offset", required = false, defaultValue = "0") long offset,
	                               @RequestParam(value = "Limit", required = false, defaultValue = "20") long limit,
	                               @RequestParam(value = "ApiId") long apiId) {
		logger.info("查询apiId:{}的修改记录", apiId);
		//offset,limit校验
		ErrorCode errorCode = CommonUtil.checkOffsetAndLimit(offset, limit);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		List<OperationLog> operationLogList = operationLogService.listApiOperationLog(apiId, Const.API, limit, offset);
		List<OperationDto> operationDtos = new ArrayList<>();
		if (!CollectionUtils.isEmpty(operationLogList)) {
			for (OperationLog operationLog : operationLogList) {
				OperationDto operationDto = new OperationDto();
				//数据库存储字段为email
				operationDto.setAccountId(operationLog.getEmail());
				operationDto.setCreateDate(operationLog.getCreateDate());
				operationDto.setOperation(operationLog.getOperation());
				operationDtos.add(operationDto);
			}
		}
		long totalCount = operationLogService.getCount(apiId, Const.API);
		Map<String, Object> result = new HashMap<>();
		result.put("TotalCount", totalCount);
		result.put("Operations", operationDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

}
