<p align="center">
<img src="https://github.com/D3VL/L3MON/raw/master/server/assets/webpublic/logo.png" height="60"><br>
<b>L3M0N Atualizado</b><br>
Suíte remota de gerenciamento Android baseada em nuvem, com tecnologia NodeJS
</p>

<p align="center">
<a href="https://img.shields.io/badge/license-MIT-blue.svg"><img src="https://img.shields.io/badge/license-MIT-blue.svg"></a>
<a href="#"><img src="https://img.shields.io/badge/tests-170%2B%20passing-brightgreen"></a>
<a href="#"><img src="https://img.shields.io/badge/status-Beta%20Avançado-yellow"></a>
<a href="https://github.com/juniorjj0130-dev/L3M0N_Atualizado/commits/main"><img src="https://img.shields.io/github/last-commit/juniorjj0130-dev/L3M0N_Atualizado"></a>
</p>

---

## ⚠️ AVISO CRÍTICO – USO EXCLUSIVO EM LABORATÓRIO CONTROLADO

**Este software deve ser utilizado APENAS em ambiente controlado e com autorização explícita.**

- Use **somente em dispositivos próprios ou emuladores** que você controla totalmente.
- Isole a rede de testes (sem acesso à internet pública sempre que possível).
- Utilize credenciais exclusivas de laboratório e rotacione com frequência.
- Realize auditoria contínua de logs e eventos.
- **Documente sempre** o escopo, o consentimento explícito e a janela de testes.

**Checklist completo de segurança e boas práticas:**
**[LAB_SECURITY_CHECKLIST.md](LAB_SECURITY_CHECKLIST.md)**

Qualquer uso fora desses parâmetros é de **inteira responsabilidade do usuário**.

---

## Uso em Engajamentos de Red Team e Pentest Autorizado

**Esta ferramenta é destinada exclusivamente a profissionais de segurança da informação que realizam testes autorizados.**

### Requisitos Mínimos para Uso

Antes de utilizar o L3M0N Atualizado em qualquer ambiente, **é obrigatório** atender a todos os itens abaixo:

- **Rules of Engagement (RoE)** formalmente documentadas e assinadas pelo cliente ou responsável pelo alvo.
- **Escopo claramente definido** (dispositivos, aplicativos, tempo de teste, limites de ação).
- **Autorização por escrito** do proprietário ou responsável legal pelos dispositivos/testes.
- **Ambiente controlado e isolado** (preferencialmente em rede isolada, sem acesso à internet pública).
- **Dispositivos próprios ou emuladores** sob controle total do time de red team.
- **Consentimento explícito** e documentado de todos os envolvidos.

### Boas Práticas Recomendadas

- Utilize credenciais exclusivas e temporárias para cada engajamento.
- Mantenha logs completos de todas as ações realizadas (comandos enviados, dados coletados, horários).
- Realize auditoria contínua durante o teste.
- Após o término do engajamento:
  - Remova completamente todos os artefatos instalados nos dispositivos.
  - Revogue acessos e credenciais utilizadas.
  - Documente evidências de limpeza.
- Nunca deixe a ferramenta ou qualquer componente persistindo em ambientes de produção ou em dispositivos de usuários finais sem autorização explícita e contínua.

### O que é Estritamente Proibido

- Utilizar esta ferramenta sem Rules of Engagement formal e autorização escrita.
- Testar dispositivos ou contas que não pertencem ao escopo autorizado.
- Utilizar a ferramenta contra alvos reais sem consentimento explícito do proprietário.
- Manter persistência ou acesso após o término do engajamento autorizado.
- Compartilhar ou distribuir builds/apks gerados fora do contexto do engajamento autorizado.

### Responsabilidade

O uso desta ferramenta **sem autorização formal** pode configurar crimes previstos na legislação brasileira (Lei 12.737/2012, LGPD, entre outras) e em legislações internacionais.

**O mantenedor e os colaboradores deste projeto não se responsabilizam por qualquer uso indevido, ilegal ou não autorizado.**

### Recomendação

Recomenda-se fortemente que equipes de red team criem um **checklist interno de autorização** baseado neste documento e no arquivo [LAB_SECURITY_CHECKLIST.md](LAB_SECURITY_CHECKLIST.md) antes de iniciar qualquer engajamento.

Se você está realizando um teste autorizado, documente:

- Data e hora de início e término
- Escopo aprovado
- RoE assinada
- Evidências de limpeza ao final

---

**Como usar esta seção:**

1. Cole logo após a seção do **Aviso Crítico** (ou no final do README, antes dos Agradecimentos).
2. Mantenha o link para o `LAB_SECURITY_CHECKLIST.md`.
3. Se quiser, pode renomear o título para “Uso Responsável em Red Team” ou “Requisitos para Engajamentos Autorizados”.

---

Quer que eu faça alguma alteração nesta seção? (ex: deixar mais curta, mais rigorosa, adicionar algo específico, ou integrar diretamente na versão completa do README?)

Posso também entregar agora a **versão completa do README** já com esta seção incluída + as melhorias que discutimos antes.

## Sumário

- [⚠️ AVISO CRÍTICO – USO EXCLUSIVO EM LABORATÓRIO CONTROLADO](#️-aviso-crítico--uso-exclusivo-em-laboratório-controlado)
- [Uso em Engajamentos de Red Team e Pentest Autorizado](#uso-em-engajamentos-de-red-team-e-pentest-autorizado)
  - [Requisitos Mínimos para Uso](#requisitos-mínimos-para-uso)
  - [Boas Práticas Recomendadas](#boas-práticas-recomendadas)
  - [O que é Estritamente Proibido](#o-que-é-estritamente-proibido)
  - [Responsabilidade](#responsabilidade)
  - [Recomendação](#recomendação)
- [Sumário](#sumário)
- [Recursos](#recursos)
  - [Funcionalidades Principais](#funcionalidades-principais)
- [Módulos Avançados](#módulos-avançados)
- [Arquitetura de Módulos](#arquitetura-de-módulos)
- [Pré-requisitos](#pré-requisitos)
- [Instalação](#instalação)

---

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
- Monitoramento de Redes WiFi
- Explorador de Arquivos e Downloader
- Fila de Comandos e Execução
- Construtor e Injetor de APK Integrado

---

## Módulos Avançados

Os **12 módulos avançados** foram desenvolvidos seguindo uma arquitetura consistente e são destinados a **automação e testes em ambiente controlado de laboratório**.

Todos os módulos seguem o mesmo padrão:

- Protocolo de comunicação via Socket.IO
- Endpoints REST no servidor
- Integração com Accessibility Service no Android
- Dashboards em tempo real
- Suítes de teste específicas

**Lista dos módulos:**

1. **0xST** - SetText
2. **0xAT** - ATS System
3. **0xAC** - AutoClick
4. **0xDF** - Defense Disable
5. **0xPG** - Permission Grant
6. **0xGS** - Gesture Simulation
7. **0xTA** - Transaction Approval
8. **0xDSU** - Dynamic Screen Unlock
9. **0xOI** - Overlay Injection
10. **0xFC** - Customized Forms Capture
11. **0xBP** - Bypass Protections
12. **0xHI** - Advanced Hide Icon

Detalhes completos de arquitetura e implementação estão em **[AGENTS.md](AGENTS.md)**.

---

## Arquitetura de Módulos

Todos os módulos avançados seguem um padrão de arquitetura consistente:

- **Protocolo de Mensagem**: Eventos Socket.IO com chaves de mensagem 0xXX
- **Lado do Servidor**: Rotas Express.js, esquemas de banco de dados via lowdb, dashboards web em tempo real
- **Lado do Cliente**: Integração do Android Accessibility Service
- **Comunicação**: Mensagens bidirecionais em tempo real com cargas JSON
- **Persistência**: Configuração por dispositivo armazenada em lowdb

Cada módulo inclui endpoints REST, dashboard web, manipuladores Android, testes automatizados e trilhas de auditoria.

---

## Pré-requisitos

- Java Runtime Environment 8 (JRE 8)
- Node.js
- Um servidor (recomendado Linux)

---

## Instalação

1. Instale o **JRE 8**
   - Debian/Ubuntu: `sudo apt-get install openjdk-8-jre`
   - Fedora/Red Hat: `su -c "yum install java-1.8.0-openjdk"`
   - Windows: baixe em [oracle.com](https://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)

2. Instale o **Node.js** (recomendado via gerenciador de pacotes oficial)

3. Instale o **PM2** globalmente:
   ```bash
   npm install pm2 -g
   ```
