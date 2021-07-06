package org.hango.cloud.ncegdashboard.envoy.meta;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 存储各个环境的信息
 *
 * @Date: 创建时间: 2018/1/17 下午5:22.
 */
public class GatewayInfo implements Serializable {

	private static final long serialVersionUID = 7147341067988626279L;

	/**
	 * 数据库主键id
	 */
	private long id;

	/**
	 * 创建时间
	 */
	private long createDate;

	/**
	 * 更新时间
	 */
	private long modifyDate;

	/**
	 * 网关名称
	 */
	private String gwName;

	/**
	 * 网关地址
	 */
	private String gwAddr;

	/**
	 * 网关描述
	 */
	private String description;

	/**
	 * 网关所属项目id
	 */
	private long projectId;


	private String gwUniId;

	/**
	 * 网关类型, g0/envoy
	 */
	private String gwType;

	/**
	 * envoy网关api-plane地址
	 */
	private String apiPlaneAddr;

	/**
	 * envoy网关gwClusterName地址
	 */
	private String gwClusterName;

	private List<String> hostList;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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

	public String getGwUniId() {
		return gwUniId;
	}

	public void setGwUniId(String gwUniId) {
		this.gwUniId = gwUniId;
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
	public int hashCode() {
		return Objects.hash(getId());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		GatewayInfo that = (GatewayInfo) o;
		return getId() == that.getId();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
