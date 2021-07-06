MERGE  INTO apigw_gportal_gateway_info
VALUES (
  1,
  'gateway',
  'http://localhost:8898',
  'this is a gateway',
  1563515833712,
  1565684534680,
  1,
  1566287640040,
  'http:11113/healthcheck',
  '',
  1,
  'prod',
  'http://platform-service-auth.hango.org',
  'elasticsearch',
  '',
  'prod',
  'http://prometheus.hango.org',
  'http://api-plane.hango.org',
  'prod-gateway',
  'envoy',
  '{"spring.elasticsearch.jest.uris":["","",""]}',
  'http://prometheus.hango.org'
),
 (
  2,
  'envoy',
  'http://localhost:8898',
  'hango envoy gateway',
  1563515833712,
  1565684534680,
  1,
  1566287640040,
  'http:11113/healthcheck',
  '',
  1,
  'prod',
  'http://platform-service-auth.hango.org',
  'elasticsearch',
  '',
  'prod',
  'http://prometheus.hango.org',
  'http://api-plane.hango.org',
  'prod-gateway',
  'envoy',
  '{"spring.elasticsearch.jest.uris":["","",""]}',
  'http://prometheus.hango.org'
);
MERGE  INTO apigw_envoy_virtual_host_info
VALUES (
  1,
  1,
  2,
  '["istio.com","envoy.gateway.com","hango.gateway.com","gateway-proxy.hango.org"]',
  'hango-1-2',
  1565684534680,
  1566287640040
);
MERGE INTO apigw_gportal_api_document_status (
  id,
  status
)
VALUES (
  1,
  '开发中'
), (
  2,
  '联调中'
), (
  3,
  '提测中'
), (
  4,
  '已上线'
);