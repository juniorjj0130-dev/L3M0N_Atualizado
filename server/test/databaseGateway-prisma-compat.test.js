const test = require('node:test');
const assert = require('node:assert/strict');
const databaseGateway = require('../includes/databaseGateway');

test('maindb supports admin log writes through the compatibility layer', () => {
  databaseGateway.maindb.get('admin.logs').push({ message: 'prisma compat test' }).write();
  const logs = databaseGateway.maindb.get('admin.logs').value();

  assert.ok(Array.isArray(logs));
  assert.ok(logs.some((entry) => entry.message === 'prisma compat test'));
});

test('maindb supports client lookup by clientID', () => {
  const clientId = 'compat-client';
  databaseGateway.maindb.get('clients').push({ clientID: clientId, isOnline: true }).write();
  const client = databaseGateway.maindb.get('clients').find({ clientID: clientId }).value();

  assert.ok(client);
  assert.equal(client.clientID, clientId);
});
