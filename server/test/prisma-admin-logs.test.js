const test = require('node:test');
const assert = require('node:assert/strict');
const logManager = require('../includes/logManager');
const db = require('../includes/databaseGateway');

test('prisma admin logs writes and reads through the Prisma-backed log manager', async () => {
  const type = { name: 'info' };
  const message = 'prisma-admin-log-test';

  await logManager.log(type, message);
  const logs = await logManager.getLogs();

  assert.ok(Array.isArray(logs));
  assert.ok(logs.some((entry) => entry.message === message && entry.type === type.name));

  if (db.usePrisma) {
    const admin = await db.prisma.admin.findFirst();
    assert.ok(admin);
    assert.ok(Array.isArray(admin.logs));
  }
});
