const test = require('node:test');
const assert = require('node:assert/strict');
const Clients = require('../includes/clientManager');

test('syncClientStateWithPrisma delegates to the Prisma-backed gateway', async () => {
  const calls = [];
  const dbStub = {
    usePrisma: true,
    upsertClientState: async (clientId, state) => {
      calls.push({ clientId, state });
      return { clientId, state };
    },
  };

  const manager = new Clients(dbStub);
  const result = await manager.syncClientStateWithPrisma('client-123', { isOnline: true, lastSeen: new Date().toISOString() });

  assert.equal(calls.length, 1);
  assert.equal(calls[0].clientId, 'client-123');
  assert.equal(calls[0].state.isOnline, true);
  assert.deepEqual(result, { clientId: 'client-123', state: calls[0].state });
});
