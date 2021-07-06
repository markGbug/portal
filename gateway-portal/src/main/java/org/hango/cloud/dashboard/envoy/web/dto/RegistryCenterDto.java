package org.hango.cloud.ncegdashboard.envoy.web.dto;

import com.alibaba.fastjson.annotation.JSONField;
import org.hango.cloud.ncegdashboard.envoy.meta.RegistryCenterInfo;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @version 1.0
 * @Type
 * @Desc
 * @date 2020/1/12
 */
public class RegistryCenterDto {

	/**
	 * 注册中心配置主键
	 */
	@JSONField(name = "Id")
	private long id;

	/**
	 * 注册中心类型
	 */
	@JSONField(name = "RegistryType")
	@NotEmpty
	private String registryType;

	/**
	 * 注册中心地址
	 */
	@JSONField(name = "RegistryAddr")
	@NotEmpty
	private String registryAddr;

	/**
	 * 注册中心别名
	 */
	@JSONField(name = "RegistryAlias")
	@NotEmpty
	private String registryAlias;

	/**
	 * 创建时间
	 */
	@JSONField(name = "CreateDate")
	private Long createDate;

	/**
	 * 修改时间
	 */
	@JSONField(name = "ModifyDate")
	private Long modifyDate;

	@JSONField(name = "ProjectId")
	private long projectId;

	public RegistryCenterDto() {
	}

	public RegistryCenterDto(String registryType, String registryAlias) {
		this.registryType = registryType;
		this.registryAlias = registryAlias;
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

	public String getRegistryType() {
		return registryType;
	}

	public void setRegistryType(String registryType) {
		this.registryType = registryType;
	}

	public String getRegistryAddr() {
		return registryAddr;
	}

	public void setRegistryAddr(String registryAddr) {
		this.registryAddr = registryAddr;
	}

	public String getRegistryAlias() {
		return registryAlias;
	}

	public void setRegistryAlias(String registryAlias) {
		this.registryAlias = registryAlias;
	}

	public Long getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Long createDate) {
		this.createDate = createDate;
	}

	public Long getModifyDate() {
		return modifyDate;
	}

	public void setModifyDate(Long modifyDate) {
		this.modifyDate = modifyDate;
	}

	public static RegistryCenterInfo trans(RegistryCenterDto registryCenter) {
		if (registryCenter == null) {
			return null;
		}
		RegistryCenterInfo registryCenterInfo = new RegistryCenterInfo();
		registryCenterInfo.setId(registryCenter.getId());
		registryCenterInfo.setRegistryType(registryCenter.getRegistryType());
		registryCenterInfo.setRegistryAddr(registryCenter.getRegistryAddr());
		registryCenterInfo.setRegistryAlias(registryCenter.getRegistryAlias());
		registryCenterInfo.setCreateDate(System.currentTimeMillis());
		registryCenterInfo.setModifyDate(System.currentTimeMillis());
		registryCenterInfo.setProjectId(registryCenter.getProjectId());
		return registryCenterInfo;
	}

	public static RegistryCenterDto trans(RegistryCenterInfo registryCenter) {
		if (registryCenter == null) {
			return null;
		}
		RegistryCenterDto registryCenterDto = new RegistryCenterDto();
		registryCenterDto.setId(registryCenter.getId());
		registryCenterDto.setRegistryType(registryCenter.getRegistryType());
		registryCenterDto.setRegistryAddr(registryCenter.getRegistryAddr());
		registryCenterDto.setRegistryAlias(registryCenter.getRegistryAlias());
		registryCenterDto.setCreateDate(registryCenter.getCreateDate());
		registryCenterDto.setModifyDate(registryCenter.getModifyDate());
		registryCenterDto.setProjectId(registryCenter.getProjectId());
		return registryCenterDto;
	}

}
