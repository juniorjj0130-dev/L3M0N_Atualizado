#!/usr/bin/env node

/**
 * Test Runner - Executa todos os testes do projeto
 */

const fs = require('fs');
const path = require('path');

const testDir = path.join(__dirname, 'test');
const testFiles = fs.readdirSync(testDir).filter(f => f.endsWith('.test.js'));
const testDataDir = path.join(__dirname, '.test-data');

fs.rmSync(testDataDir, { recursive: true, force: true });
fs.mkdirSync(testDataDir, { recursive: true });

process.env.L3MON_MAINDB_PATH = path.join(testDataDir, 'maindb.json');
process.env.L3MON_DATA_DIR = path.join(testDataDir, 'clientData');

console.log('\n========================================');
console.log('    L3MON - Test Suite Runner');
console.log('========================================\n');

let totalTests = 0;
let failedTests = 0;

testFiles.forEach(file => {
    console.log(`\n📋 Running ${file}...`);
    console.log('─'.repeat(50));
    
    try {
        require(path.join(testDir, file));
        console.log('─'.repeat(50));
    } catch (error) {
        console.error(`❌ Test failed: ${error.message}`);
        failedTests++;
        console.log('─'.repeat(50));
    }
});

console.log('\n========================================');
if (failedTests === 0) {
    console.log('✅ All tests passed!');
} else {
    console.log(`❌ ${failedTests} test file(s) failed`);
    process.exit(1);
}
console.log('========================================\n');
