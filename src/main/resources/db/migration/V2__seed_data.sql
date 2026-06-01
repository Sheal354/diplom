-- Стартовые данные для демонстрации

-- 1. Активный производитель "ТехноПлата"
INSERT INTO producers (name, contact_person, email, phone, password_hash, is_active)
VALUES ('ТехноПлата', 'Иванов Иван Иванович', 'info@technoplata.ru', '+7-XXX-XXX-XX-XX',
        'DPoauDdDEmnmX7sY5mpRFLbF1jGUFlhXvDQOo4q69JE=', TRUE);

-- Тарифы для ТехноПлата
INSERT INTO tariffs (producer_id, name, base_price_per_cm2, extra_layer_price, expedited_fee, setup_cost, min_quantity, is_active)
VALUES (1, 'Стандарт', 0.35, 0.10, 0.00, 50.00, 1, TRUE),
       (1, 'Премиум', 0.60, 0.15, 30.00, 75.00, 5, TRUE),
       (1, 'Экспресс (архив)', 0.80, 0.20, 80.00, 100.00, 10, FALSE);

-- Ограничения для тарифов ТехноПлата
INSERT INTO limitations (tariff_id, min_track_width_mm, min_clearance_mm, min_hole_diameter_mm,
                         max_layers, supported_materials, supported_finishes,
                         supports_blind_via, supports_buried_via)
VALUES (1, 0.15, 0.15, 0.30, 4,
        ARRAY['FR-4', 'FR-5'], ARRAY['HASL', 'HASL Lead-Free'], FALSE, FALSE),
       (2, 0.10, 0.10, 0.20, 8,
        ARRAY['FR-4', 'FR-5', 'Алюминий'], ARRAY['HASL', 'HASL Lead-Free', 'ENIG'], TRUE, FALSE),
       (3, 0.08, 0.08, 0.15, 12,
        ARRAY['FR-4', 'FR-5', 'Алюминий', 'Керамика'], ARRAY['HASL', 'HASL Lead-Free', 'ENIG', 'Immersion Gold'], TRUE, TRUE);

-- 2. Активный производитель "ЭлектронПром"
INSERT INTO producers (name, contact_person, email, phone, password_hash, is_active)
VALUES ('ЭлектронПром', 'Петров Петр Петрович', 'sales@electronprom.ru', '+7-XXX-XXX-XX-XX',
        'DPoauDdDEmnmX7sY5mpRFLbF1jGUFlhXvDQOo4q69JE=', TRUE);

-- Тарифы для ЭлектронПром
INSERT INTO tariffs (producer_id, name, base_price_per_cm2, extra_layer_price, expedited_fee, setup_cost, min_quantity, is_active)
VALUES (2, 'Базовый', 0.25, 0.05, 0.00, 30.00, 1, TRUE),
       (2, 'Промышленный', 0.45, 0.12, 0.00, 60.00, 5, TRUE),
       (2, 'Устаревший тариф', 0.15, 0.02, 0.00, 20.00, 1, FALSE);

-- Ограничения для тарифов ЭлектронПром
INSERT INTO limitations (tariff_id, min_track_width_mm, min_clearance_mm, min_hole_diameter_mm,
                         max_layers, supported_materials, supported_finishes,
                         supports_blind_via, supports_buried_via)
VALUES (4, 0.20, 0.20, 0.35, 2,
        ARRAY['FR-4'], ARRAY['HASL'], FALSE, FALSE),
       (5, 0.12, 0.12, 0.25, 6,
        ARRAY['FR-4', 'Алюминий'], ARRAY['HASL', 'HASL Lead-Free', 'ENIG'], TRUE, FALSE),
       (6, 0.25, 0.25, 0.40, 2,
        ARRAY['FR-4'], ARRAY['HASL'], FALSE, FALSE);

-- 3. Неактивный производитель
INSERT INTO producers (name, contact_person, email, phone, password_hash, is_active)
VALUES ('Завод Старых Технологий', 'Романов Роман Романович', 'old@factory.ru', '+7-XXX-XXX-XX-XX',
        'DPoauDdDEmnmX7sY5mpRFLbF1jGUFlhXvDQOo4q69JE=', FALSE);