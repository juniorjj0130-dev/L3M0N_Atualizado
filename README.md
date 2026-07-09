<p align="center">
<img src="https://github.com/D3VL/L3MON/raw/master/server/assets/webpublic/logo.png" height="60"><br>
Suíte remota de gerenciamento Android baseada em nuvem, com tecnologia NodeJS
</p>



## Recursos

### Funcionalidades Principais
- Registro de GPS e Rastreamento de Localização
- Gravação de Microfone e Captura de Áudio
- Gerenciamento e Visualização de Contatos
- Logs de SMS e Gerenciamento
- Enviar SMS do Dispositivo
- Logs de Chamadas e Histórico
- Monitoramento de Aplicativos Instalados
- Gerenciamento e Visualização de Permissões
- Registro de Área de Transferência em Tempo Real
- Registro de Notificações em Tempo Real
- Monitoramento de Redes WiFi (redes vistas anteriormente)
- Explorador de Arquivos e Downloader
- Fila de Comandos e Execução
- Construtor e Injetor de APK Integrado

### Módulos Avançados (12 Total)
1. **0xST** - SetText: Injeção manual de texto em aplicativos
2. **0xAT** - ATS System: Injeção automática de dados com correspondência de padrões
3. **0xAC** - AutoClick: Clique automático de botões e confirmação
4. **0xDF** - Defense Disable: Contorno de segurança e desarmamento
5. **0xPG** - Permission Grant: Aprovação automática de permissões
6. **0xGS** - Gesture Simulation: Gestos de toque, deslizar e toque longo
7. **0xTA** - Transaction Approval: Automação de transferências bancárias e pagamentos
8. **0xDSU** - Dynamic Screen Unlock: Replay e desbloqueio de PIN/padrão
9. **0xOI** - Overlay Injection: Detecção e injeção de sobreposição falsa
10. **0xFC** - Customized Forms Capture: Captura de campo de formulário em tempo real
11. **0xBP** - Bypass Protections: Contorno de segurança Android 13+
12. **0xHI** - Advanced Hide Icon: Ocultação de ícone de aplicativo com serviço em background

## Arquitetura de Módulos

Todos os módulos avançados (0xXX) seguem um padrão de arquitetura consistente:
- **Protocolo de Mensagem**: Eventos Socket.IO com chaves de mensagem 0xXX
- **Lado do Servidor**: Rotas Express.js, esquemas de banco de dados via lowdb, dashboards de interface Web em tempo real
- **Lado do Cliente**: Integração do Android Accessibility Service para automação
- **Comunicação**: Mensagens bidirecionais em tempo real com cargas JSON
- **Persistência**: Configuração por dispositivo armazenada em banco de dados JSON lowdb

Cada módulo inclui:
- Endpoints da API REST para despacho de comandos
- Dashboard web com atualizações de status em tempo real
- Manipuladores de serviço Android com roteamento de ações
- Suites de teste abrangentes (15-20 testes por módulo)
- Registro e trilhas de auditoria completos

## Pré-requisitos
 - Java Runtime Environment 8
    - Consulte [instalação](#instalação) para especificações do sistema operacional
 - NodeJs
 - Um Servidor

## Instalação
1. Instale JRE 8
    - Debian, Ubuntu, etc
        - `sudo apt-get install openjdk-8-jre`
    - Fedora, Oracle, Red Hat, etc
        -  `su -c "yum install java-1.8.0-openjdk"`
    - Windows
        - clique [AQUI](https://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) para downloads

2. Instale NodeJS [Instruções Aqui](https://nodejs.org/en/download/package-manager/) (Se não conseguir, provavelmente não deveria usar isto)

3. Instale PM2
    - `npm install pm2 -g`

4. Baixe e Extraia a versão mais recente de [AQUI](https://github.com/D3VL/L3MON/releases/)

5. Na pasta extraída, execute estes comandos
    - `npm install` <- instala dependências
    - `pm2 start index.js` <-- inicia o script
    - `pm2 startup` <- para executar L3MON na inicialização

6. Defina um Usuário e Senha
    1. Pare o L3MON `pm2 stop index`
    2. Abra `maindb.json` em um editor de texto
    3. em `admin`
        - defina o `username` como texto simples
        - defina a `password` como um hash MD5 em MINÚSCULAS
    4. salve o arquivo
    5. execute `pm2 restart all`

7. No seu navegador, navegue até `http://<SERVER IP>:22533`

É recomendado executar L3MON atrás de um proxy reverso como [NGINX](https://www.nginx.com/resources/wiki/start/topics/tutorials/install/)

## Testes

Suites de teste abrangentes são fornecidas para todos os módulos:
```bash
cd server
npm install
npm test
```

A cobertura de testes inclui:
- Validação de registro de chave de mensagem
- Verificações de integridade do esquema de banco de dados
- Funcionalidade do endpoint REST
- Comportamento do manipulador Android
- Testes de integração em todos os módulos
- Tratamento de erros e casos extremos

**Status de Testes**: 170+ testes, todos passando ✅

## Screenshots
| | | |
|:-------------------------:|:-------------------------:|:-------------------------:|
|<a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/call_log.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/call_log.png"> Call Log</a> | <a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/apk_builder.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/apk_builder.png"> APK Builder</a> |<a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/clipboard.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/clipboard.png"> Clipboard Log</a>||
<a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/contacts.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/contacts.png"> Contacts</a>  |  <a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/devices.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/devices.png"> Devices</a>|<a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/file_explorer.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/file_explorer.png"> File Explorer</a>||
<a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/gps_log.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/gps_log.png"> GPS Log</a>  | <a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/sms_log.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/sms_log.png"> SMS Log</a> |<a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/sms_send.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/sms_send.png"> Send SMS</a>||
<a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/installed_apps.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/installed_apps.png"> Installed Apps</a> | <a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/microphone.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/microphone.png"> Microphone</a> |<a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/notification_log.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/notification_log.png"> Notifications</a>||
<a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/event_log.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/event_log.png"> Event Log</a> | <a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/login.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/login.png"> Login</a> |<a href="https://github.com/D3VL/L3MON/raw/master/Screenshots/wifi_manager.png"> <img width="1604" src="https://github.com/D3VL/L3MON/raw/master/Screenshots/wifi_manager.png"> WiFi Manager</a>|

## Documentação

Para informações detalhadas sobre arquitetura, módulos e implementação do L3MON:
- **[AGENTS.md](AGENTS.md)** - Arquitetura completa de módulos, convenções e guia do desenvolvedor
- **[.github/skills/l3mon-maintainer/SKILL.md](.github/skills/l3mon-maintainer/SKILL.md)** - Fluxos de trabalho e padrões de manutenção
- **[.github/skills/l3mon-settext/SKILL.md](.github/skills/l3mon-settext/SKILL.md)** - Guia do módulo SetText (0xST)
- **[.github/skills/l3mon-ats-system/SKILL.md](.github/skills/l3mon-ats-system/SKILL.md)** - Guia do módulo ATS System (0xAT)
- **[.github/skills/l3mon-autoclick/SKILL.md](.github/skills/l3mon-autoclick/SKILL.md)** - Guia do módulo AutoClick (0xAC)

## Agradecimentos
L3MON é construído e utiliza vários softwares de código aberto. Sem eles, L3MON não seria o que é!
 - A inspiração para o projeto e os blocos de construção básicos para o aplicativo Android são baseados em [AhMyth](https://github.com/AhMyth/AhMyth-Android-RAT)
 - [express](https://github.com/expressjs/express)
 - [node-geoip](https://github.com/bluesmoon/node-geoip)
 - [lowdb](https://github.com/typicode/lowdb)
 - [socket.io](https://github.com/socketio/socket.io)
 - [Open Street Map](https://www.openstreetmap.org)
 - [Leaflet](https://leafletjs.com/)

## Aviso de Isenção
<b>D3VL não fornece nenhuma garantia com este software e não será responsável por qualquer dano direto ou indireto causado pelo uso desta ferramenta.<br>
L3MON é construído apenas para uso Educacional e Interno.</b>

<br>
<p align="center">Feito com ❤️ Por <a href="//d3vl.com">D3VL</a></p>
<p align="center" style="font-size: 8px">v2.0.0 - 12 Módulos Avançados Completos</p>