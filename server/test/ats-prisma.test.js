const test = require('node:test');
const assert = require('node:assert/strict');
const ATSManager = require('../includes/atsManager');

test('ATS manager ensures client records through the Prisma-backed gateway when enabled', async () => {
  const calls = [];
  const dbStub = {
    usePrisma: true,
    upsertClientState: async (clientId, state) => {
      calls.push({ clientId, state });
      return { clientId, state };
    },
    maindb: {
      get: () => ({
        find: () => ({ value: () => undefined }),
        push: () => ({ write: () => {} }),
      }),
    },
    clientdb: class FakeClientDB {
      constructor() {}
      get() {
        return {
          value: () => ({ enabled: false, dataMapping: {}, fieldPatterns: [], buttonPatterns: [], atsLog: [] }),
          assign: () => ({ write: () => {} }),
          push: () => ({ write: () => {} }),
        };
      }
    },
  };

  const manager = new ATSManager(dbStub);
  await manager.ensureClientRecord('ats-client');

  assert.deepEqual(calls, [
    {
      clientId: 'ats-client',
      state: {
        clientID: 'ats-client',
        firstSeen: calls[0]?.state?.firstSeen,
        lastSeen: calls[0]?.state?.lastSeen,
        isOnline: false,
        dynamicData: {},
      },
    },
  ]);
});
