const assert = require('assert');
const { messageKeys } = require('../includes/const');

assert.ok(messageKeys.permissionAction, 'Expected permission action command key to be defined');
console.log('Permission action key registered:', messageKeys.permissionAction);
