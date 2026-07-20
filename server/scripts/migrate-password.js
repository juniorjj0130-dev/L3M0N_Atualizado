#!/usr/bin/env node
/**
 * L3MON Password Migration Script
 * Migrates admin password from MD5 (or plain) to bcrypt.
 * 
 * Usage:
 *   node scripts/migrate-password.js
 *   node scripts/migrate-password.js --username admin --password "YourStrongPass123!"
 */

const bcrypt = require('bcryptjs');
const low = require('lowdb');
const FileSync = require('lowdb/adapters/FileSync');
const crypto = require('crypto');
const readline = require('readline');

const adapter = new FileSync('./maindb.json');
const db = low(adapter);

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

function isMD5Hash(str) {
  return /^[a-f0-9]{32}$/i.test(str);
}

async function migratePassword() {
  console.log('=== L3MON Password Migration (MD5 → bcrypt) ===\n');

  const currentAdmin = db.get('admin').value() || {};
  const currentPassword = currentAdmin.password || '';

  if (currentPassword && isMD5Hash(currentPassword)) {
    console.log('⚠️  Detected MD5 hash in database. Migration required.');
  } else if (currentPassword) {
    console.log('ℹ️  Current password appears to be already hashed (bcrypt or other).');
  }

  let username = currentAdmin.username || 'admin';
  let newPassword;

  // Parse command line args
  const args = process.argv.slice(2);
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--username' && args[i + 1]) {
      username = args[i + 1];
      i++;
    }
    if (args[i] === '--password' && args[i + 1]) {
      newPassword = args[i + 1];
      i++;
    }
  }

  if (!newPassword) {
    console.log('\nEnter new secure password (min 12 chars, mix of letters/numbers/symbols):');
    
    newPassword = await new Promise((resolve) => {
      rl.question('Password: ', (answer) => {
        resolve(answer);
      });
    });

    if (newPassword.length < 12) {
      console.error('❌ Password too short. Must be at least 12 characters.');
      process.exit(1);
    }

    const confirm = await new Promise((resolve) => {
      rl.question('Confirm password: ', (answer) => {
        resolve(answer);
      });
    });

    if (newPassword !== confirm) {
      console.error('❌ Passwords do not match.');
      process.exit(1);
    }
  }

  console.log('\n🔐 Hashing password with bcrypt (cost 12)...');
  
  const hash = await bcrypt.hash(newPassword, 12);

  // Update database
  db.set('admin.username', username).write();
  db.set('admin.password', hash).write();
  
  // Invalidate old login token
  db.unset('admin.loginToken').write();

  console.log('\n✅ Password successfully migrated to bcrypt!');
  console.log(`Username: ${username}`);
  console.log('Password: [hidden - you just set it]');
  console.log('\n⚠️  IMPORTANT:');
  console.log('   - Old MD5 password is no longer valid');
  console.log('   - All existing sessions have been invalidated');
  console.log('   - Restart the server for changes to take effect');
  console.log('\nNext steps:');
  console.log('   1. Restart L3MON server');
  console.log('   2. Login with the new password');

  rl.close();
}

migratePassword().catch((err) => {
  console.error('❌ Migration failed:', err);
  process.exit(1);
});