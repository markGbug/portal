package org.hango.cloud.ncegdashboard.envoy.service;

import org.hango.cloud.ncegdashboard.envoy.meta.PermissionScopeDto;

public interface IGatewayProjectService {

	/**
	 * 根据项目id获取项目的详细描述
	 */
	PermissionScopeDto getProjectScopeDto(long projectId);

}
