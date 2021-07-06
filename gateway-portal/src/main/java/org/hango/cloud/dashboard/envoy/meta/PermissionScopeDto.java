package org.hango.cloud.ncegdashboard.envoy.meta;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

public class PermissionScopeDto implements Serializable {

	private static final long serialVersionUID = 4284492442066101272L;

	@JSONField(name = "id")
	private long id;

	@JSONField(name = "PermissionScopeName")
	private String permissionScopeName;

	@JSONField(name = "PermissionScopeEnName")
	private String permissonScopeEnvName;

	@JSONField(name = "ParentId")
	private long parentId;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getPermissionScopeName() {
		return permissionScopeName;
	}

	public void setPermissionScopeName(String permissionScopeName) {
		this.permissionScopeName = permissionScopeName;
	}

	public String getPermissonScopeEnvName() {
		return permissonScopeEnvName;
	}

	public void setPermissonScopeEnvName(String permissonScopeEnvName) {
		this.permissonScopeEnvName = permissonScopeEnvName;
	}

	public long getParentId() {
		return parentId;
	}

	public void setParentId(long parentId) {
		this.parentId = parentId;
	}

}
