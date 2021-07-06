package org.hango.cloud.dashboard.envoy.dao.impl;

import org.apache.commons.lang3.math.NumberUtils;
import org.hango.cloud.dashboard.envoy.util.Const;
import org.hango.cloud.dashboard.envoy.dao.IEnvoyServiceProxyDao;
import org.hango.cloud.dashboard.envoy.meta.EnvoyServiceProxyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关元服务关联至envoy网关服务相关dao
 */
@Component
public class EnvoyServiceProxyDaoImpl extends BaseDao implements IEnvoyServiceProxyDao {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyServiceProxyDaoImpl.class);

	@Override
	public long add(EnvoyServiceProxyInfo envoyServiceProxyInfo) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		String sql =
			"insert into apigw_envoy_service_proxy (service_id, code, publish_protocol, backend_service, publish_type,"
			+ " gw_id, project_id, create_time, update_time, load_balancer, subsets, registry_center_addr, "
			+ "registry_center_type, traffic_policy) "
			+ " values (:serviceId, :code, :publishProtocol, :backendService, :publishType, :gwId, :projectId, "
			+ ":createTime, :updateTime, :loadBalancer, :subsets, :registryCenterAddr, :registryCenterType, "
			+ ":trafficPolicy)";
		SqlParameterSource ps = new BeanPropertySqlParameterSource(envoyServiceProxyInfo);
		namedParameterJdbcTemplate.update(sql, ps, keyHolder);
		logger.info("add EnvoyServiceProxyInfo: {}", envoyServiceProxyInfo);
		return keyHolder.getKey().intValue();
	}

	@Override
	public long update(EnvoyServiceProxyInfo envoyServiceProxyInfo) {
		String sql = "update apigw_envoy_service_proxy set service_id=:serviceId, code=:code, "
		             + "publish_protocol=:publishProtocol, backend_service=:backendService, "
		             + "publish_type=:publishType, "
		             + "gw_id=:gwId, update_time=:updateTime, load_balancer=:loadBalancer, subsets=:subsets, "
		             + "registry_center_addr=:registryCenterAddr, registry_center_type=:registryCenterType,"
		             + " traffic_policy=:trafficPolicy where id=:id";
		SqlParameterSource ps = new BeanPropertySqlParameterSource(envoyServiceProxyInfo);
		logger.info("update EnvoyServiceProxyInfo: {}", envoyServiceProxyInfo);
		return namedParameterJdbcTemplate.update(sql, ps);
	}

	@Override
	public int delete(EnvoyServiceProxyInfo envoyServiceProxyInfo) {
		String sql = "delete from apigw_envoy_service_proxy where id=:id";
		SqlParameterSource ps = new BeanPropertySqlParameterSource(envoyServiceProxyInfo);
		logger.info("delete EnvoyServiceInfo: {}", envoyServiceProxyInfo);
		return namedParameterJdbcTemplate.update(sql, ps);
	}

	@Override
	public EnvoyServiceProxyInfo get(long id) {
		String sql = "select * from apigw_envoy_service_proxy where id=:id";
		return queryForObject(sql, new MapSqlParameterSource("id", id), new EnvoyServiceProxyRowMapper());
	}

	@Override
	public List<EnvoyServiceProxyInfo> findAll() {
		String sql = "select * from apigw_envoy_service_proxy";
		return namedParameterJdbcTemplate.query(sql, new EnvoyServiceProxyRowMapper());
	}

	@Override
	public List<EnvoyServiceProxyInfo> getRecordsByField(Map<String, Object> params) {
		String head = "select * from apigw_envoy_service_proxy where ";
		String sql = getQueryCondition(head, params);
		return namedParameterJdbcTemplate.query(sql, params, new EnvoyServiceProxyRowMapper());
	}

	@Override
	public int getCountByFields(Map<String, Object> params) {
		String head = "select count(*) from apigw_envoy_service_proxy where ";
		String sql = getQueryCondition(head, params);
		return namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
	}

	@Override
	public List<EnvoyServiceProxyInfo> getServiceProxyByLimit(long gwId, long serviceId, long projectId, long offset,
	                                                          long limit) {
		String sql;
		Map<String, Object> params = new HashMap<String, Object>();
		if (NumberUtils.INTEGER_ZERO != gwId) {
			sql = "select * from apigw_envoy_service_proxy where gw_id=:gwId and project_id=:projectId  order by id "
			      + "desc limit :limit offset :offset";
			if (NumberUtils.INTEGER_ZERO != serviceId) {
				sql = "select * from apigw_envoy_service_proxy where service_id=:serviceId and gw_id=:gwId and "
				      + "project_id=:projectId  order by id desc limit :limit offset :offset";
				params.put("serviceId", serviceId);
			}
			params.put("gwId", gwId);
		} else {
			sql = "select * from apigw_envoy_service_proxy where project_id=:projectId order by id desc limit :limit "
			      + "offset :offset";
			if (NumberUtils.INTEGER_ZERO != serviceId) {
				sql = "select * from apigw_envoy_service_proxy where service_id=:serviceId and project_id=:projectId "
				      + "order by id desc limit :limit offset :offset";
				params.put("serviceId", serviceId);
			}
		}
		params.put("projectId", projectId);
		params.put("offset", offset);
		params.put("limit", limit);
		return namedParameterJdbcTemplate.query(sql, params, new EnvoyServiceProxyRowMapper());
	}

	@Override
	public List<EnvoyServiceProxyInfo> batchGetServiceProxyList(long gwId, List<Long> serviceIdList) {
		String sql = "select * from apigw_envoy_service_proxy where service_id in (:serviceIdList) and gw_id=:gwId";
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("serviceIdList", serviceIdList);
		params.put("gwId", gwId);
		return namedParameterJdbcTemplate.query(sql, params, new EnvoyServiceProxyRowMapper());
	}

	class EnvoyServiceProxyRowMapper implements RowMapper<EnvoyServiceProxyInfo> {

		@Override
		public EnvoyServiceProxyInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			EnvoyServiceProxyInfo envoyServiceProxyInfo = new EnvoyServiceProxyInfo();
			envoyServiceProxyInfo.setId(rs.getLong("id"));
			envoyServiceProxyInfo.setCreateTime(rs.getLong("create_time"));
			envoyServiceProxyInfo.setUpdateTime(rs.getLong("update_time"));
			envoyServiceProxyInfo.setCode(rs.getString("code"));
			envoyServiceProxyInfo.setServiceId(rs.getLong("service_id"));
			envoyServiceProxyInfo.setBackendService(rs.getString("backend_service"));
			envoyServiceProxyInfo.setPublishType(rs.getString("publish_type"));
			envoyServiceProxyInfo.setGwId(rs.getLong("gw_id"));
			envoyServiceProxyInfo.setProjectId(rs.getLong("project_id"));
			envoyServiceProxyInfo.setPublishProtocol(rs.getString("publish_protocol"));
			envoyServiceProxyInfo.setLoadBalancer(rs.getString("load_balancer"));
			envoyServiceProxyInfo.setTrafficPolicy(rs.getString("traffic_policy"));
			envoyServiceProxyInfo.setSubsets(rs.getString("subsets"));
			envoyServiceProxyInfo.setRegistryCenterAddr(rs.getString("registry_center_addr"));
			envoyServiceProxyInfo.setRegistryCenterType(rs.getString("registry_center_type"));
			return envoyServiceProxyInfo;
		}

	}

}
