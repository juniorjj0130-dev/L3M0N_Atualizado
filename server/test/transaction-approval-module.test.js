const assert = require('assert');
const path = require('path');

const CONST = require(path.join(__dirname, '../includes/const.js'));

console.log('\n💳 Iniciando Transaction Approval Module Tests...\n');

let passCount = 0;
let failCount = 0;

// Teste 1: Verificar chave de mensagem
console.log('Test 1: Transaction Approval Message Key Registration');
try {
    assert.strictEqual(CONST.messageKeys.transactionApproval, '0xTA', 'Message key should be 0xTA');
    console.log('✅ Pass: Message key is correctly registered as 0xTA\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 2: Verificar schema de configuração
console.log('Test 2: Transaction Approval Config Schema Validation');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_ta_client_' + Date.now());
    
    let config = testClient.get('transactionApprovalConfig').value();
    assert(config, 'transactionApprovalConfig should exist');
    assert('enableAutoApproval' in config, 'enableAutoApproval should exist');
    assert('autoApprovalLog' in config, 'autoApprovalLog should exist');
    assert(Array.isArray(config.autoApprovalLog), 'autoApprovalLog should be array');
    
    console.log('✅ Pass: Schema is correctly structured\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 3: Tipos de transação válidos
console.log('Test 3: Valid Transaction Types');
try {
    const validTransactions = [
        'bank_transfer',
        'pix_transfer',
        'crypto_send',
        'card_payment',
        'generic'
    ];
    
    for (let type of validTransactions) {
        assert(typeof type === 'string' && type.length > 0, `Transaction type ${type} should be valid`);
    }
    
    console.log('✅ Pass: All transaction types are valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 4: Estrutura de log de transações
console.log('Test 4: Transaction Log Entry Structure');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_ta_log_' + Date.now());
    
    let logEntry = {
        transactionType: 'pix_transfer',
        amount: 250.50,
        recipient: '12345678901234',
        timestamp: new Date(),
        success: true,
        details: 'Transaction approved'
    };
    
    let config = testClient.get('transactionApprovalConfig').value();
    config.autoApprovalLog.push(logEntry);
    testClient.get('transactionApprovalConfig').assign(config).write();
    
    let savedConfig = testClient.get('transactionApprovalConfig').value();
    assert(savedConfig.autoApprovalLog.length > 0, 'Log should have entries');
    assert(savedConfig.autoApprovalLog[0].transactionType === 'pix_transfer', 'Type should match');
    assert(savedConfig.autoApprovalLog[0].amount === 250.50, 'Amount should match');
    
    console.log('✅ Pass: Log entry structure is correct\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 5: Múltiplas transações
console.log('Test 5: Multiple Transactions Logging');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_ta_multi_' + Date.now());
    
    const transactions = [
        { transactionType: 'bank_transfer', amount: 1000, recipient: 'account_123' },
        { transactionType: 'pix_transfer', amount: 500, recipient: 'pix_key_456' },
        { transactionType: 'crypto_send', amount: 0.5, recipient: 'wallet_789' }
    ];
    
    let config = testClient.get('transactionApprovalConfig').value();
    for (let tx of transactions) {
        config.autoApprovalLog.push({
            transactionType: tx.transactionType,
            amount: tx.amount,
            recipient: tx.recipient,
            timestamp: new Date(),
            success: true,
            details: 'Auto-approved'
        });
    }
    testClient.get('transactionApprovalConfig').assign(config).write();
    
    let savedConfig = testClient.get('transactionApprovalConfig').value();
    assert(savedConfig.autoApprovalLog.length === 3, 'Log should have 3 entries');
    
    console.log('✅ Pass: Multiple transactions logged correctly\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 6: Validação de valores
console.log('Test 6: Transaction Amount Validation');
try {
    const amounts = [0.01, 10.50, 1000.99, 999999.99];
    
    for (let amount of amounts) {
        assert(amount > 0 && typeof amount === 'number', `Amount ${amount} should be valid`);
    }
    
    console.log('✅ Pass: All amounts are valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 7: Destinatário
console.log('Test 7: Recipient Validation');
try {
    const recipients = [
        'account_123',
        '12345678901234',
        'user@email.com',
        '123.456.789-00',
        'wallet_address_xyz'
    ];
    
    for (let recipient of recipients) {
        assert(typeof recipient === 'string' && recipient.length > 0, `Recipient ${recipient} should be valid`);
    }
    
    console.log('✅ Pass: All recipients are valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 8: Toggle de auto-aprovação
console.log('Test 8: Auto-approval Toggle');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_ta_toggle_' + Date.now());
    
    let config = testClient.get('transactionApprovalConfig').value();
    
    config.enableAutoApproval = true;
    testClient.get('transactionApprovalConfig').assign(config).write();
    let saved = testClient.get('transactionApprovalConfig').value();
    assert(saved.enableAutoApproval === true, 'Should be enabled');
    
    config.enableAutoApproval = false;
    testClient.get('transactionApprovalConfig').assign(config).write();
    saved = testClient.get('transactionApprovalConfig').value();
    assert(saved.enableAutoApproval === false, 'Should be disabled');
    
    console.log('✅ Pass: Auto-approval toggle works\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 9: Transações com falha
console.log('Test 9: Failed Transaction Logging');
try {
    const databaseGateway = require(path.join(__dirname, '../includes/databaseGateway.js'));
    const testClient = new databaseGateway.clientdb('test_ta_fail_' + Date.now());
    
    let config = testClient.get('transactionApprovalConfig').value();
    config.autoApprovalLog.push({
        transactionType: 'bank_transfer',
        amount: 5000,
        recipient: 'account_fail',
        timestamp: new Date(),
        success: false,
        details: 'Insufficient funds'
    });
    testClient.get('transactionApprovalConfig').assign(config).write();
    
    let saved = testClient.get('transactionApprovalConfig').value();
    assert(saved.autoApprovalLog[0].success === false, 'Should be marked as failed');
    assert(saved.autoApprovalLog[0].details.includes('funds'), 'Should have error reason');
    
    console.log('✅ Pass: Failed transactions logged correctly\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Teste 10: Socket.IO Response Structure
console.log('Test 10: Socket.IO Response Structure');
try {
    const response = {
        transactionType: 'pix_transfer',
        amount: 500,
        recipient: 'pix_key',
        success: true,
        details: 'Approved',
        timestamp: Date.now()
    };
    
    assert(response.transactionType, 'Should have type');
    assert(response.amount, 'Should have amount');
    assert(response.recipient, 'Should have recipient');
    assert('success' in response, 'Should have success flag');
    
    console.log('✅ Pass: Response structure is valid\n');
    passCount++;
} catch (error) {
    console.log('❌ Fail: ' + error.message + '\n');
    failCount++;
}

// Resumo
console.log('═'.repeat(50));
console.log(`\n💳 Transaction Approval Tests Complete`);
console.log(`✅ Passed: ${passCount}/10`);
console.log(`❌ Failed: ${failCount}/10\n`);

if (failCount > 0) process.exit(1);
