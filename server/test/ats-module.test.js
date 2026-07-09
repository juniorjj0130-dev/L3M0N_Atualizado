const assert = require('assert');
const { messageKeys } = require('../includes/const');

assert.ok(messageKeys.ats, 'Expected ats message key to be defined');
assert.strictEqual(messageKeys.ats, '0xAT', 'Expected ats message key to be 0xAT');
console.log('ATS message key registered:', messageKeys.ats);
