# Instruções de Agentes L3MON

L3MON é um conjunto de ferramentas de gerenciamento remoto de Android (RAT) baseado em nuvem construído com Node.js e Express. A base de código consiste em um servidor Node.js/Express com interface web e um cliente Android que se comunica via Socket.IO.

## Arquitetura do Projeto

### Servidor (`/server`)
- **Interface Web**: Servidor Express.js na porta 22533
- **Controle Socket.IO**: Comunicação em tempo real com clientes na porta 22222
- **Banco de Dados**: Baseado em JSON usando lowdb (`maindb.json`)
- **Visualizações**: Modelos EJS em `assets/views/`
- **Recursos Estáticos**: `assets/webpublic/` (inclui CSS, JS, downloads de APK)

### Cliente (`/client`)
- App Android construído com Gradle
- Se comunica com servidor via Socket.IO
- Envia dados do dispositivo (GPS, SMS, chamadas, contatos, microfone, área de transferência, etc.)

## Dependências Principais e Versões

```json
{
  "express": "^4.17.1",
  "socket.io": "^2.2.0",
  "ejs": "^2.6.2",
  "lowdb": "^1.0.0",
  "geoip-lite": "^1.3.7"
}
```

## Convenções Importantes

### Objetos Globais
O servidor expõe estes como globais em `index.js`:
- `global.CONST` — Constantes e caminhos
- `global.db` — Instância do gateway de banco de dados
- `global.logManager` — Sistema de logging
- `global.app` — Aplicação Express
- `global.clientManager` — Gerenciamento de dispositivos/clientes
- `global.apkBuilder` — Funcionalidade de construção de APK

### Chaves de Mensagem (Protocolo de Mensagem)
Localizado em `includes/const.js`, usa notação hexadecimal para mensagens de dispositivo para servidor:
```javascript
'0xCA' = câmera, '0xFI' = arquivos, '0xCL' = chamada, '0xSM' = SMS,
'0xMI' = microfone, '0xLO' = localização, '0xCO' = contatos, '0xWI' = wifi,
'0xNO' = notificação, '0xCB' = área de transferência, '0xIN' = aplicativos instalados,
'0xPM' = permissões, '0xGP' = permissão obtida, '0xPA' = ação de permissão,
'0xAC' = clique automático, '0xOV' = sobreposição, '0xAB' = bypass de acessibilidade, '0xAS' = acessibilidade
```

### Autenticação
- Credenciais de admin armazenadas em `maindb.json` sob `admin.username` e `admin.password`
- Senha armazenada como **hash MD5 em minúsculas**
- Login cria um cookie `loginToken` para gerenciamento de sessão

### Construção de APK
- APK descompilado em `app/factory/decompiled/`
- Arquivo de injeção de destino: `app/factory/decompiled/smali/com/etechd/l3mon/IOSocket.smali`
- Comandos em `includes/const.js`:
  - `buildCommand` — Constrói APK modificado
  - `signCommand` — Assina APK para instalação

## Configuração de Desenvolvimento

**Pré-requisitos:**
- Node.js
- Java 8 (JRE)
- PM2 (para produção: `npm install pm2 -g`)

**Desenvolvimento Local:**
```bash
cd server
npm install
node index.js          # Inicia o servidor
# Interface Web: http://localhost:22533
# Controle Socket.IO: localhost:22222
```

**Produção:**
```bash
pm2 start index.js
pm2 startup
pm2 restart all
```

## Arquivos Principais e Padrões

| Arquivo | Propósito | Notas |
|---------|-----------|-------|
| `server/index.js` | Ponto de entrada principal | Inicializa Express, Socket.IO, carrega todos os módulos |
| `server/includes/const.js` | Configuração e constantes | Chaves de mensagem, caminhos, comandos de construção |
| `server/includes/expressRoutes.js` | Rotas web e autenticação | Login, rotas do gerenciador de dispositivos, download de APK |
| `server/includes/clientManager.js` | Ciclo de vida do cliente | Conexões de dispositivos, fila de comandos |
| `server/includes/databaseGateway.js` | Wrapper do BD | Envolve lowdb para persistência de dados |
| `server/includes/logManager.js` | Logging de eventos | Rastreia eventos do sistema para auditoria |
| `server/includes/apkBuilder.js` | Modificação de APK | Corrige SMALI, constrói, assina APK |
| `server/assets/views/` | Modelos EJS | Login, interface do gerenciador de dispositivos, páginas de recursos de dispositivos |
| `server/test/` | Testes unitários | Validação de chaves de mensagem, testes de módulos |

## Fluxo de Comunicação Socket.IO

1. **Cliente conecta** com parâmetros: `id`, `model`, `manf`, `release`
2. **Servidor identifica cliente** por IP e geolocalização via geoip-lite
3. **Servidor enfileira comandos** para cliente via chaves de mensagem
4. **Cliente responde** com dados (logs, conteúdo de arquivos, etc.)
5. **Servidor processa e armazena** em banco de dados

## Tarefas Comuns

### Adicionando um Novo Recurso de Dispositivo
1. Defina a chave de mensagem em `includes/const.js`
2. Crie um manipulador em `clientManager.js` para enfileirar comandos
3. Adicione rota Express em `includes/expressRoutes.js`
4. Crie visualização EJS em `assets/views/deviceManagerPages/`
5. Adicione manipulador Android correspondente no código do cliente

### Modificando Injeção de APK
1. Edite `app/factory/decompiled/smali/com/etechd/l3mon/IOSocket.smali`
2. Atualize parâmetros de conexão Socket.IO se necessário
3. Execute `apkBuilder.buildAPK()` para reempacotar
4. APK disponível em `assets/webpublic/L3MON.apk`

### Depurando Conexões de Clientes
Defina `debug: true` em `const.js` para registrar todos os eventos Socket.IO no console. Observe `clientManager` para ciclo de vida do dispositivo.

## Testes

**Execute testes existentes:**
```bash
cd server
npm test
```

Testes validam chaves de mensagem e funcionalidade de módulos. Localizado em `server/test/`.

## Notas de Implantação

- Execute atrás de proxy reverso NGINX (recomendado)
- Configure firewall para portas 22533 (web) e 22222 (Socket.IO)
- Armazene `maindb.json` com segurança (contém credenciais de admin)
- Use PM2 para gerenciamento de processos e reinicialização automática

## Preenchimento de Dados e Injeção de Texto

### SetText (0xST) - Injeção Manual de Texto

O recurso **SetText** utiliza o método `ACTION_SET_TEXT` do Accessibility Service para injetar texto em campos de texto editáveis:

1. **Localização**: O app localiza campos de destino (como chave, endereço, email, etc)
2. **Injeção**: Utiliza `ACTION_SET_TEXT` ou envia argumentos de texto diretamente para o nó do campo
3. **Execução**: O texto é injetado automaticamente no campo especificado

**Implementação:**
- **Chave**: `0xST` 
- **Servidor**: Rota `POST /manage/:deviceid/0xST`
- **Cliente**: Método `ST()` em `ConnectionManager.java`
- **Interface**: Página "Text Injection" no gerenciador de dispositivos

### ATS (0xAT) - Automated Transfer System

O **ATS (Automated Transfer System)** é um mecanismo de automação que substitui automaticamente dados legítimos que o usuário pretendia inserir por dados fictícios em campos específicos:

**Funcionamento:**
1. **Detecção**: Monitora campos de texto através do Accessibility Service
2. **Identificação**: Detecta padrões de campos-alvo (email, password, CPF, cartão de crédito, etc)
3. **Substituição**: Injeta dados fictícios automaticamente nesses campos
4. **Auditoria**: Registra todas as operações em log para rastreamento

**Padrões Suportados:**
- Email, telefone, senha, endereço, CPF, cartão de crédito
- Conta bancária, login, usuário, CEP, RG, banco

**Dados Fictícios Padrão:**
```javascript
email: usuario.ficticioso@example.com
phone: +5511999999999
password: SenhaFictica123!
cpf: 12345678900
creditCard: 4532015112830366
```

**Implementação:**
- **Chave**: `0xAT`
- **Servidor**: Classe `ATSManager` em `includes/atsManager.js`
- **Cliente**: Método `performATSAutomation()` em `AccessibilityCaptureService.java`
- **Interface**: Página "ATS System" com gerenciamento de dados e padrões
- **Banco de Dados**: Configuração persistida em `atsConfig` de cada cliente

**Fluxo de Automação ATS:**
```
Usuário abre aplicativo
    ↓
Campo "email" é detectado
    ↓
Padrão "email" encontra match
    ↓
Dado fictício é injetado: usuario.ficticioso@example.com
    ↓
Operação é registrada no log de auditoria
```

### AutoClick (0xAC) - Clique Automático de Confirmação

O **AutoClick** detecta e clica automaticamente em botões de confirmação/envio após o preenchimento automático de dados. Funciona em conjunto com ATS para criar um fluxo completamente automático.

**Funcionamento:**
1. **Detecção**: Procura por botões na hierarquia de nós do Accessibility Service
2. **Padrão Matching**: Compara texto, description e viewId contra lista de padrões
3. **Clique**: Dispara `AccessibilityNodeInfo.ACTION_CLICK` quando padrão encontra match
4. **Timing**: Executa com delay configurável (ex: 500ms) para garantir sincronia
5. **Auditoria**: Registra cada clique em log de operações

**Padrões de Botão Suportados:**
- Português: enviar, confirmar, continuar, próximo, avançar, ok, realizar, aceitar
- English: send, confirm, continue, next, forward, submit, accept, ok

**Dados Configuráveis:**
```javascript
autoClickEnabled: false,              // Ativa/desativa feature
clickDelayMs: 500,                    // Delay antes do clique (ms)
buttonPatterns: [                     // Padrões a detectar
    'enviar', 'send', 'submit', 'confirmar', 'confirm',
    'ok', 'continuar', 'continue', 'proximo', 'next'
]
```

**Implementação:**
- **Chave**: `0xAC`
- **Servidor**: Métodos em `ATSManager` para gerenciar configuração
- **Rotas**: `/ats/autoclick/enable/:enabled`, `/ats/autoclick/delay/:delayMs`
- **Cliente**: Método `findAndClickConfirmationButton()` em `AccessibilityCaptureService.java`
- **Handler**: `ACAutoClickWithPatterns()` em `ConnectionManager.java`
- **Interface**: Seção "Clique Automático" na página ATS
- **Integração**: Pode funcionar com ATS via `performATSAutomationWithAutoClick()`

**Fluxo Completo ATS + AutoClick:**
```
1. Usuário abre aplicativo de e-commerce
    ↓
2. ATS detecta campo "Número do Cartão"
    ↓
3. ATS injeta: "4532015112830366"
    ↓
4. [Aguarda 500ms - delay configurado]
    ↓
5. AutoClick detecta botão "Confirmar Compra"
    ↓
6. AutoClick clica no botão automaticamente
    ↓
7. Transação é processada com cartão fictício
    ↓
8. Log registra: padrão "confirmar", texto do botão, timestamp, sucesso
```

**Tempo de Execução:** O processo completo (injeção + clique) ocorre em frações de segundo, típicamente 500-1000ms.

### Comparação das Três Técnicas

| Aspecto | SetText (0xST) | ATS (0xAT) | AutoClick (0xAC) |
|---------|---|---|---|
| **Ativação** | Manual via UI | Automática quando habilitada | Automática após ATS |
| **Alvo** | Campo específico | Campos com padrão match | Botões com padrão match |
| **Dados** | Especificado pelo usuário | Configurado via mapeamento | Padrões pré-definidos |
| **Trigger** | Usuário escreve comando | Detecção de campo | Conclusão de injeção ATS |
| **Velocidade** | Controlada pelo usuário | Imediata | 500ms+ (delay) |
| **Automação** | Manual | Semi-automática | Totalmente automática |
| **Uso** | Testes pontuais | Automação contínua | Automação de confirmação |
| **Log** | Sucesso/erro da injeção | Auditoria de injeção | Auditoria de cliques |

---

## Links para Documentação Relacionada

- [README.md](README.md) — Visão geral do projeto, recursos, instalação
- [Documentação Express.js](https://expressjs.com/) — Framework web
- [Documentação Socket.IO](https://socket.io/docs/) — Comunicação em tempo real
- [Documentação lowdb](https://github.com/typicode/lowdb) — Banco de dados JSON
- [.github/skills/l3mon-settext/SKILL.md](.github/skills/l3mon-settext/SKILL.md) — Guia SetText
- [.github/skills/l3mon-ats-system/SKILL.md](.github/skills/l3mon-ats-system/SKILL.md) — Guia ATS System
- [.github/skills/l3mon-autoclick/SKILL.md](.github/skills/l3mon-autoclick/SKILL.md) — Guia AutoClick

---

**Ao trabalhar nesta base de código, concentre-se em:**
- Manter consistência do protocolo de chaves de mensagem
- Respeitar o padrão de inicialização de objetos globais
- Seguir convenções EJS/Express para novas rotas
- Testar comunicação Socket.IO cliente/servidor
- Manter modificações de APK isoladas em correção SMALI
- Implementar novos recursos de injeção e automação seguindo padrão SetText/ATS
