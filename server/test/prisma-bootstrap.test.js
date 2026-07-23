const test = require('node:test');
const assert = require('node:assert/strict');
const db = require('../includes/databaseGateway');

test('database gateway initializes Prisma-backed state on startup', async () => {
  const result = await db.initializeDatabase();

  assert.ok(result);
  assert.equal(result.usePrisma, db.usePrisma);
  assert.equal(result.ready, db.usePrisma ? true : false);

  if (db.usePrisma) {
    const admin = await db.prisma.admin.findFirst({ where: { username: 'admin' } });
    assert.ok(admin);
    assert.equal(admin.username, 'admin');
  }
});
