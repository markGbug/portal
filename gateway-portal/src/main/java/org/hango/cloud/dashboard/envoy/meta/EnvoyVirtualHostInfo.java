package org.hango.cloud.dashboard.envoy.meta;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

/**
 * 网关vh meta
 * <p>
 * 2020-01-09
 */
public class EnvoyVirtualHostInfo {

	/**
	 * vh id
	 */
	private long id;

	/**
	 * 项目id
	 */
	private long projectId;

	/**
	 * 网关id
	 */
	private long gwId;

	/**
	 * vh中域名列表
	 */
	private String hosts;

	/**
	 * vh中域名列表
	 */
	private List<String> hostList;

	/**
	 * vh唯一标识
	 */
	private String virtualHostCode;

	private long createTime;

	private long updateTime;

	public EnvoyVirtualHostInfo(long projectId, long gwId, List<String> hostList) {
		this.projectId = projectId;
		this.gwId = gwId;
		this.hostList = hostList;
		this.hosts = JSON.toJSONString(this.hostList);
	}

	public EnvoyVirtualHostInfo() {
	}

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

	public String getHosts() {
		return hosts;
	}

	public void setHosts(String hosts) {
		this.hosts = hosts;
	}

	public String getVirtualHostCode() {
		return virtualHostCode;
	}

	public void setVirtualHostCode(String virtualHostCode) {
		this.virtualHostCode = virtualHostCode;
	}

	public List<String> getHostList() {
		return hostList;
	}

	public void setHostList(List<String> hostList) {
		this.hostList = hostList;
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

}
