const test = require('node:test');
const assert = require('node:assert/strict');
const db = require('../includes/databaseGateway');
const logManager = require('../includes/logManager');

async function cleanup() {
  await db.prisma.admin.deleteMany();
  await db.prisma.client.deleteMany();
}

test('prisma admin logging writes and reads from PostgreSQL', async () => {
  await cleanup();
  await logManager.log({ name: 'info' }, 'prisma flow test');
  const logs = await logManager.getLogs();
  assert.ok(Array.isArray(logs));
  assert.ok(logs.some((entry) => entry.message === 'prisma flow test'));
});

test('prisma client records can be created and read', async () => {
  await cleanup();
  await db.createClientRecord({ clientId: 'runtime-client', isOnline: true, data: { foo: 'bar' } });
  const client = await db.prisma.client.findUnique({ where: { clientId: 'runtime-client' } });
  assert.ok(client);
  assert.equal(client.clientId, 'runtime-client');
  assert.equal(client.data.foo, 'bar');
});
