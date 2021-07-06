package org.hango.cloud.dashboard.envoy.web.dto;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hango.cloud.dashboard.envoy.meta.GatewayInfo;
import org.hango.cloud.dashboard.envoy.util.BeanUtil;
import org.hango.cloud.dashboard.envoy.util.Const;
import org.hango.cloud.dashboard.envoy.web.holder.ProjectTraceHolder;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 与前端交互的网关dto
 */
public class GatewayDto implements Serializable {

	private static final long serialVersionUID = -289652590295163660L;

	@JSONField(name = "GwId")
	private long id;

	/**
	 * 网关类型envoy
	 */
	@JSONField(name = "GwType")
	@NotEmpty(message = "网关类型不能为空")
	@Pattern(regexp = "envoy", message = "网关类型填写错误")
	private String gwType;

	/**
	 * 网关名称
	 */
	@JSONField(name = "GwName")
	@NotEmpty
	@Pattern(regexp = Const.REGEX_GATEWAY_NAME)
	private String gwName;

	/**
	 * 网关地址
	 */
	@JSONField(name = "GwAddr")
	@NotEmpty
	@Pattern(regexp = Const.REGEX_GATEWAY_URL)
	private String gwAddr;

	/**
	 * Envoy网关对接的api-plane的地址
	 */
	@JSONField(name = "ApiPlaneAddr")
	@Pattern(regexp = "^(http://|https://)\\S{5,254}|", message = "网关对接api-plane地址不合法")
	private String apiPlaneAddr;

	/**
	 * Envoy网关部署时gw_cluster标签的值，用于区分envoy示例所属网关集群
	 */
	@JSONField(name = "GwClusterName")
	@Pattern(regexp = "^[\\s\\S]{1,127}|", message = "参数网关集群名称不能为空且长度不能超过128字符")
	private String gwClusterName;

	/**
	 * 网关描述
	 */
	@JSONField(name = "Description")
	@Pattern(regexp = Const.REGEX_DESCRIPTION)
	private String description;

	/**
	 * 网关所属项目id
	 */
	@JSONField(name = "ProjectId")
	private long projectId;

	/**
	 * 网关创建时间
	 */
	@JSONField(name = "CreateDate")
	private long createDate;

	/**
	 * 网关更新时间
	 */
	@JSONField(name = "ModifiedDate")
	private long modifyDate;

	/**
	 * 网关关联host列表
	 */
	@JSONField(name = "HostList")
	@NotNull
	private List<String> hostList;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getProjectId() {
		return projectId;
	}

	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}

	public long getCreateDate() {
		return createDate;
	}

	public void setCreateDate(long createDate) {
		this.createDate = createDate;
	}

	public long getModifyDate() {
		return modifyDate;
	}

	public void setModifyDate(long modifyDate) {
		this.modifyDate = modifyDate;
	}

	public String getGwType() {
		return gwType;
	}

	public void setGwType(String gwType) {
		this.gwType = gwType;
	}

	public String getApiPlaneAddr() {
		return apiPlaneAddr;
	}

	public void setApiPlaneAddr(String apiPlaneAddr) {
		this.apiPlaneAddr = apiPlaneAddr;
	}

	public String getGwClusterName() {
		return gwClusterName;
	}

	public void setGwClusterName(String gwClusterName) {
		this.gwClusterName = gwClusterName;
	}

	public List<String> getHostList() {
		return hostList;
	}

	public void setHostList(List<String> hostList) {
		this.hostList = hostList;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public static GatewayDto fromMeta(GatewayInfo gatewayInfo) {
		GatewayDto dto = BeanUtil.copy(gatewayInfo, GatewayDto.class);
		return dto;
	}

	public static GatewayInfo toMeta(GatewayDto dto) {
		GatewayInfo meta = new GatewayInfo();
		meta.setId(dto.getId());
		meta.setGwName(dto.getGwName());
		meta.setGwAddr(dto.getGwAddr());
		meta.setDescription(dto.getDescription());
		meta.setGwType(dto.getGwType());
		meta.setHostList(dto.getHostList().stream().map(String::trim).collect(Collectors.toList()));
		meta.setApiPlaneAddr(dto.getApiPlaneAddr());
		meta.setGwClusterName(dto.getGwClusterName());
		meta.setProjectId(dto.getProjectId() == 0 ? ProjectTraceHolder.DEFAULT_PROJECT_ID : dto.getProjectId());
		return meta;
	}

}
