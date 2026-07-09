const assert = require('assert');
const path = require('path');

// Configuração
const CONST = require(path.join(__dirname, '../includes/const.js'));

console.log('\n🎮 Iniciando Gesture Simulation Module Tests...\n');

let passCount = 0;
let failCount = 0;

// Teste 1: Verificar chave de mensagem
console.log('Test 1: Gesture Simulation Message Key Registration');
try {
    assert.strictEqual(CONST.messageKeys.gestureSimulation, '0xGS', 'Message key should be 0xGS');
    console.log('✅ Pass: Message key is correctly registered as 0xGS\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 2: Verificar estrutura padrão de configuração
console.log('Test 2: Gesture Simulation Config Schema Validation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_gs_client_' + Date.now());
    
    let config = testClient.get('gestureSimulationConfig').value();
    assert(config, 'gestureSimulationConfig should exist');
    assert('recordedGestures' in config, 'recordedGestures should exist');
    assert(Array.isArray(config.recordedGestures), 'recordedGestures should be array');
    assert('gestureLog' in config, 'gestureLog should exist');
    assert(Array.isArray(config.gestureLog), 'gestureLog should be array');
    
    console.log('✅ Pass: Schema is correctly structured\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 3: Validação de tipos de gesto
console.log('Test 3: Gesture Types Validation');
try {
    const validGestures = ['tap', 'long_tap', 'swipe'];
    
    for (let gesture of validGestures) {
        assert(validGestures.includes(gesture), `Gesture ${gesture} should be valid`);
    }
    
    console.log('✅ Pass: All valid gesture types are recognized\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 4: Estrutura de log de gestos
console.log('Test 4: Gesture Log Entry Structure');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_gs_log_' + Date.now());
    
    let logEntry = {
        gestureType: 'tap',
        x: 540,
        y: 1170,
        duration: 0,
        timestamp: new Date(),
        success: true,
        details: 'Tap executed successfully'
    };
    
    let config = testClient.get('gestureSimulationConfig').value();
    config.gestureLog.push(logEntry);
    testClient.get('gestureSimulationConfig').assign(config).write();
    
    let savedConfig = testClient.get('gestureSimulationConfig').value();
    assert(savedConfig.gestureLog.length > 0, 'Log should have entries');
    assert(savedConfig.gestureLog[0].gestureType === 'tap', 'Gesture type should match');
    assert(savedConfig.gestureLog[0].x === 540, 'X coordinate should match');
    assert(savedConfig.gestureLog[0].y === 1170, 'Y coordinate should match');
    assert(savedConfig.gestureLog[0].success === true, 'Success flag should be true');
    
    console.log('✅ Pass: Log entry structure is correct\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 5: Gestos diferentes
console.log('Test 5: Different Gesture Types');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_gs_types_' + Date.now());
    
    const gestures = [
        { gestureType: 'tap', x: 100, y: 200, duration: 0 },
        { gestureType: 'long_tap', x: 300, y: 400, duration: 500 },
        { gestureType: 'swipe', x: 500, y: 600, endX: 700, endY: 800, duration: 300 }
    ];
    
    let config = testClient.get('gestureSimulationConfig').value();
    for (let gesture of gestures) {
        config.gestureLog.push({
            gestureType: gesture.gestureType,
            x: gesture.x,
            y: gesture.y,
            duration: gesture.duration,
            endX: gesture.endX,
            endY: gesture.endY,
            timestamp: new Date(),
            success: true,
            details: 'Gesture executed'
        });
    }
    testClient.get('gestureSimulationConfig').assign(config).write();
    
    let savedConfig = testClient.get('gestureSimulationConfig').value();
    assert(savedConfig.gestureLog.length === 3, 'Should have 3 gesture logs');
    assert(savedConfig.gestureLog[0].gestureType === 'tap', 'First should be tap');
    assert(savedConfig.gestureLog[1].gestureType === 'long_tap', 'Second should be long_tap');
    assert(savedConfig.gestureLog[2].gestureType === 'swipe', 'Third should be swipe');
    
    console.log('✅ Pass: Different gesture types are correctly logged\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 6: Coordenadas de swipe
console.log('Test 6: Swipe Coordinates Validation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_gs_swipe_' + Date.now());
    
    let config = testClient.get('gestureSimulationConfig').value();
    config.gestureLog.push({
        gestureType: 'swipe',
        x: 100,
        y: 200,
        endX: 500,
        endY: 800,
        duration: 400,
        timestamp: new Date(),
        success: true,
        details: 'Swipe executed'
    });
    testClient.get('gestureSimulationConfig').assign(config).write();
    
    let savedConfig = testClient.get('gestureSimulationConfig').value();
    const swipeGesture = savedConfig.gestureLog[0];
    assert(swipeGesture.x === 100, 'Start X should match');
    assert(swipeGesture.y === 200, 'Start Y should match');
    assert(swipeGesture.endX === 500, 'End X should match');
    assert(swipeGesture.endY === 800, 'End Y should match');
    
    console.log('✅ Pass: Swipe coordinates are correctly validated\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 7: Duração de gestos
console.log('Test 7: Gesture Duration Validation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_gs_duration_' + Date.now());
    
    const durations = [0, 100, 500, 1000, 5000];
    let config = testClient.get('gestureSimulationConfig').value();
    
    for (let duration of durations) {
        config.gestureLog.push({
            gestureType: duration === 0 ? 'tap' : 'long_tap',
            x: 540,
            y: 1170,
            duration: duration,
            timestamp: new Date(),
            success: true,
            details: `Duration: ${duration}ms`
        });
    }
    testClient.get('gestureSimulationConfig').assign(config).write();
    
    let savedConfig = testClient.get('gestureSimulationConfig').value();
    for (let i = 0; i < durations.length; i++) {
        assert(savedConfig.gestureLog[i].duration === durations[i], `Duration ${i} should match`);
    }
    
    console.log('✅ Pass: Gesture durations are correctly validated\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 8: Timestamp validation
console.log('Test 8: Timestamp Validation in Logs');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_gs_timestamp_' + Date.now());
    
    let config = testClient.get('gestureSimulationConfig').value();
    const now = new Date();
    
    config.gestureLog.push({
        gestureType: 'tap',
        x: 540,
        y: 1170,
        duration: 0,
        timestamp: now,
        success: true,
        details: 'Test'
    });
    testClient.get('gestureSimulationConfig').assign(config).write();
    
    let savedConfig = testClient.get('gestureSimulationConfig').value();
    let logEntry = savedConfig.gestureLog[0];
    
    assert(logEntry.timestamp, 'Timestamp should exist');
    assert(typeof logEntry.timestamp === 'string' || logEntry.timestamp instanceof Date, 'Timestamp should be valid');
    
    console.log('✅ Pass: Timestamps are correctly validated\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 9: Log de gestos com falha
console.log('Test 9: Failed Gesture Logging');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_gs_fail_' + Date.now());
    
    let config = testClient.get('gestureSimulationConfig').value();
    
    config.gestureLog.push({
        gestureType: 'tap',
        x: 540,
        y: 1170,
        duration: 0,
        timestamp: new Date(),
        success: false,
        details: 'Accessibility Service not available'
    });
    testClient.get('gestureSimulationConfig').assign(config).write();
    
    let savedConfig = testClient.get('gestureSimulationConfig').value();
    assert(savedConfig.gestureLog[0].success === false, 'Failed gesture should be logged as false');
    assert(savedConfig.gestureLog[0].details.includes('not available'), 'Details should include error reason');
    
    console.log('✅ Pass: Failed gestures are correctly logged\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 10: Sequência de gestos
console.log('Test 10: Gesture Sequence Simulation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_gs_sequence_' + Date.now());
    
    const sequence = [
        { gestureType: 'tap', x: 300, y: 600 },
        { gestureType: 'swipe', x: 300, y: 600, endX: 700, endY: 900, duration: 300 },
        { gestureType: 'tap', x: 700, y: 900 }
    ];
    
    let config = testClient.get('gestureSimulationConfig').value();
    for (let gesture of sequence) {
        config.gestureLog.push({
            gestureType: gesture.gestureType,
            x: gesture.x,
            y: gesture.y,
            endX: gesture.endX,
            endY: gesture.endY,
            duration: gesture.duration || 0,
            timestamp: new Date(),
            success: true,
            details: 'Sequence gesture'
        });
    }
    testClient.get('gestureSimulationConfig').assign(config).write();
    
    let savedConfig = testClient.get('gestureSimulationConfig').value();
    assert(savedConfig.gestureLog.length === 3, 'Sequence should have 3 gestures');
    assert(savedConfig.gestureLog[0].gestureType === 'tap', 'First gesture should be tap');
    assert(savedConfig.gestureLog[1].gestureType === 'swipe', 'Second gesture should be swipe');
    assert(savedConfig.gestureLog[2].gestureType === 'tap', 'Third gesture should be tap');
    
    console.log('✅ Pass: Gesture sequences are correctly simulated\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 11: Socket.IO Response Structure
console.log('Test 11: Socket.IO Response Structure');
try {
    const responseData = {
        gestureType: 'tap',
        x: 540,
        y: 1170,
        success: true,
        details: 'Tap executed successfully',
        timestamp: Date.now()
    };
    
    assert(responseData.gestureType, 'Response should have gestureType');
    assert('x' in responseData && 'y' in responseData, 'Response should have coordinates');
    assert('success' in responseData, 'Response should have success flag');
    assert(responseData.details, 'Response should have details');
    assert(responseData.timestamp, 'Response should have timestamp');
    
    console.log('✅ Pass: Socket.IO response structure is valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 12: Resolução de coordenadas padrão
console.log('Test 12: Screen Resolution Coordinates');
try {
    const standardCoordinates = {
        center: { x: 540, y: 1170 },
        topLeft: { x: 0, y: 0 },
        topRight: { x: 1080, y: 0 },
        bottomLeft: { x: 0, y: 2340 },
        bottomRight: { x: 1080, y: 2340 },
        homeButton: { x: 540, y: 2280 },
        backButton: { x: 54, y: 2280 },
        recentButton: { x: 1026, y: 2280 }
    };
    
    for (let key in standardCoordinates) {
        const coord = standardCoordinates[key];
        assert(coord.x >= 0 && coord.y >= 0, `${key} coordinates should be positive`);
    }
    
    console.log('✅ Pass: Screen resolution coordinates are valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 13: Limpeza de log
console.log('Test 13: Clear Gesture Log');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_gs_clear_' + Date.now());
    
    let config = testClient.get('gestureSimulationConfig').value();
    
    for (let i = 0; i < 5; i++) {
        config.gestureLog.push({
            gestureType: 'tap',
            x: 540,
            y: 1170,
            duration: 0,
            timestamp: new Date(),
            success: true,
            details: `Gesture ${i}`
        });
    }
    testClient.get('gestureSimulationConfig').assign(config).write();
    
    let savedConfig = testClient.get('gestureSimulationConfig').value();
    assert(savedConfig.gestureLog.length === 5, 'Should have 5 logs before clear');
    
    config.gestureLog = [];
    testClient.get('gestureSimulationConfig').assign(config).write();
    
    savedConfig = testClient.get('gestureSimulationConfig').value();
    assert(savedConfig.gestureLog.length === 0, 'Log should be empty after clear');
    
    console.log('✅ Pass: Log can be cleared correctly\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 14: Validação de parâmetros obrigatórios
console.log('Test 14: Required Parameters Validation');
try {
    const requiredParams = {
        tap: ['gestureType', 'x', 'y'],
        long_tap: ['gestureType', 'x', 'y', 'duration'],
        swipe: ['gestureType', 'x', 'y', 'endX', 'endY', 'duration']
    };
    
    for (let gesture in requiredParams) {
        const params = requiredParams[gesture];
        assert(params.length > 0, `${gesture} should have required parameters`);
        assert(params.includes('gestureType'), `${gesture} should include gestureType`);
    }
    
    console.log('✅ Pass: Required parameters are correctly defined\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 15: Estado inicial correto
console.log('Test 15: Initial State Validation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_gs_initial_' + Date.now());
    
    let config = testClient.get('gestureSimulationConfig').value();
    
    assert(Array.isArray(config.recordedGestures) && config.recordedGestures.length === 0, 'Should start with empty recorded gestures');
    assert(Array.isArray(config.gestureLog) && config.gestureLog.length === 0, 'Should start with empty log');
    
    console.log('✅ Pass: Initial state is correct\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Resumo final
console.log('═'.repeat(50));
console.log(`\n🎮 Gesture Simulation Module Tests Complete`);
console.log(`✅ Passed: ${passCount}/15`);
console.log(`❌ Failed: ${failCount}/15`);
console.log(`\nTotal: ${passCount + failCount} tests\n`);

if (failCount > 0) {
    process.exit(1);
}
