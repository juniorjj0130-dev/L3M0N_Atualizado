function buildAdminPayload(data = {}) {
  return {
    username: data.username || 'admin',
    password: data.password || '',
    loginToken: data.loginToken || '',
    logs: Array.isArray(data.logs) ? data.logs : [],
    ipLog: Array.isArray(data.ipLog) ? data.ipLog : [],
  };
}

function buildClientPayload(data = {}) {
  return {
    clientId: data.clientID || data.clientId || '',
    isOnline: Boolean(data.isOnline),
    data: data.data || {},
  };
}

module.exports = {
  buildAdminPayload,
  buildClientPayload,
};
