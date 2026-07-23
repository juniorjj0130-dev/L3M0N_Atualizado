const test = require('node:test');
const assert = require('node:assert/strict');
const Clients = require('../includes/clientManager');

test('persistClientDataEntry uses the Prisma helper when enabled', async () => {
  const calls = [];
  const dbStub = {
    usePrisma: true,
    upsertClientData: async (clientId, kind, payload) => {
      calls.push({ clientId, kind, payload });
      return { clientId, kind, payload };
    },
    maindb: {
      get: () => ({
        find: () => ({ value: () => undefined }),
        push: () => ({ write: () => {} }),
      }),
    },
    clientdb: class FakeClientDB {},
  };

  const manager = new Clients(dbStub);
  await manager.persistClientDataEntry('client-1', 'sms', {
    address: '123456',
    body: 'hello',
  });

  assert.deepEqual(calls, [
    {
      clientId: 'client-1',
      kind: 'sms',
      payload: { address: '123456', body: 'hello' },
    },
  ]);
});
