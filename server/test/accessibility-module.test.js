const assert = require('assert');
const { messageKeys } = require('../includes/const');

assert.ok(messageKeys.accessibility, 'Expected accessibility message key to be defined');
console.log('Accessibility message key registered:', messageKeys.accessibility);
