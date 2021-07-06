package org.hango.cloud.ncegdashboard.envoy.web.controller.api;

import org.apache.commons.lang3.math.NumberUtils;
import org.hango.cloud.gdashboard.api.dto.ApiInfoBasicDto;
import org.hango.cloud.gdashboard.api.dto.ApiListDto;
import org.hango.cloud.gdashboard.api.meta.ApiInfo;
import org.hango.cloud.gdashboard.api.meta.errorcode.ApiErrorCode;
import org.hango.cloud.gdashboard.api.meta.errorcode.CommonApiErrorCode;
import org.hango.cloud.gdashboard.api.service.IApiInfoService;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.service.IServiceInfoService;
import org.hango.cloud.ncegdashboard.envoy.util.BeanUtil;
import org.hango.cloud.ncegdashboard.envoy.util.CommonUtil;
import org.hango.cloud.ncegdashboard.envoy.util.Const;
import org.hango.cloud.ncegdashboard.envoy.web.controller.AbstractController;
import org.hango.cloud.ncegdashboard.envoy.web.holder.ProjectTraceHolder;
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
 * api基本信息管理，包括API名称，标识等基本信息
 */
@RestController
@RequestMapping(value = Const.ENVOY_GATEWAY_PREFIX, params = {"Version=2019-09-01"})
@Validated
public class ApiBasicInfoController extends AbstractController {

	private static Logger logger = LoggerFactory.getLogger(ApiBasicInfoController.class);

	@Autowired
	private IApiInfoService apiInfoService;

	@Autowired
	private IServiceInfoService serviceInfoService;

	/**
	 * 创建新的API
	 */
	@RequestMapping(params = {"Action=CreateApi"}, method = RequestMethod.POST)
	public Object addApi(@Validated @RequestBody ApiInfoBasicDto apiInfoBasicDto) {
		logger.info("创建API，apiInfoBasicDto:{}", apiInfoBasicDto);
		//服务id校验
		if (serviceInfoService.getServiceByServiceId(apiInfoBasicDto.getServiceId()) == null) {
			return apiReturn(CommonErrorCode.NoSuchService);
		}
		ApiErrorCode errorCode = apiInfoService.checkParamApiBasicDto(apiInfoBasicDto);
		//参数校验
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		long apiId = apiInfoService.addApiInfos(apiInfoBasicDto, apiInfoBasicDto.getType());
		return apiReturnSuccess(apiId);

	}

	/**
	 * 根据Id查询api基本信息
	 */
	@RequestMapping(params = {"Action=DescribeApiById"}, method = RequestMethod.GET)
	public Object getApiInfo(@RequestParam(value = "ApiId") long apiId) {
		logger.info("根据apiId:{},查询api基本信息", apiId);
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo == null) {
			logger.info("根据apiId:{},查询api基本信息，接口不存在", apiId);
			return apiReturn(CommonErrorCode.NoSuchApiInterface);
		}
		ApiInfoBasicDto apiInfoBasicDto = BeanUtil.copy(apiInfo, ApiInfoBasicDto.class);
		Map<String, Object> result = new HashMap<>();
		result.put("ApiInfoBasic", apiInfoBasicDto);
		return apiReturn(CommonErrorCode.Success, result);
	}

	/**
	 * 更新API信息
	 *
	 * @param apiInfoBasicDto api基本信息dto
	 *
	 * @return 更新结果
	 */
	@RequestMapping(params = {"Action=UpdateApi"}, method = RequestMethod.POST)
	public Object updateApi(@Validated @RequestBody ApiInfoBasicDto apiInfoBasicDto) {
		logger.info("更新API基本信息，apiInfoBasicBto:{}", apiInfoBasicDto);

		ApiInfo apiInfo = apiInfoService.getApiById(apiInfoBasicDto.getId());
		if (apiInfo == null) {
			logger.info("更新apiId:{},查询api基本信息不存在", apiInfoBasicDto.getId());
			return apiReturn(CommonApiErrorCode.NoSuchApiInterface);
		}

		//服务id校验
		if (serviceInfoService.getServiceByServiceId(apiInfoBasicDto.getServiceId()) == null) {
			return apiReturn(CommonErrorCode.NoSuchService);
		}

		ApiErrorCode errorCode = apiInfoService.checkParamApiBasicDto(apiInfoBasicDto);
		//参数校验
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}

		//看着整齐，没有进行service抽取
		apiInfo.setModifyDate(System.currentTimeMillis());
		apiInfo.setApiName(apiInfoBasicDto.getApiName());
		apiInfo.setApiPath(apiInfoBasicDto.getApiPath());
		apiInfo.setApiMethod(apiInfoBasicDto.getApiMethod());
		apiInfo.setAliasName(apiInfoBasicDto.getAliasName());
		apiInfo.setDescription(apiInfoBasicDto.getDescription());
		apiInfo.setDocumentStatusId(apiInfoBasicDto.getDocumentStatusId());
		String regex = apiInfo.getApiPath().replaceAll("\\{[^}]*\\}", "*");
		apiInfo.setRegex(regex);

		apiInfoService.updateApi(apiInfo);
		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * 根据apiId删除API基本信息
	 *
	 * @param apiId 接口APIid
	 *
	 * @return 删除结果
	 */
	@RequestMapping(params = {"Action=DeleteApiById"}, method = RequestMethod.GET)
	public Object deleteApi(@RequestParam(value = "ApiId") long apiId) {
		logger.info("请求删除apiId:{}的接口信息", apiId);
		ApiInfo apiInfo = apiInfoService.getApiById(apiId);
		if (apiInfo != null) {
			if (NumberUtils.INTEGER_ONE.equals(NumberUtils.toInt(apiInfo.getStatus()))) {
				logger.info("接口未下线，不能进行删除");
				return apiReturn(CommonErrorCode.CannotDeleteOnlineApi);
			}
		}
		apiInfoService.deleteApi(apiId);
		return apiReturn(CommonErrorCode.Success);
	}

	/**
	 * 分页获取API
	 *
	 * @param pattern       模糊匹配pattern
	 * @param offset        分页offset
	 * @param limit         分页limit
	 * @param serviceId     服务id，支持全部
	 * @param apiDocumentId API状态，默认为全部状态
	 *
	 * @return APIList
	 */
	@RequestMapping(params = {"Action=DescribeApiListByLimit"}, method = RequestMethod.GET)
	public Object apiList(@RequestParam(value = "Pattern", required = false) String pattern,
	                      @RequestParam(value = "Offset", required = false, defaultValue = "0") long offset,
	                      @RequestParam(value = "Limit", required = false, defaultValue = "20") long limit,
	                      @RequestParam(value = "ServiceId", required = false, defaultValue = "0") long serviceId,
	                      @RequestParam(value = "ApiDocumentStatus", required = false,
	                                    defaultValue = "0") long apiDocumentId) {
		//offset,limit校验
		ErrorCode errorCode = CommonUtil.checkOffsetAndLimit(offset, limit);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		//没有传服务id，获取项目下的所有服务
		List<ApiInfo> apiInfos = apiInfoService.findAllApiByProjectLimit(ProjectTraceHolder.getProId(), serviceId,
		                                                                 apiDocumentId, pattern, offset, limit);
		List<ApiListDto> apiListDtos = new ArrayList<>();
		if (!CollectionUtils.isEmpty(apiInfos)) {
			apiInfos.forEach(apiInfo -> {
				ApiInfoBasicDto apiInfoBasicDto = BeanUtil.copy(apiInfo, ApiInfoBasicDto.class);
				ApiListDto apiListDto = BeanUtil.copy(apiInfo, ApiListDto.class);
				apiListDto.setApiInfoBasicDto(apiInfoBasicDto);
				apiListDto.setStatus(apiInfo.getStatus());
				apiListDtos.add(apiListDto);
			});
		}
		Map<String, Object> result = new HashMap<>();
		result.put("TotalCount", apiInfoService.getCountByProjectOrService(ProjectTraceHolder.getProId(), serviceId,
		                                                                   apiDocumentId, pattern));
		result.put("ApiList", apiListDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

}
