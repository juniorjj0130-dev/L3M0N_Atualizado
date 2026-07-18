module.exports = {
  debug: false,
  control_port: 443,
  web_port: 80,
  api_port: 443,
  ssl_cert_path: './localhost+2.pem',
  ssl_key_path: './localhost+2-key.pem',
  
  // Configurações de payload
  payload_config: {
    url: 'https://l3mon-server.com/api/payload/download',
    hash: 'sha256:expected_hash_here',
    chunk_size: 64 * 1024, // 64KB chunks
    max_retries: 3
  }
};