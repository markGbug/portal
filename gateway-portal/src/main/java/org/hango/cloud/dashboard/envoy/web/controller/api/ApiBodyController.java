package org.hango.cloud.dashboard.envoy.web.controller.api;

import org.hango.cloud.gdashboard.api.dto.ApiBodyBasicDto;
import org.hango.cloud.gdashboard.api.dto.ApiBodyJsonDto;
import org.hango.cloud.gdashboard.api.dto.ApiBodysDto;
import org.hango.cloud.gdashboard.api.dto.ApiStatusCodeBasicDto;
import org.hango.cloud.gdashboard.api.dto.ApiStatusCodesDto;
import org.hango.cloud.gdashboard.api.meta.ApiBody;
import org.hango.cloud.gdashboard.api.meta.ApiInfo;
import org.hango.cloud.gdashboard.api.meta.ApiParamType;
import org.hango.cloud.gdashboard.api.meta.ApiStatusCode;
import org.hango.cloud.gdashboard.api.meta.AssociationType;
import org.hango.cloud.gdashboard.api.meta.errorcode.ApiErrorCode;
import org.hango.cloud.gdashboard.api.service.IApiBodyService;
import org.hango.cloud.gdashboard.api.service.IApiConvertToJsonService;
import org.hango.cloud.gdashboard.api.service.IApiInfoService;
import org.hango.cloud.gdashboard.api.service.IApiParamTypeService;
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
 * API body controller，包括request body,response body 以及queryString
 */
@RestController
@RequestMapping(value = Const.ENVOY_GATEWAY_PREFIX, params = {"Version=2019-09-01"})
@Validated
public class ApiBodyController extends AbstractController {

	private static Logger logger = LoggerFactory.getLogger(ApiBodyController.class);

	@Autowired
	private IApiInfoService apiInfoService;

	@Autowired
	private IApiParamTypeService apiParamTypeService;

	@Autowired
	private IApiBodyService apiBodyService;

	@Autowired
	private IApiConvertToJsonService apiConvertToJsonService;

	/**
	 * 添加request body
	 *
	 * @param apiBodysDto apiBody包装dto
	 *
	 * @return 创建request body结果
	 */
	@RequestMapping(params = {"Action=CreateRequestBody"}, method = RequestMethod.POST)
	public Object addRequestBody(@Validated @RequestBody ApiBodysDto apiBodysDto) {
		logger.info("创建request body，apiBody：{}", apiBodysDto);
		ApiErrorCode errorCode = apiBodyService.checkApiBodyBasicInfo(apiBodysDto);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		List<ApiBody> bodyList = apiBodyService.generateApiBodyFromApiBodyList(apiBodysDto, Const.REQUEST_PARAM_TYPE);
		apiBodyService.deleteBody(apiBodysDto.getId(), Const.REQUEST_PARAM_TYPE);
		bodyList.forEach(apiBody -> {
			apiBody.setAssociationType(AssociationType.NORMAL.name());
			apiBodyService.addBody(apiBody);
		});
		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * create query String
	 *
	 * @param apiBodysDto apiBody包装dto
	 *
	 * @return 创建queryString结果
	 */
	@RequestMapping(params = {"Action=CreateQueryString"}, method = RequestMethod.POST)
	public Object addQueryString(@Validated @RequestBody ApiBodysDto apiBodysDto) {
		logger.info("创建request body，apiBody：{}", apiBodysDto);
		ApiErrorCode errorCode = apiBodyService.checkApiBodyBasicInfo(apiBodysDto);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		List<ApiBody> bodyList = apiBodyService.generateApiBodyFromApiBodyList(apiBodysDto,
		                                                                       Const.QUERYSTRING_PARAM_TYPE);
		apiBodyService.deleteBody(apiBodysDto.getId(), Const.QUERYSTRING_PARAM_TYPE);
		bodyList.forEach(apiBody -> apiBodyService.addBody(apiBody));

		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * 查询queryString
	 *
	 * @param apiId 接口ID
	 *
	 * @return ApiBodyBasicDtos，queryString的基本值
	 */
	@RequestMapping(params = {"Action=DescribeQueryString"}, method = RequestMethod.GET)
	public Object getQueryString(@RequestParam(value = "ApiId") long apiId) {
		logger.info("查询apiId:{}下的queryString", apiId);
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		List<ApiBody> apiBodies = apiBodyService.getBody(apiId, Const.QUERYSTRING_PARAM_TYPE);
		List<ApiBodyBasicDto> apiBodyBasicDtos = new ArrayList<>();
		if (!CollectionUtils.isEmpty(apiBodies)) {
			apiBodyBasicDtos = BeanUtil.copyList(apiBodies, ApiBodyBasicDto.class);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("QueryString", apiBodyBasicDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

	/**
	 * 创建statusCode
	 *
	 * @param apiStatusCodesDto statusCode的包装dto
	 *
	 * @return 创建结果
	 */
	@RequestMapping(params = {"Action=CreateStatusCode"}, method = RequestMethod.POST)
	public Object addStatusCode(@Validated @RequestBody ApiStatusCodesDto apiStatusCodesDto) {
		ApiInfo apiInfo = apiInfoService.getApiById(apiStatusCodesDto.getId());
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		List<ApiStatusCode> apiStatusCodes = apiBodyService.generateApiStatusCodeFromCodeList(apiStatusCodesDto);

		apiBodyService.addStatusCodes(apiStatusCodes, apiStatusCodesDto.getId(), Const.API);

		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * 根据APIId查询statusCode
	 *
	 * @param apiId 接口id
	 *
	 * @return statusCode
	 */
	@RequestMapping(params = {"Action=DescribeStatusCode"}, method = RequestMethod.GET)
	public Object getStatusCode(@RequestParam(value = "ApiId") long apiId) {
		logger.info("查询apiId:{}下的statusCode", apiId);
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		List<ApiStatusCode> apiStatusCodes = apiBodyService.listStatusCode(apiId, Const.API);
		List<ApiStatusCodeBasicDto> apiStatusCodeBasicDtos = new ArrayList<>();
		if (!CollectionUtils.isEmpty(apiStatusCodes)) {
			apiStatusCodeBasicDtos = BeanUtil.copyList(apiStatusCodes, ApiStatusCodeBasicDto.class);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("ResponseStatusCode", apiStatusCodeBasicDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

	/**
	 * 生成responsebody
	 *
	 * @param apiBodysDto apiBody的包装dto
	 *
	 * @return 创建结果
	 */
	@RequestMapping(params = {"Action=CreateResponseBody"}, method = RequestMethod.POST)
	public Object addResponseBody(@Validated @RequestBody ApiBodysDto apiBodysDto) {
		logger.info("创建request body，apiBody：{}", apiBodysDto);
		ApiErrorCode errorCode = apiBodyService.checkApiBodyBasicInfo(apiBodysDto);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		List<ApiBody> bodyList = apiBodyService.generateApiBodyFromApiBodyList(apiBodysDto, Const.RESPONSE_PARAM_TYPE);
		apiBodyService.deleteBody(apiBodysDto.getId(), Const.RESPONSE_PARAM_TYPE);
		bodyList.forEach(apiBody -> {
			apiBody.setAssociationType(org.hango.cloud.gdashboard.api.meta.AssociationType.NORMAL.name());
			apiBodyService.addBody(apiBody);
		});

		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * 根据apiID查询responseBody
	 *
	 * @param apiId 接口APIId
	 *
	 * @return ResponseBody
	 */
	@RequestMapping(params = {"Action=DescribeResponseBody"}, method = RequestMethod.GET)
	public Object getResponseBody(@RequestParam(value = "ApiId") long apiId) {
		logger.info("查询apiId:{}下的response body", apiId);
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		List<ApiBody> apiBodies = apiBodyService.getBody(apiId, Const.RESPONSE_PARAM_TYPE);
		List<ApiBodyBasicDto> apiBodyBasicDtos = new ArrayList<>();
		if (!CollectionUtils.isEmpty(apiBodies)) {
			apiBodyBasicDtos = BeanUtil.copyList(apiBodies, ApiBodyBasicDto.class);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("ResponseBody", apiBodyBasicDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

	/**
	 * 查询request body
	 *
	 * @param apiId APIId
	 *
	 * @return RequestBody
	 */
	@RequestMapping(params = {"Action=DescribeRequestBody"}, method = RequestMethod.GET)
	public Object getRequestBody(@RequestParam(value = "ApiId") long apiId) {
		logger.info("查询apiId:{}下的request body", apiId);
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		List<ApiBody> apiBodies = apiBodyService.getBody(apiId, Const.REQUEST_PARAM_TYPE);
		List<ApiBodyBasicDto> apiBodyBasicDtos = new ArrayList<>();
		if (!CollectionUtils.isEmpty(apiBodies)) {
			apiBodyBasicDtos = BeanUtil.copyList(apiBodies, ApiBodyBasicDto.class);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("RequestBody", apiBodyBasicDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

	/**
	 * 通过json导入body
	 *
	 * @param apiBodyJsonDto json导入的包装dto
	 */
	@RequestMapping(params = {"Action=GenerateBodyByJson"}, method = RequestMethod.POST)
	public Object addBodyByJson(@Validated @RequestBody ApiBodyJsonDto apiBodyJsonDto) {
		ApiInfo apiInfo = apiInfoService.getApiById(apiBodyJsonDto.getId());
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		//type值校验
		if (!Const.REQUEST_PARAM_TYPE.equals(apiBodyJsonDto.getType()) && !Const.RESPONSE_PARAM_TYPE.equals(
			apiBodyJsonDto.getType())) {
			return apiReturn(CommonErrorCode.InvalidParameter(apiBodyJsonDto.getType(), "Type"));
		}

		List<ApiBody> bodyList = apiConvertToJsonService.generateApiBodyByJson(apiBodyJsonDto.getId(),
		                                                                       apiInfo.getServiceId(),
		                                                                       apiBodyJsonDto.getParams(),
		                                                                       apiBodyJsonDto.getType());
		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * 查询json信息
	 *
	 * @param type  查询类型，Request,Response
	 * @param apiId 接口id
	 */
	@RequestMapping(params = {"Action=DescribeBodyParamJson"}, method = RequestMethod.GET)
	public Object getBodyJson(@RequestParam(value = "Type") String type, @RequestParam(value = "ApiId") long apiId) {
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo == null) {
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		//type值校验
		if (!Const.REQUEST_PARAM_TYPE.equals(type) && !Const.RESPONSE_PARAM_TYPE.equals(type)) {
			return apiReturn(CommonErrorCode.InvalidParameter(type, "Type"));
		}
		Map<String, Object> paramMap = apiConvertToJsonService.generateJsonForApi(apiId, type);
		Map<String, Object> result = new HashMap<>();
		result.put("Result", paramMap);
		return apiReturn(CommonErrorCode.Success, result);
	}

	/**
	 * 删除body中的某个param
	 */
	@RequestMapping(params = {"Action=DeleteBodyParamId"}, method = RequestMethod.GET)
	public Object deleteBodyParamId(@RequestParam(value = "ParamId") Long paramId) {
		apiBodyService.deleteBodyParam(paramId);
		return apiReturn(CommonErrorCode.Success);
	}

	private StringBuilder getOperationLog(List<ApiBody> bodyList) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("{");

		ApiParamType apiParamType;

		int count = 1;
		String required;
		for (ApiBody apiBody : bodyList) {
			apiParamType = apiParamTypeService.listApiParamType(apiBody.getParamTypeId());
			stringBuilder.append(
				count + ". 名称：" + apiBody.getParamName() + ", 类型：" + apiParamType.getParamType() + ", ");
			if ("Array".equals(apiParamType.getParamType())) {
				//Array数据类型必须填写ArrayDataTypeId
				apiParamType = apiParamTypeService.listApiParamType(apiBody.getArrayDataTypeId());
				stringBuilder.append("其中Array中的数据类型为：" + apiParamType.getParamType() + ", ");
			}

			if ("0".equals(apiBody.getRequired())) {
				required = "否";
			} else {
				required = "是";
			}
			stringBuilder.append(
				"默认取值：" + apiBody.getDefValue() + ", 是否必填：" + required + ", 描述：" + apiBody.getDescription() + ". ");
			count++;
		}
		stringBuilder.append("}");
		return stringBuilder;
	}

	private StringBuilder getStatusCodeOperationLog(List<ApiStatusCode> apiStatusCodeList) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("{");
		int count = 1;
		for (ApiStatusCode statusCode : apiStatusCodeList) {
			stringBuilder.append(count + ". 返回码：" + statusCode.getStatusCode() + ", 描述：" + statusCode.getDescription());
			count++;
		}
		stringBuilder.append("}");
		return stringBuilder;
	}

}
