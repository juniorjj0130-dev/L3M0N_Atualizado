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
            let client = this.db.maindb.get('clients').find({ clientID }).value();
            if (client === undefined) {
                return callback('Cliente não existe');
            }

            let clientDB = this.getClientDatabase(clientID);
            clientDB.get('atsConfig').assign({ enabled }).write();
            
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
            clientDB.get('atsConfig').assign({ autoClickEnabled: enabled }).write();
            
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
            clientDB.get('atsConfig').assign({ clickDelayMs: parseInt(delayMs) }).write();
            
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

    getClientDatabase(clientID) {
        return new (require('./databaseGateway')).clientdb(clientID);
    }
}

module.exports = ATSManager;
