package org.hango.cloud.ncegdashboard.envoy.service.impl;

import org.hango.cloud.ncegdashboard.envoy.meta.PermissionScopeDto;
import org.hango.cloud.ncegdashboard.envoy.service.IGatewayProjectService;
import org.springframework.stereotype.Service;

@Service
public class GatewayProjectImpl implements IGatewayProjectService {

	@Override
	public PermissionScopeDto getProjectScopeDto(long projectId) {
		PermissionScopeDto permissionScopeDto = new PermissionScopeDto();
		permissionScopeDto.setId(projectId);
		permissionScopeDto.setParentId(1L);
		permissionScopeDto.setPermissionScopeName("hango-test");
		permissionScopeDto.setPermissonScopeEnvName("hango");
		return permissionScopeDto;
	}

}
