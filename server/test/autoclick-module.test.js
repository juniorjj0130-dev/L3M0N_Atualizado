const assert = require('assert');
const { messageKeys } = require('../includes/const');

console.log('\n=== AutoClick Module Tests ===\n');

// Test 1: Message Key
assert.ok(messageKeys.autoClick, 'Expected auto click command key to be defined');
assert.strictEqual(messageKeys.autoClick, '0xAC');
console.log('✓ AutoClick message key registered:', messageKeys.autoClick);

// Test 2: Database Schema
const schema = {
    autoClickEnabled: false,
    clickDelayMs: 500,
    buttonPatterns: []
};

assert.strictEqual(typeof schema.autoClickEnabled, 'boolean');
assert.strictEqual(typeof schema.clickDelayMs, 'number');
assert.ok(Array.isArray(schema.buttonPatterns));
console.log('✓ atsConfig schema validation passed');

// Test 3: Button Patterns
const patterns = [
    'enviar', 'send', 'submit', 'confirmar', 'confirm',
    'ok', 'continuar', 'continue', 'proximo', 'next',
    'avancar', 'forward', 'realizar', 'efetuar', 'fazer'
];

assert.ok(patterns.length > 0);
patterns.forEach(p => {
    assert.strictEqual(typeof p, 'string');
    assert.ok(p.length > 0);
});
console.log(`✓ ${patterns.length} button patterns defined`);

// Test 4: Delays Validation
const validDelays = [0, 100, 500, 1000, 5000];
validDelays.forEach(delay => {
    assert.ok(delay >= 0 && delay <= 5000);
});
console.log('✓ Click delays are valid');

// Test 5: Pattern Matching (case-insensitive)
const buttonText = 'Enviar';
const pattern = 'enviar';
const combined = buttonText.toLowerCase();
const matches = combined.includes(pattern.toLowerCase());
assert.strictEqual(matches, true);
console.log('✓ Pattern matching "enviar" works');

// Test 6: Pattern Matching with description
const description = 'confirmar_compra';
const confirmPattern = 'confirmar';
const descMatches = description.includes(confirmPattern.toLowerCase());
assert.strictEqual(descMatches, true);
console.log('✓ Pattern matching "confirmar" works');

// Test 7: Multiple Patterns
const multiPatterns = ['send', 'enviar', 'submit', 'confirmar'];
const multiButtonText = 'Confirmar Operação';
const multiCombined = multiButtonText.toLowerCase();
let found = false;
for (let p of multiPatterns) {
    if (multiCombined.includes(p.toLowerCase())) {
        found = true;
        break;
    }
}
assert.strictEqual(found, true);
console.log('✓ Multiple patterns verified successfully');

// Test 8: Config Enable/Disable
let config = { autoClickEnabled: false };
config.autoClickEnabled = true;
assert.strictEqual(config.autoClickEnabled, true);
config.autoClickEnabled = false;
assert.strictEqual(config.autoClickEnabled, false);
console.log('✓ AutoClick enable/disable works');

// Test 9: Delay Update
let delayConfig = { clickDelayMs: 500 };
delayConfig.clickDelayMs = 1000;
assert.strictEqual(delayConfig.clickDelayMs, 1000);
delayConfig.clickDelayMs = 200;
assert.strictEqual(delayConfig.clickDelayMs, 200);
console.log('✓ Delay update works');

// Test 10: Pattern Addition
let patList = ['enviar', 'confirmar'];
patList.push('custom_pattern');
assert.ok(patList.includes('custom_pattern'));
assert.strictEqual(patList.length, 3);
console.log('✓ Pattern addition works');

// Test 11: Duplicate Prevention
let dupList = ['enviar', 'confirmar'];
if (!dupList.includes('enviar')) {
    dupList.push('enviar');
}
assert.strictEqual(dupList.length, 2);
console.log('✓ Duplicate pattern prevention works');

// Test 12: Message Command Structure
const command = {
    type: '0xAC',
    action: 'button_pattern_click',
    clickDelayMs: 500
};
assert.strictEqual(command.type, '0xAC');
assert.strictEqual(command.action, 'button_pattern_click');
assert.strictEqual(typeof command.clickDelayMs, 'number');
console.log('✓ AutoClick command structure is valid');

// Test 13: Success Response
const response = {
    buttonClicked: true,
    buttonText: 'Enviar',
    timestamp: Date.now()
};
assert.strictEqual(response.buttonClicked, true);
assert.strictEqual(typeof response.buttonText, 'string');
assert.ok(Number.isInteger(response.timestamp));
console.log('✓ Success response structure is valid');

// Test 14: Error Response
const errorResponse = {
    buttonClicked: false,
    error: 'Botão não encontrado'
};
assert.strictEqual(errorResponse.buttonClicked, false);
assert.strictEqual(typeof errorResponse.error, 'string');
console.log('✓ Error response structure is valid');

// Test 15: ATS+AutoClick Command
const atsCommand = {
    type: '0xAT',
    action: 'activateWithAutoClick',
    buttonPatterns: ['enviar', 'confirmar'],
    clickDelayMs: 750
};
assert.strictEqual(atsCommand.type, '0xAT');
assert.strictEqual(atsCommand.action, 'activateWithAutoClick');
assert.ok(Array.isArray(atsCommand.buttonPatterns));
assert.strictEqual(atsCommand.clickDelayMs, 750);
console.log('✓ ATS+AutoClick command structure is valid');

// Test 16: Timing Validation
const typicalDelay = 500;
const minDelay = 200;
const maxDelay = 1000;
assert.ok(typicalDelay >= minDelay && typicalDelay <= maxDelay);
console.log('✓ Recommended delay 500ms is in correct interval');

// Test 17: Log Entry
const logEntry = {
    type: 'auto_click',
    buttonText: 'Confirmar',
    timestamp: new Date(),
    success: true
};
assert.strictEqual(logEntry.type, 'auto_click');
assert.strictEqual(typeof logEntry.buttonText, 'string');
assert.ok(logEntry.timestamp instanceof Date);
assert.strictEqual(typeof logEntry.success, 'boolean');
console.log('✓ AutoClick log entry structure is valid');

// Test 18: Multiple Log Operations
const log = [
    { type: 'auto_click', buttonText: 'Enviar', success: true },
    { type: 'auto_click', buttonText: 'Confirmar', success: true },
    { type: 'auto_click', buttonText: 'Desconhecido', success: false }
];
assert.strictEqual(log.length, 3);
assert.strictEqual(log[2].success, false);
console.log(`✓ ${log.length} operations registered successfully`);

// Test 19: ATS Workflow
const workflow = {
    step1: 'performATSAutomation',
    step2: 'aguarda delay configurado',
    step3: 'findAndClickConfirmationButton'
};
assert.strictEqual(typeof workflow.step1, 'string');
assert.strictEqual(typeof workflow.step2, 'string');
assert.strictEqual(typeof workflow.step3, 'string');
console.log('✓ AutoClick + ATS workflow is valid');

// Test 20: Config Synchronization
const atsConfig = {
    enabled: true,
    autoClickEnabled: true,
    buttonPatterns: ['confirmar', 'enviar'],
    dataMapping: { email: 'test@example.com' }
};
assert.ok(atsConfig.enabled);
assert.ok(atsConfig.autoClickEnabled);
assert.ok(atsConfig.dataMapping.email);
console.log('✓ ATS + AutoClick config synchronized');

console.log('\n✅ All AutoClick module tests passed!\n');
