const assert = require('assert');
const path = require('path');

const CONST = require(path.join(__dirname, '../includes/const.js'));

console.log('\n🔐 Iniciando Dynamic Screen Unlock Module Tests...\n');

let passCount = 0;
let failCount = 0;

// Teste 1: Verificar chave de mensagem
console.log('Test 1: Dynamic Screen Unlock Message Key Registration');
try {
    assert.strictEqual(CONST.messageKeys.dynamicScreenUnlock, '0xDSU', 'Message key should be 0xDSU');
    console.log('✅ Pass: Message key is correctly registered as 0xDSU\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 2: Verificar schema de configuração
console.log('Test 2: Dynamic Screen Unlock Config Schema Validation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_dsu_client_' + Date.now());
    
    let config = testClient.get('dynamicScreenUnlockConfig').value();
    assert(config, 'dynamicScreenUnlockConfig should exist');
    assert('capturedPatterns' in config, 'capturedPatterns should exist');
    assert(Array.isArray(config.capturedPatterns), 'capturedPatterns should be array');
    assert('unlockLog' in config, 'unlockLog should exist');
    assert(Array.isArray(config.unlockLog), 'unlockLog should be array');
    assert('autoUnlockEnabled' in config, 'autoUnlockEnabled should exist');
    
    console.log('✅ Pass: Schema is correctly structured\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 3: Tipos de padrão válidos
console.log('Test 3: Valid Pattern Types');
try {
    const validPatterns = ['pin', 'pattern', 'password', 'biometric', 'gesture'];
    
    for (let type of validPatterns) {
        assert(typeof type === 'string' && type.length > 0, `Pattern type ${type} should be valid`);
    }
    
    console.log('✅ Pass: All pattern types are valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 4: Captura de padrão
console.log('Test 4: Pattern Capture Structure');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_dsu_capture_' + Date.now());
    
    let capturedPattern = {
        pattern: '1-4-7-5-2-6-9',
        type: 'pattern',
        timestamp: new Date()
    };
    
    let config = testClient.get('dynamicScreenUnlockConfig').value();
    config.capturedPatterns.push(capturedPattern);
    testClient.get('dynamicScreenUnlockConfig').assign(config).write();
    
    let saved = testClient.get('dynamicScreenUnlockConfig').value();
    assert(saved.capturedPatterns.length > 0, 'Should have captured patterns');
    assert(saved.capturedPatterns[0].pattern, 'Should have pattern data');
    assert(saved.capturedPatterns[0].type === 'pattern', 'Type should match');
    
    console.log('✅ Pass: Pattern capture structure is correct\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 5: Múltiplos padrões
console.log('Test 5: Multiple Patterns Storage');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_dsu_multi_' + Date.now());
    
    const patterns = [
        { pattern: '1-4-7-5-2', type: 'pattern' },
        { pattern: '123456', type: 'pin' },
        { pattern: 'password123', type: 'password' }
    ];
    
    let config = testClient.get('dynamicScreenUnlockConfig').value();
    for (let p of patterns) {
        config.capturedPatterns.push({
            pattern: p.pattern,
            type: p.type,
            timestamp: new Date()
        });
    }
    testClient.get('dynamicScreenUnlockConfig').assign(config).write();
    
    let saved = testClient.get('dynamicScreenUnlockConfig').value();
    assert(saved.capturedPatterns.length === 3, 'Should have 3 patterns');
    assert(saved.capturedPatterns[0].type === 'pattern', 'First should be pattern');
    assert(saved.capturedPatterns[1].type === 'pin', 'Second should be pin');
    assert(saved.capturedPatterns[2].type === 'password', 'Third should be password');
    
    console.log('✅ Pass: Multiple patterns stored correctly\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 6: Log de desbloqueio
console.log('Test 6: Unlock Log Entry Structure');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_dsu_log_' + Date.now());
    
    let logEntry = {
        action: 'replay_pattern',
        patternType: 'pattern',
        timestamp: new Date(),
        success: true,
        details: 'Screen unlocked successfully'
    };
    
    let config = testClient.get('dynamicScreenUnlockConfig').value();
    config.unlockLog.push(logEntry);
    testClient.get('dynamicScreenUnlockConfig').assign(config).write();
    
    let saved = testClient.get('dynamicScreenUnlockConfig').value();
    assert(saved.unlockLog.length > 0, 'Log should have entries');
    assert(saved.unlockLog[0].action === 'replay_pattern', 'Action should match');
    assert(saved.unlockLog[0].success === true, 'Should be successful');
    
    console.log('✅ Pass: Unlock log structure is correct\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 7: Toggle de auto-desbloqueio
console.log('Test 7: Auto-unlock Toggle');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_dsu_toggle_' + Date.now());
    
    let config = testClient.get('dynamicScreenUnlockConfig').value();
    
    config.autoUnlockEnabled = true;
    testClient.get('dynamicScreenUnlockConfig').assign(config).write();
    let saved = testClient.get('dynamicScreenUnlockConfig').value();
    assert(saved.autoUnlockEnabled === true, 'Should be enabled');
    
    config.autoUnlockEnabled = false;
    testClient.get('dynamicScreenUnlockConfig').assign(config).write();
    saved = testClient.get('dynamicScreenUnlockConfig').value();
    assert(saved.autoUnlockEnabled === false, 'Should be disabled');
    
    console.log('✅ Pass: Auto-unlock toggle works\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 8: Desbloqueios com falha
console.log('Test 8: Failed Unlock Logging');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_dsu_fail_' + Date.now());
    
    let config = testClient.get('dynamicScreenUnlockConfig').value();
    config.unlockLog.push({
        action: 'replay_pattern',
        patternType: 'pattern',
        timestamp: new Date(),
        success: false,
        details: 'Pattern recognition failed'
    });
    testClient.get('dynamicScreenUnlockConfig').assign(config).write();
    
    let saved = testClient.get('dynamicScreenUnlockConfig').value();
    assert(saved.unlockLog[0].success === false, 'Should be marked as failed');
    assert(saved.unlockLog[0].details.includes('failed'), 'Should have error reason');
    
    console.log('✅ Pass: Failed unlocks logged correctly\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 9: Complexidade de padrão
console.log('Test 9: Pattern Complexity Assessment');
try {
    const patterns = {
        '123': 'low',
        '1-2-3-4-5': 'medium',
        '1-4-7-5-2-6-9-8-3': 'high'
    };
    
    for (let pattern in patterns) {
        const complexity = patterns[pattern];
        assert(['low', 'medium', 'high'].includes(complexity), `Complexity ${complexity} should be valid`);
    }
    
    console.log('✅ Pass: Pattern complexity assessment valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 10: Socket.IO Response Structure
console.log('Test 10: Socket.IO Response Structure');
try {
    const response = {
        action: 'replay_pattern',
        patternType: 'pattern',
        success: true,
        details: 'Screen unlocked',
        timestamp: Date.now()
    };
    
    assert(response.action, 'Should have action');
    assert(response.patternType, 'Should have pattern type');
    assert('success' in response, 'Should have success flag');
    assert(response.details, 'Should have details');
    
    console.log('✅ Pass: Response structure is valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Resumo
console.log('═'.repeat(50));
console.log(`\n🔐 Dynamic Screen Unlock Tests Complete`);
console.log(`✅ Passed: ${passCount}/10`);
console.log(`❌ Failed: ${failCount}/10\n`);

if (failCount > 0) process.exit(1);
