package org.hango.cloud.dashboard.envoy.web.controller.api;

import org.apache.commons.collections.CollectionUtils;
import org.hango.cloud.gdashboard.api.dto.ApiParamDto;
import org.hango.cloud.gdashboard.api.dto.CreateApiModelDto;
import org.hango.cloud.gdashboard.api.meta.ApiModel;
import org.hango.cloud.gdashboard.api.meta.ApiParamType;
import org.hango.cloud.gdashboard.api.meta.errorcode.AbstractErrorCode;
import org.hango.cloud.gdashboard.api.meta.errorcode.CommonApiErrorCode;
import org.hango.cloud.gdashboard.api.service.IApiModelService;
import org.hango.cloud.gdashboard.api.service.IApiParamTypeService;
import org.hango.cloud.dashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.dashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.dashboard.envoy.service.IServiceInfoService;
import org.hango.cloud.dashboard.envoy.util.BeanUtil;
import org.hango.cloud.dashboard.envoy.util.CommonUtil;
import org.hango.cloud.dashboard.envoy.util.Const;
import org.hango.cloud.dashboard.envoy.web.controller.AbstractController;
import org.hango.cloud.dashboard.envoy.web.holder.ProjectTraceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import javax.validation.constraints.NotNull;

/**
 * @Date: 创建时间: 2018/1/26 下午2:46.
 */
@RestController
@RequestMapping(value = Const.ENVOY_GATEWAY_PREFIX, params = {"Version=2019-09-01"})
@Validated
public class ApiModelController extends AbstractController {

	private static Logger logger = LoggerFactory.getLogger(ApiModelController.class);

	@Autowired
	private IApiModelService apiModelService;

	@Autowired
	private IServiceInfoService serviceInfoService;

	@Autowired
	private IApiParamTypeService apiParamTypeService;

	/**
	 * 创建新数据模型
	 */
	@RequestMapping(params = {"Action=CreateApiModel"}, method = RequestMethod.POST)
	public Object addModel(@Validated @RequestBody CreateApiModelDto createApiModelDto) {
		logger.info("请求创建模型，创建的数据模型：{}", createApiModelDto);
		if (serviceInfoService.getServiceByServiceId(createApiModelDto.getServiceId()) == null) {
			return apiReturn(CommonErrorCode.NoSuchService);
		}
		//参数检查
		AbstractErrorCode code = apiModelService.checkAddApiModelParam(createApiModelDto);
		if (!CommonErrorCode.Success.getCode().equals(code.getCode())) {
			return apiReturn(code);
		}

		//判断ModelName是否存在
		if (apiModelService.isApiModelExists(createApiModelDto.getModelName(), createApiModelDto.getServiceId())) {
			code = CommonApiErrorCode.ModelNameAlreadyExist;
			return apiReturn(code);
		}

		long modelId = apiModelService.addApiModel(createApiModelDto);

		Map<String, Object> response = new HashMap<>();

		response.put("ApiModelId", modelId);
		return apiReturn(CommonErrorCode.Success, response);
	}

	/**
	 *
	 */
	@RequestMapping(params = {"Action=UpdateApiModel"}, method = RequestMethod.POST)
	public Object modifyModel(@Validated @RequestBody CreateApiModelDto createApiModelDto) {

		//服务id校验
		if (serviceInfoService.getServiceByServiceId(createApiModelDto.getServiceId()) == null) {
			return apiReturn(CommonErrorCode.NoSuchService);
		}
		//参数检查
		AbstractErrorCode code = apiModelService.checkAddApiModelParam(createApiModelDto);
		if (!CommonErrorCode.Success.getCode().equals(code.getCode())) {
			return apiReturn(code);
		}

		//根据Id查询模型基本信息
		ApiModel apiModel = apiModelService.getApiModelInfoByModelId(createApiModelDto.getId());
		//ModelId非法
		if (apiModel == null) {
			return apiReturn(CommonApiErrorCode.NoSuchModel);
		}

		//不能自引用，防止递归栈溢出
		ApiParamType apiParamType = apiParamTypeService.listModleParamType(apiModel.getId());
		if (apiParamType != null) {
			for (ApiParamDto apiParamDto : createApiModelDto.getParams()) {
				if (apiParamDto.getParamTypeId() == apiParamType.getId()
				    || apiParamDto.getArrayDataTypeId() == apiParamType.getId()) {
					code = CommonApiErrorCode.InvalidParamType;
					return apiReturn(code);
				}
			}
		}

		//根据ModelName和serviceId判断是否重复
		ApiModel apiModelTemp = apiModelService.getApiModelByServiceIdAndModelName(createApiModelDto.getServiceId(),
		                                                                           createApiModelDto.getModelName());
		if (apiModelTemp != null) {
			if (apiModelTemp.getId() != apiModel.getId()) {
				code = CommonApiErrorCode.InvalidParameterValueModelName(apiModelTemp.getModelName());
				return apiReturn(code);
			}
		}

		//判断是否修改了模型名，如果修改了，需要修改参数表
		boolean flag = false;
		if (!apiModel.getModelName().equals(createApiModelDto.getModelName())) {
			flag = true;
		}

		try {
			apiModelService.updateApiModel(createApiModelDto, createApiModelDto.getId(), apiModel.getModelName(),
			                               flag);

			return apiReturn(CommonErrorCode.Success);

		} catch (Exception e) {
			logger.error("更新数据模型失败！ModelId {}", createApiModelDto.getId(), e);
			code = CommonErrorCode.InternalServerError;
			return apiReturn(code);
		}

	}

	/**
	 * 根据Id查询Model详情
	 */
	@RequestMapping(params = {"Action=DescribeApiModel"}, method = RequestMethod.GET)
	public String describeApiModel(@RequestParam(value = "ModelId") long modelId) {
		logger.info("请求查询模型，modelId为：{}", modelId);

		//判断ModelId是否存在
		if (!apiModelService.isApiModelExists(modelId)) {
			return apiReturn(CommonApiErrorCode.NoSuchModel);
		}

		CreateApiModelDto createApiModelDto = apiModelService.getApiModelByModelId(modelId);

		//根据serviceId查询serviceName
		createApiModelDto.setDisplayName(
			serviceInfoService.getServiceById(String.valueOf(createApiModelDto.getServiceId())).getDisplayName());

		Map<String, Object> response = new HashMap<>();

		response.put("ApiModelInfo", createApiModelDto);
		return apiReturn(CommonErrorCode.Success, response);
	}

	/**
	 * 查询Model List
	 */
	@RequestMapping(params = {"Action=DescribeModelList"}, method = RequestMethod.GET)
	public String describeApiModels(@RequestParam(value = "Pattern", required = false) String pattern,
	                                @RequestParam(value = "Offset", required = false, defaultValue = "0") long offset,
	                                @RequestParam(value = "Limit", required = false, defaultValue = "20") long limit,
	                                @RequestParam(value = "ServiceId", required = false,
	                                              defaultValue = "0") long serviceId) {

		logger.info("分页获取数据模型列表，offset:{},limit:{}", offset, limit);
		//offset,limit校验
		ErrorCode errorCode = CommonUtil.checkOffsetAndLimit(offset, limit);
		if (!CommonErrorCode.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}

		List<ApiModel> apiModelList = apiModelService.findAllApiModelByProjectLimit(serviceId,
		                                                                            ProjectTraceHolder.getProId(),
		                                                                            offset, limit, pattern);
		List<CreateApiModelDto> createApiModelDtos = new ArrayList<>();
		if (!CollectionUtils.isEmpty(apiModelList)) {
			createApiModelDtos = BeanUtil.copyList(apiModelList, CreateApiModelDto.class);
			createApiModelDtos.forEach(createApiModelDto -> {
				createApiModelDto.setDisplayName(
					serviceInfoService.getServiceByServiceId(createApiModelDto.getServiceId()).getDisplayName());
			});
		}
		Map<String, Object> result = new HashMap<>();
		result.put("TotalCount", apiModelService
			                         .getApiModelCountByProjectOrService(serviceId, ProjectTraceHolder.getProId(),
			                                                             pattern));
		result.put("ModelList", createApiModelDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

	/**
	 * 根据Id删除model
	 */
	@RequestMapping(params = {"Action=DeleteApiModel"}, method = RequestMethod.GET)
	public String deleteApiModelByModelId(@NotNull @RequestParam(value = "ModelId") long modelId) {

		logger.info("请求删除模型，modelId为：{}", modelId);

		ApiModel dbModel = apiModelService.getApiModelInfoByModelId(modelId);
		if (null == dbModel) {
			return apiReturn(CommonApiErrorCode.NoSuchModel);
		}

		//确认model是否被引用
		String message = apiModelService.getApiModelRefer(modelId);
		if (message != null) {
			return apiReturn(400, "ErrorOperation", message, null);
		}

		apiModelService.deleteApiModelByModelId(modelId, true);
		return apiReturn(CommonErrorCode.Success);
	}

}
