const test = require('node:test');
const assert = require('node:assert/strict');
const { buildAdminPayload, buildClientPayload } = require('../includes/databaseMigration');

test('buildAdminPayload serializes logs and ipLog arrays', () => {
  const payload = buildAdminPayload({
    username: 'admin',
    password: 'secret',
    loginToken: 'token',
    logs: [{ time: 'now', type: 'info' }],
    ipLog: [{ ip: '127.0.0.1' }],
  });

  assert.equal(payload.username, 'admin');
  assert.equal(payload.password, 'secret');
  assert.equal(payload.loginToken, 'token');
  assert.deepEqual(payload.logs, [{ time: 'now', type: 'info' }]);
  assert.deepEqual(payload.ipLog, [{ ip: '127.0.0.1' }]);
});

test('buildClientPayload preserves client metadata and JSON state', () => {
  const payload = buildClientPayload({
    clientID: 'abc123',
    isOnline: true,
    data: {
      SMSData: [{ address: '123' }],
      CallData: [{ phoneNo: '456' }],
    },
  });

  assert.equal(payload.clientId, 'abc123');
  assert.equal(payload.isOnline, true);
  assert.deepEqual(payload.data.SMSData, [{ address: '123' }]);
  assert.deepEqual(payload.data.CallData, [{ phoneNo: '456' }]);
});
