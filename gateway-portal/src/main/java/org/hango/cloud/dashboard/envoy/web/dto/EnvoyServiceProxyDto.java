package org.hango.cloud.ncegdashboard.envoy.web.dto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hango.cloud.ncegdashboard.envoy.meta.EnvoyServiceProxyInfo;
import org.hibernate.validator.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

/**
 * 元数据发布信息相关dto
 */
public class EnvoyServiceProxyDto {

	/**
	 * 数据库主键自增id
	 */
	@JSONField(name = "Id")
	private long id;

	@JSONField(name = "ServiceId")
	@Min(value = 1)
	private long serviceId;

	/**
	 * 服务名称，用于前端展示，不进行存储
	 */
	@JSONField(name = "ServiceName")
	private String serviceName;

	/**
	 * 服务标识，用于前端展示及告警获取，不进行存储
	 */
	@JSONField(name = "ServiceTag")
	private String serviceTag;

	/**
	 * 服务类型，用于前端显示
	 */
	@JSONField(name = "ServiceType")
	private String serviceType;

	/**
	 * 服务唯一标识
	 */
	@JSONField(name = "Code")
	private String code;

	/**
	 * 服务发布所选服务名称（网关真实名称）
	 * 静态发布，则为后端服务host；注册中心发布k8s service
	 */
	@JSONField(name = "BackendService")
	@NotBlank
	private String backendService;

	/**
	 * 注册中心类型,DYNAMIC时必填，默认Kubernetes
	 */
	@JSONField(name = "RegistryCenterType")
	private String registryCenterType;

	/**
	 * 注册中心地址
	 */
	@JSONField(name = "RegistryCenterAddr")
	private String registryCenterAddr;

	/**
	 * 路由发布协议，可以为空，默认为
	 */
	@JSONField(name = "PublishProtocol")
	@Pattern(regexp = "http|https")
	private String publishProtocol = "http";

	/**
	 * 服务发布策略，DYNAMIC,STATIC
	 */
	@JSONField(name = "PublishType")
	@NotBlank
	private String publishType;

	/**
	 * envoy网关id
	 */
	@JSONField(name = "GwId")
	@Min(1)
	private long gwId;

	/**
	 * 网关名称，用于前端展示，不进行存储
	 */
	@JSONField(name = "GwName")
	private String gwName;

	@JSONField(name = "GwAddr")
	private String gwAddr;

	@JSONField(name = "EnvId")
	private String envId;

	/**
	 * 发布时间
	 */
	@JSONField(name = "CreateTime")
	private long createTime;

	/**
	 * 更新时间
	 */
	@JSONField(name = "UpdateTime")
	private long updateTime;

	@JSONField(name = "LoadBalancer")
	@Pattern(regexp = "|ROUND_ROBIN|LEAST_CONN|RANDOM")
	private String loadBalancer = "ROUND_ROBIN";

	/**
	 * 服务健康状态：0表示异常；1表示健康；2表示部分健康
	 */
	@JSONField(name = "HealthyStatus")
	private Integer healthyStatus;

	/**
	 * 服务发布后，后端服务对应的port信息
	 * 只有动态发布，才有port信息，静态发布不存在port信息
	 */
	@JSONField(name = "Port")
	private List<Integer> port;

	/**
	 * 版本集合
	 */
	@JSONField(name = "Subsets")
	private List<EnvoySubsetDto> subsets;

	/**
	 * 高级配置包含负载均衡策略和连接池
	 */
	@JSONField(name = "TrafficPolicy")
	private EnvoyServiceTrafficPolicyDto trafficPolicy;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getServiceId() {
		return serviceId;
	}

	public void setServiceId(long serviceId) {
		this.serviceId = serviceId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getBackendService() {
		return backendService;
	}

	public void setBackendService(String backendService) {
		this.backendService = backendService;
	}

	public String getPublishType() {
		return publishType;
	}

	public void setPublishType(String publishType) {
		this.publishType = publishType;
	}

	public long getGwId() {
		return gwId;
	}

	public void setGwId(long gwId) {
		this.gwId = gwId;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getGwName() {
		return gwName;
	}

	public void setGwName(String gwName) {
		this.gwName = gwName;
	}

	public String getGwAddr() {
		return gwAddr;
	}

	public void setGwAddr(String gwAddr) {
		this.gwAddr = gwAddr;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public String getPublishProtocol() {
		return publishProtocol;
	}

	public void setPublishProtocol(String publishProtocol) {
		this.publishProtocol = publishProtocol;
	}

	public String getLoadBalancer() {
		return loadBalancer;
	}

	public void setLoadBalancer(String loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	public Integer getHealthyStatus() {
		return healthyStatus;
	}

	public void setHealthyStatus(Integer healthyStatus) {
		this.healthyStatus = healthyStatus;
	}

	public List<Integer> getPort() {
		return port;
	}

	public void setPort(List<Integer> port) {
		this.port = port;
	}

	public List<EnvoySubsetDto> getSubsets() {
		return subsets;
	}

	public void setSubsets(List<EnvoySubsetDto> subsets) {
		this.subsets = subsets;
	}

	public String getServiceTag() {
		return serviceTag;
	}

	public void setServiceTag(String serviceTag) {
		this.serviceTag = serviceTag;
	}

	public String getRegistryCenterType() {
		return registryCenterType;
	}

	public void setRegistryCenterType(String registryCenterType) {
		this.registryCenterType = registryCenterType;
	}

	public String getRegistryCenterAddr() {
		return registryCenterAddr;
	}

	public void setRegistryCenterAddr(String registryCenterAddr) {
		this.registryCenterAddr = registryCenterAddr;
	}

	public EnvoyServiceTrafficPolicyDto getTrafficPolicy() {
		return trafficPolicy;
	}

	public void setTrafficPolicy(EnvoyServiceTrafficPolicyDto trafficPolicy) {
		this.trafficPolicy = trafficPolicy;
	}

	public String getEnvId() {
		return envId;
	}

	public void setEnvId(String envId) {
		this.envId = envId;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public static EnvoyServiceProxyInfo toMeta(EnvoyServiceProxyDto envoyServiceProxyDto) {
		EnvoyServiceProxyInfo envoyServiceProxyInfo = new EnvoyServiceProxyInfo();
		envoyServiceProxyInfo.setId(envoyServiceProxyDto.getId());
		envoyServiceProxyInfo.setServiceId(envoyServiceProxyDto.getServiceId());
		envoyServiceProxyInfo.setCode(new StringBuilder().append(envoyServiceProxyDto.getPublishType()).append("-")
		                                                 .append(envoyServiceProxyDto.getServiceId()).toString());
		envoyServiceProxyInfo.setBackendService(envoyServiceProxyDto.getBackendService());
		envoyServiceProxyInfo.setPublishProtocol(envoyServiceProxyDto.getPublishProtocol());
		envoyServiceProxyInfo.setPublishType(envoyServiceProxyDto.getPublishType());
		envoyServiceProxyInfo.setRegistryCenterType(envoyServiceProxyDto.getRegistryCenterType());
		envoyServiceProxyInfo.setRegistryCenterAddr(envoyServiceProxyDto.getRegistryCenterAddr());
		envoyServiceProxyInfo.setGwId(envoyServiceProxyDto.getGwId());
		envoyServiceProxyInfo.setCreateTime(envoyServiceProxyDto.getCreateTime());
		envoyServiceProxyInfo.setUpdateTime(envoyServiceProxyDto.getUpdateTime());
		envoyServiceProxyInfo.setLoadBalancer(envoyServiceProxyDto.getLoadBalancer());
		envoyServiceProxyInfo.setTrafficPolicy(
			envoyServiceProxyDto.getTrafficPolicy() != null ?
			JSON.toJSONString(envoyServiceProxyDto.getTrafficPolicy())
			                                                : null);
		envoyServiceProxyInfo.setSubsets(
			envoyServiceProxyDto.getSubsets() != null ? JSON.toJSONString(envoyServiceProxyDto.getSubsets()) : null);
		return envoyServiceProxyInfo;
	}

	public static EnvoyServiceProxyDto toDto(EnvoyServiceProxyInfo envoyServiceProxyInfo) {
		EnvoyServiceProxyDto envoyServiceProxyDto = new EnvoyServiceProxyDto();
		envoyServiceProxyDto.setId(envoyServiceProxyInfo.getId());
		envoyServiceProxyDto.setServiceId(envoyServiceProxyInfo.getServiceId());
		envoyServiceProxyDto.setCode(envoyServiceProxyInfo.getCode());
		envoyServiceProxyDto.setBackendService(envoyServiceProxyInfo.getBackendService());
		envoyServiceProxyDto.setRegistryCenterType(envoyServiceProxyInfo.getRegistryCenterType());
		envoyServiceProxyDto.setRegistryCenterAddr(envoyServiceProxyInfo.getRegistryCenterAddr());
		envoyServiceProxyDto.setPublishProtocol(envoyServiceProxyInfo.getPublishProtocol());
		envoyServiceProxyDto.setPublishType(envoyServiceProxyInfo.getPublishType());
		envoyServiceProxyDto.setGwId(envoyServiceProxyInfo.getGwId());
		envoyServiceProxyDto.setCreateTime(envoyServiceProxyInfo.getCreateTime());
		envoyServiceProxyDto.setUpdateTime(envoyServiceProxyInfo.getUpdateTime());
		envoyServiceProxyDto.setLoadBalancer(envoyServiceProxyInfo.getLoadBalancer());
		envoyServiceProxyDto.setSubsets(setSubsetForDto(envoyServiceProxyInfo));
		envoyServiceProxyDto.setTrafficPolicy(setTrafficPolicyForDto(envoyServiceProxyInfo));

		return envoyServiceProxyDto;
	}

	/**
	 * 为dto增加版本信息，因为db中存储的是字符串，dto中是list，不能直接用BeanUtil.copy来赋值
	 * 用于前端展示
	 */
	public static List<EnvoySubsetDto> setSubsetForDto(EnvoyServiceProxyInfo envoyServiceProxyInfo) {
		//增加版本信息
		if (StringUtils.isNotBlank(envoyServiceProxyInfo.getSubsets())) {
			List<JSONObject> subsetObject = JSON.parseObject(envoyServiceProxyInfo.getSubsets(), List.class);
			List<EnvoySubsetDto> subsets = new ArrayList<>();
			for (JSONObject subsetTemp : subsetObject) {
				subsets.add(JSONObject.toJavaObject(subsetTemp, EnvoySubsetDto.class));
			}
			return subsets;
		}
		return null;
	}

	/**
	 * 为dto增加负载均衡和连接池信息，因为db中存储的是字符串，dto中是list，不能直接用BeanUtil.copy来赋值
	 * 用于前端展示
	 */
	public static EnvoyServiceTrafficPolicyDto setTrafficPolicyForDto(EnvoyServiceProxyInfo envoyServiceProxyInfo) {
		//增加负载均衡和连接池信息
		if (StringUtils.isNotBlank(envoyServiceProxyInfo.getTrafficPolicy())) {
			EnvoyServiceTrafficPolicyDto trafficPolicy = JSON.parseObject(envoyServiceProxyInfo.getTrafficPolicy(),
			                                                              EnvoyServiceTrafficPolicyDto.class);
			return trafficPolicy;
		}
		return null;
	}

}
