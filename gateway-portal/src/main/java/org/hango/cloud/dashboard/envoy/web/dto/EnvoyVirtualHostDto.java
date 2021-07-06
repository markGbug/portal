package org.hango.cloud.dashboard.envoy.web.dto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hango.cloud.dashboard.envoy.meta.EnvoyVirtualHostInfo;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 网关vh Dto
 * <p>
 * 2020-01-08
 */
public class EnvoyVirtualHostDto {

	@JSONField(name = "Id")
	private long id;

	@JSONField(name = "ProjectId")
	@Min(1)
	private long projectId;

	@JSONField(name = "GwId")
	@Min(1)
	private long gwId;

	@JSONField(name = "HostList")
	@NotNull
	private List<String> hostList;

	@JSONField(name = "VirtualHostCode")
	private String virtualHostCode;

	@JSONField(name = "CreateTime")
	private long createTime;

	@JSONField(name = "UpdateTime")
	private long updateTime;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getProjectId() {
		return projectId;
	}

	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}

	public long getGwId() {
		return gwId;
	}

	public void setGwId(long gwId) {
		this.gwId = gwId;
	}

	public List<String> getHostList() {
		return hostList;
	}

	public void setHostList(List<String> hostList) {
		this.hostList = hostList;
	}

	public String getVirtualHostCode() {
		return virtualHostCode;
	}

	public void setVirtualHostCode(String virtualHostCode) {
		this.virtualHostCode = virtualHostCode;
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

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public static EnvoyVirtualHostDto fromMeta(EnvoyVirtualHostInfo info) {
		EnvoyVirtualHostDto dto = new EnvoyVirtualHostDto();
		dto.setId(info.getId());
		dto.setGwId(info.getGwId());
		dto.setHostList(info.getHostList());
		dto.setProjectId(info.getProjectId());
		dto.setCreateTime(info.getCreateTime());
		dto.setUpdateTime(info.getUpdateTime());
		dto.setVirtualHostCode(info.getVirtualHostCode());
		return dto;
	}

	public EnvoyVirtualHostInfo toMeta() {
		EnvoyVirtualHostInfo info = new EnvoyVirtualHostInfo();
		info.setId(this.id);
		info.setGwId(this.gwId);
		List<String> hostListTrim = hostList.stream().map(String::trim).collect(Collectors.toList());
		info.setHostList(hostListTrim);
		info.setProjectId(this.projectId);
		info.setCreateTime(this.createTime);
		info.setUpdateTime(this.updateTime);
		info.setHosts(JSON.toJSONString(hostListTrim));
		info.setVirtualHostCode(this.virtualHostCode);
		return info;
	}

}
