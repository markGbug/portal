package org.hango.cloud.dashboard.envoy.web.dto;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * Envoy网关vh更新dto
 * <p>
 * 2020-03-09
 */
public class EnvoyVirtualHostUpdateDto {

	@JSONField(name = "VirtualHostId")
	private long virtualHostId;

	@JSONField(name = "HostList")
	@NotNull
	private List<String> hostList;

	public long getVirtualHostId() {
		return virtualHostId;
	}

	public void setVirtualHostId(long virtualHostId) {
		this.virtualHostId = virtualHostId;
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

}
