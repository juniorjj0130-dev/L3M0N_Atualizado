const assert = require('assert');
const { messageKeys } = require('../includes/const');

assert.ok(messageKeys.setText, 'Expected setText message key to be defined');
assert.strictEqual(messageKeys.setText, '0xST', 'Expected setText message key to be 0xST');
console.log('SetText message key registered:', messageKeys.setText);
