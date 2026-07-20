  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_queued_commands_client_id ON queued_commands(client_id);

-- =====================
-- Funções de atualização automática de updated_at
-- =====================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;$$ language 'plpgsql';

-- Triggers
DROP TRIGGER IF EXISTS update_admin_updated_at ON admin;
CREATE TRIGGER update_admin_updated_at
    BEFORE UPDATE ON admin
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_devices_updated_at ON devices;
CREATE TRIGGER update_devices_updated_at
    BEFORE UPDATE ON devices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================
-- Comentários nas tabelas
-- =====================
COMMENT ON TABLE admin IS 'Usuários administradores do painel';
COMMENT ON TABLE devices IS 'Dispositivos Android conectados (clientes)';
COMMENT ON TABLE logs IS 'Logs do sistema (info, error, alert, success)';
COMMENT ON TABLE commands IS 'Histórico de comandos enviados para os dispositivos';
COMMENT ON TABLE device_events IS 'Eventos granulares por dispositivo (SMS, chamadas, GPS, etc.)';