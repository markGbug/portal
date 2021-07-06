package org.hango.cloud.ncegdashboard.envoy.web.controller;

import com.google.common.collect.Lists;
import com.alibaba.fastjson.JSON;
import org.apache.commons.httpclient.HttpStatus;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyVirtualHostInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.GatewayInfo;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.CommonErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;
import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCodeEnum;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyGatewayService;
import org.hango.cloud.ncegdashboard.envoy.service.IEnvoyIstioGatewayService;
import org.hango.cloud.ncegdashboard.envoy.service.IGatewayInfoService;
import org.hango.cloud.ncegdashboard.envoy.util.Const;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyGatewaySettingDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyGatewayVirtualHostDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyPluginManagerDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyVirtualHostDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.EnvoyVirtualHostUpdateDto;
import org.hango.cloud.ncegdashboard.envoy.web.dto.GatewayDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Envoy网关特有操作Controller层
 * <p>
 * 2020-01-09
 */
@RestController
@Validated
@RequestMapping(value = Const.ENVOY_GATEWAY_PREFIX, params = {"Version=2019-09-01"})
public class EnvoyGatewayController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyGatewayController.class);

	@Autowired
	private IGatewayInfoService gatewayInfoService;

	@Autowired
	private IEnvoyGatewayService envoyGatewayService;

	@Autowired
	private IEnvoyIstioGatewayService envoyIstioGatewayService;

	@PostMapping(params = {"Action=CreateGateway"})
	public Object addGwInfo(@Validated @RequestBody GatewayDto gatewayDto) {
		//创建网关参数校验
		ErrorCode errorCode = gatewayInfoService.checkAddParam(gatewayDto);
		if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}
		if (Const.ENVOY_GATEWAY_TYPE.equals(gatewayDto.getGwType())) {
			boolean result = envoyIstioGatewayService.updateGatewaySetting(new EnvoyGatewaySettingDto(),
			                                                               GatewayDto.toMeta(gatewayDto));
			if (!result) {
				logger.warn("调用ApiPlane创建网关失败,请检查网络连接");
				return apiReturn(CommonErrorCode.UpdateToGwFailure);
			}
		}
		long gwId = gatewayInfoService.addGatewayByMetaDto(gatewayDto);
		return apiReturnSuccess(gwId);
	}

	@GetMapping(params = {"Action=DescribeGatewayList"})
	public Object getGwInfoList(@RequestParam(value = "Pattern", required = false) String pattern,
	                                            @Min(0) @RequestParam(value = "Offset", required = false,
	                                                                  defaultValue = "0") long offset,
	                                            @Min(1) @Max(1000) @RequestParam(value = "Limit", required = false,
	                                                                             defaultValue = "20") long limit) {
		List<GatewayInfo> gatewayInfoList = gatewayInfoService.findGatewayByLimit(pattern, offset, limit);
		long gatewayCount = gatewayInfoService.getGatewayCount(pattern);
		List<GatewayDto> gatewayDtos = gatewayInfoList.stream().map(GatewayDto::fromMeta).collect(Collectors.toList());
		Map<String, Object> result = new HashMap<>();
		result.put("GatewayCount", gatewayCount);
		result.put("GatewayInfos", gatewayDtos);
		return apiReturn(CommonErrorCode.Success, result);
	}

	@GetMapping(params = {"Action=DescribeGateway"})
	public Object getGwInfoById(@RequestParam(value = "GwId") long id) {
		GatewayInfo gatewayInfo = gatewayInfoService.get(id);
		if (gatewayInfo == null) {
			logger.info("不存在gwId下的网关，gwId:{}", id);
			return apiReturn(CommonErrorCode.NoSuchGwId);
		}
		Map<String, Object> result = new HashMap<>();
		result.put("GatewayInfo", GatewayDto.fromMeta(gatewayInfo));
		return apiReturn(CommonErrorCode.Success, result);
	}

	@PostMapping(params = {"Action=UpdateGateway"})
	public Object updateGwInfo(@Validated @RequestBody GatewayDto gatewayDto) {
		logger.info("修改网关信息,gatewayInfo:", gatewayDto.toString());
		ErrorCode errorCode = gatewayInfoService.checkUpdateParam(gatewayDto);
		if (!ErrorCodeEnum.Success.getCode().equals(errorCode.getCode())) {
			return apiReturn(errorCode);
		}

		if (!gatewayInfoService.updateGwInfo(GatewayDto.toMeta(gatewayDto), false)) {
			logger.info("更新网关失败");
			return apiReturn(CommonErrorCode.InternalServerError);
		}
		return apiReturn(CommonErrorCode.Success);
	}

	@PostMapping(params = {"Action=UpdateGatewayVirtualHosts"})
	public String updateGatewayVirtualHosts(@Validated @RequestBody EnvoyGatewayVirtualHostDto gatewayVirtualHost) {
		logger.info("项目关联网关，修改网关virtual host， gatewayVirtualHost:{}", gatewayVirtualHost);

		ErrorCode checkResult = envoyGatewayService.checkVirtualHostList(gatewayVirtualHost.getGwId(),
		                                                                 gatewayVirtualHost.toEnvoyVirutalHostList());
		if (!CommonErrorCode.Success.getCode().equals(checkResult.getCode())) {
			return apiReturn(checkResult);
		}

		boolean updateSucc = envoyGatewayService.updateVirtualHostList(gatewayVirtualHost.getGwId(),
		                                                               gatewayVirtualHost.toEnvoyVirutalHostList());
		if (!updateSucc) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}
		return apiReturn(CommonErrorCode.Success);
	}

	@PostMapping(params = {"Action=CreateGatewayVirtualHost"})
	public String createGatewayVirtualHost(@Validated @RequestBody EnvoyVirtualHostDto virtualHostDto) {
		logger.info("创建单个vh， virtualHostDto:{} ", virtualHostDto);
		ErrorCode checkResult = envoyGatewayService.checkCreateVirtualHost(virtualHostDto.toMeta());
		if (!CommonErrorCode.Success.getCode().equals(checkResult.getCode())) {
			return apiReturn(checkResult);
		}

		boolean createSucc = envoyGatewayService.createVirtualHost(virtualHostDto.toMeta());
		if (!createSucc) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}
		return apiReturn(CommonErrorCode.Success);
	}

	@PostMapping(params = {"Action=UpdateGatewayVirtualHost"})
	public String updateGatewayVirtualHost(@Validated @RequestBody EnvoyVirtualHostUpdateDto updateDto) {
		logger.info("更新vh，updateDto:{}", updateDto);
		ErrorCode checkResult = envoyGatewayService.checkUpdateVirtualHost(updateDto.getVirtualHostId(),
		                                                                   updateDto.getHostList());
		if (!CommonErrorCode.Success.getCode().equals(checkResult.getCode())) {
			return apiReturn(checkResult);
		}
		boolean updateSucc = envoyGatewayService.updateVirtualHost(updateDto.getVirtualHostId(),
		                                                           updateDto.getHostList());
		if (!updateSucc) {
			return apiReturn(CommonErrorCode.InternalServerError);
		}
		return apiReturn(CommonErrorCode.Success);
	}

	@GetMapping(params = {"Action=DescribeGatewayVirtualHosts"})
	public String getGatewayVirtualHosts(@RequestParam(value = "GwId", required = false, defaultValue = "0") long gwId,
	                                     @RequestParam(value = "ProjectId", required = false,
	                                                   defaultValue = "0") long projectId,
	                                     @RequestParam(value = "Domain", required = false) String domain,
	                                     @RequestParam(value = "Limit", required = false,
	                                                   defaultValue = "20") long limit,
	                                     @RequestParam(value = "Offset", required = false,
	                                                   defaultValue = "0") long offset) {
		logger.info("查询网关的virtual host列表信息, gwId:{}, projectId:{}, domain:{}, limit:{}, offset:{}", gwId,
		            projectId, domain, limit, offset);
		List<Long> projectIdList = Lists.newArrayList();
		Map<String, Object> result = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		if (0 < projectId) {
			projectIdList.add(projectId);
		}
		long totalCount = envoyGatewayService.getVirtualHostCount(gwId, projectIdList, domain);
		result.put("TotalCount", totalCount);
		if (0 == totalCount || totalCount < offset) {
			result.put("VirtualHostList", Lists.newArrayList());
		}
		List<EnvoyVirtualHostInfo> virtualHostInfoList = envoyGatewayService.getVirtualHostList(gwId, projectIdList,
		                                                                                        domain, limit, offset);
		result.put("VirtualHostList", CollectionUtils.isEmpty(virtualHostInfoList) ? Lists.newArrayList()
		                                                                           : virtualHostInfoList.stream().map(
			                                                                           EnvoyVirtualHostDto::fromMeta)
		                                                                                                .collect(
			                                                                                                Collectors
				                                                                                                .toList()));

		return apiReturn(HttpStatus.SC_OK, null, null, result);
	}

	@GetMapping(params = {"Action=DescribeGatewayVirtualHost"})
	public String getGatewayVirtualHost(@RequestParam(value = "VirtualHostId") long virtualHostId) {
		logger.info("查询网关的 VirtualHost 详情, virtualHostId:{}", virtualHostId);
		EnvoyVirtualHostInfo virtualHostInfo = envoyGatewayService.getVirtualHost(virtualHostId);
		if (null == virtualHostInfo) {
			return apiReturn(HttpStatus.SC_OK, null, null, null);
		}
		Map<String, Object> result = (Map<String, Object>) JSON.toJSON(EnvoyVirtualHostDto.fromMeta(virtualHostInfo));
		return apiReturn(HttpStatus.SC_OK, null, null, result);
	}

	/**
	 * 获取插件列表
	 */
	@GetMapping(params = {"Action=DescribePluginManager"})
	public Object describePluginManager(@RequestParam(name = "GwId") long gwId) {
		GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
		if (gatewayInfo == null) {
			logger.info("未查询到网关信息, GwId = {}", gwId);
			return apiReturn(CommonErrorCode.NoSuchGateway);
		}
		List<EnvoyPluginManagerDto> envoyPluginManager = envoyGatewayService.getEnvoyPluginManager(gatewayInfo);
		return apiReturnSuccess(envoyPluginManager);
	}

	/**
	 * 修改插件列表
	 */
	@PostMapping(params = {"Action=UpdatePluginManager"})
	public Object updatePluginManager(@RequestParam(name = "GwId") long gwId, @RequestParam(name = "Name") String name,
	                                  @RequestParam(name = "Enable") boolean enable) {
		GatewayInfo gatewayInfo = gatewayInfoService.get(gwId);
		if (gatewayInfo == null) {
			logger.info("未查询到网关信息, GwId = {}", gwId);
			return apiReturn(CommonErrorCode.NoSuchGateway);
		}
		ErrorCode errorCode = envoyGatewayService.checkEnvoyPluginManager(gatewayInfo, name, enable);
		if (!CommonErrorCode.Success.equals(errorCode)) {
			return apiReturn(errorCode);
		}
		boolean result = envoyGatewayService.updateEnvoyPluginManager(gatewayInfo, name, enable);
		return apiReturnSuccess(result);
	}

}
