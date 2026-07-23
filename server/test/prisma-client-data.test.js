const test = require('node:test');
const assert = require('node:assert/strict');
const db = require('../includes/databaseGateway');

async function cleanup(clientId) {
  await db.prisma.clientDataEntry.deleteMany({ where: { clientId } });
}

test('prisma stores and lists client data entries', async () => {
  const clientId = 'client-data-flow';
  await cleanup(clientId);

  await db.upsertClientData(clientId, 'sms', { address: '123456', body: 'hello' });
  await db.upsertClientData(clientId, 'call', { phoneNo: '654321', duration: 15 });

  const smsData = await db.listClientData(clientId, 'sms');
  const callData = await db.listClientData(clientId, 'call');

  assert.equal(smsData.length, 1);
  assert.equal(callData.length, 1);
  assert.equal(smsData[0].address, '123456');
  assert.equal(callData[0].phoneNo, '654321');
});
