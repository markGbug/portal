package org.hango.cloud.dashboard.envoy.web.controller.api;

import org.hango.cloud.gdashboard.api.dto.ApiHeaderBasicDto;
import org.hango.cloud.gdashboard.api.dto.ApiHeadersDto;
import org.hango.cloud.gdashboard.api.meta.ApiHeader;
import org.hango.cloud.gdashboard.api.meta.ApiInfo;
import org.hango.cloud.gdashboard.api.meta.errorcode.ApiErrorCode;
import org.hango.cloud.gdashboard.api.service.IApiHeaderService;
import org.hango.cloud.gdashboard.api.service.IApiInfoService;
import org.hango.cloud.dashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.dashboard.envoy.util.BeanUtil;
import org.hango.cloud.dashboard.envoy.util.Const;
import org.hango.cloud.dashboard.envoy.web.controller.AbstractController;
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
 * Api header controller,包括 request header 以及 response header
 */
@RestController
@RequestMapping(value = Const.ENVOY_GATEWAY_PREFIX, params = {"Version=2019-09-01"})
public class ApiHeaderController extends AbstractController {

	private static Logger logger = LoggerFactory.getLogger(ApiHeaderController.class);

	@Autowired
	private IApiInfoService apiInfoService;

	@Autowired
	private IApiHeaderService apiHeaderService;

	/**
	 * 添加api header
	 */
	@RequestMapping(params = {"Action=CreateRequestHeader"}, method = RequestMethod.POST)
	public Object addRequestHeader(@Validated @RequestBody ApiHeadersDto apiHeadersDto) {
		logger.info("创建API request:header：api header:{}", apiHeadersDto);
		ApiErrorCode errorCode = apiHeaderService.checkCreateOrUpdateHeader(apiHeadersDto);
		//参数校验
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		List<ApiHeader> headerList = apiHeaderService.generateApiHeaderFromApiHeaderList(apiHeadersDto,
		                                                                                 Const.REQUEST_PARAM_TYPE);
		apiHeaderService.deleteHeader(apiHeadersDto.getId(), Const.REQUEST_PARAM_TYPE);
		for (ApiHeader apiHeader : headerList) {
			apiHeaderService.addHeader(apiHeader);
		}

		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * 添加response header
	 */
	@RequestMapping(params = {"Action=CreateResponseHeader"}, method = RequestMethod.POST)
	public Object addResponseHeader(@Validated @RequestBody ApiHeadersDto apiHeadersDto) {
		logger.info("创建API response header，api response header:{}", apiHeadersDto);
		ApiErrorCode errorCode = apiHeaderService.checkCreateOrUpdateHeader(apiHeadersDto);
		//参数校验
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		List<ApiHeader> headerList = apiHeaderService.generateApiHeaderFromApiHeaderList(apiHeadersDto,
		                                                                                 Const.RESPONSE_PARAM_TYPE);
		apiHeaderService.deleteHeader(apiHeadersDto.getId(), Const.RESPONSE_PARAM_TYPE);

		headerList.forEach(apiHeader -> apiHeaderService.addHeader(apiHeader));

		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * 查询request header
	 */
	@RequestMapping(params = {"Action=DescribeRequestHeader"}, method = RequestMethod.GET)
	public Object getRequestHeader(@RequestParam(value = "ApiId") long apiId) {
		logger.info("查询apiId:{}下的requestHeader", apiId);
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		List<ApiHeader> apiHeaders = apiHeaderService.getHeader(apiId, Const.REQUEST_PARAM_TYPE);
		List<ApiHeaderBasicDto> apiHeaderBasicDtos = new ArrayList<>();
		if (!CollectionUtils.isEmpty(apiHeaders)) {
			apiHeaderBasicDtos = BeanUtil.copyList(apiHeaders, ApiHeaderBasicDto.class);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("Headers", apiHeaderBasicDtos);
		return apiReturn(CommonErrorCode.Success, result);

	}

	/**
	 * 查询request header
	 */
	@RequestMapping(params = {"Action=DescribeResponseHeader"}, method = RequestMethod.GET)
	public Object getResponseHeader(@RequestParam(value = "ApiId") long apiId) {
		logger.info("查询apiId:{}下的responseHeader", apiId);
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		List<ApiHeader> apiHeaders = apiHeaderService.getHeader(apiId, Const.RESPONSE_PARAM_TYPE);
		List<ApiHeaderBasicDto> apiHeaderBasicDtos = new ArrayList<>();
		if (!CollectionUtils.isEmpty(apiHeaders)) {
			apiHeaderBasicDtos = BeanUtil.copyList(apiHeaders, ApiHeaderBasicDto.class);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("Headers", apiHeaderBasicDtos);
		return apiReturn(CommonErrorCode.Success, result);

	}

	/**
	 * 删除header中的某个param
	 */
	@RequestMapping(params = {"Action=DeleteHeaderByParamId"}, method = RequestMethod.GET)
	public Object deleteHeaderParam(@RequestParam(value = "ParamId") Long paramId) {
		apiHeaderService.deleteHeaderParam(paramId);
		return apiReturn(CommonErrorCode.Success);
	}

}
