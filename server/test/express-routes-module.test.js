const test = require('node:test');
const assert = require('node:assert/strict');

test('includes/expressRoutes should load as an Express router', () => {
  const router = require('../includes/expressRoutes');
  assert.ok(router, 'router should be defined');
  assert.equal(typeof router.use, 'function');
});
