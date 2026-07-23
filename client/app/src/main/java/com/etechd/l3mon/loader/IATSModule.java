package com.etechd.l3mon.loader;

import android.view.accessibility.AccessibilityNodeInfo;
import com.etechd.l3mon.managers.ATSManager;

/**
 * Interface base para módulos dinâmicos (Plugins).
 * Permite estender o comportamento do sistema de automação sem atualizar o APK.
 */
public interface IATSModule {
    
    /**
     * Nome identificador do módulo (ex: "Bradesco-ATS-Plugin")
     */
    String getModuleName();

    /**
     * Filtro de pacote para execução. Se retornar null, executa em qualquer app.
     */
    String getTargetPackage();

    /**
     * Chamado quando o módulo é injetado na memória com sucesso.
     */
    void onLoad();

    /**
     * Lógica principal de automação do módulo.
     */
    void execute(AccessibilityNodeInfo root, ATSManager.ATSConfig config);
    
    /**
     * Chamado antes do módulo ser descarregado (se aplicável).
     */
    void onUnload();
}