-- ------------------------------------------------------------
-- 1. Производитель «ТехноПлата» (активен)
-- ------------------------------------------------------------
INSERT INTO producers (name, contact_person, email, phone, password_hash, is_active)
VALUES ('ТехноПлата', 'Иванов Иван Иванович', 'info@technoplata.ru', '+7-900-111-22-33',
        'DPoauDdDEmnmX7sY5mpRFLbF1jGUFlhXvDQOo4q69JE=', TRUE);

-- Тарифы «ТехноПлаты»
INSERT INTO tariffs (producer_id, name, base_price_per_cm2, extra_layer_price, expedited_fee,
                     setup_cost, min_quantity, is_active)
VALUES
    (1, 'Стандарт', 0.25, 0.10, 200.00, 500.00, 5, TRUE),
    (1, 'Профессиональный', 0.50, 0.15, 350.00, 800.00, 10, TRUE),
    (1, 'Экспресс (архив)', 0.80, 0.25, 600.00, 1200.00, 10, FALSE);

-- Ограничения для тарифов «ТехноПлаты»
INSERT INTO limitations (tariff_id, min_track_width_mm, min_clearance_mm, min_hole_diameter_mm,
                         max_layers, supported_materials, supported_finishes,
                         supports_blind_via, supports_buried_via)
VALUES
    (1, 0.15, 0.15, 0.30, 4,
        ARRAY['FR-4 1.6mm', 'FR-5 1.6mm'], ARRAY['HASL', 'HASL Lead-Free'], FALSE, FALSE),
    (2, 0.10, 0.10, 0.20, 8,
        ARRAY['FR-4 1.6mm', 'FR-5 1.6mm', 'Алюминий 1.6mm'], ARRAY['HASL', 'HASL Lead-Free', 'ENIG'], TRUE, FALSE),
    (3, 0.08, 0.08, 0.15, 12,
        ARRAY['FR-4 1.6mm', 'FR-5 1.6mm', 'Алюминий 1.6mm', 'Керамика 1.0mm'],
        ARRAY['HASL', 'HASL Lead-Free', 'ENIG', 'Immersion Gold'], TRUE, TRUE);

-- ------------------------------------------------------------
-- 2. Производитель «ЭлектронПром» (активен)
-- ------------------------------------------------------------
INSERT INTO producers (name, contact_person, email, phone, password_hash, is_active)
VALUES ('ЭлектронПром', 'Петров Петр Петрович', 'sales@electronprom.ru', '+7-900-222-33-44',
        'DPoauDdDEmnmX7sY5mpRFLbF1jGUFlhXvDQOo4q69JE=', TRUE);

-- Тарифы «ЭлектронПром»
INSERT INTO tariffs (producer_id, name, base_price_per_cm2, extra_layer_price, expedited_fee,
                     setup_cost, min_quantity, is_active)
VALUES
    (2, 'Базовый', 0.20, 0.08, 150.00, 400.00, 5, TRUE),
    (2, 'Промышленный', 0.40, 0.12, 250.00, 600.00, 10, TRUE),
    (2, 'Устаревший (неактивен)', 0.15, 0.05, 100.00, 300.00, 5, FALSE);

-- Ограничения для тарифов «ЭлектронПром»
INSERT INTO limitations (tariff_id, min_track_width_mm, min_clearance_mm, min_hole_diameter_mm,
                         max_layers, supported_materials, supported_finishes,
                         supports_blind_via, supports_buried_via)
VALUES
    (4, 0.20, 0.20, 0.35, 2,
        ARRAY['FR-4 1.6mm'], ARRAY['HASL'], FALSE, FALSE),
    (5, 0.12, 0.12, 0.25, 6,
        ARRAY['FR-4 1.6mm', 'Алюминий 1.6mm'], ARRAY['HASL', 'HASL Lead-Free', 'ENIG'], TRUE, FALSE),
    (6, 0.25, 0.25, 0.40, 2,
        ARRAY['FR-4 1.6mm'], ARRAY['HASL'], FALSE, FALSE);

-- ------------------------------------------------------------
-- 3. Производитель «TestCorp» (активен, новый)
-- ------------------------------------------------------------
INSERT INTO producers (name, contact_person, email, phone, password_hash, is_active)
VALUES ('TestCorp', 'Сидоров Сидор Сидорович', 'info@testcorp.ru', '+7-900-333-55-66',
        'DPoauDdDEmnmX7sY5mpRFLbF1jGUFlhXvDQOo4q69JE=', TRUE);

-- Единственный тариф «Стандартный»
INSERT INTO tariffs (producer_id, name, base_price_per_cm2, extra_layer_price, expedited_fee,
                     setup_cost, min_quantity, is_active)
VALUES (3, 'Стандартный', 0.30, 0.12, 250.00, 500.00, 5, TRUE);

-- Ограничения для тарифа «Стандартный»
INSERT INTO limitations (tariff_id, min_track_width_mm, min_clearance_mm, min_hole_diameter_mm,
                         max_layers, supported_materials, supported_finishes,
                         supports_blind_via, supports_buried_via)
VALUES (7, 0.15, 0.15, 0.30, 4,
        ARRAY['FR-4 1.6mm', 'FR-5 1.6mm'], ARRAY['HASL', 'HASL Lead-Free', 'ENIG'], FALSE, FALSE);

-- ------------------------------------------------------------
-- 4. Неактивный производитель (для демонстрации фильтрации)
-- ------------------------------------------------------------
INSERT INTO producers (name, contact_person, email, phone, password_hash, is_active)
VALUES ('Завод Старых Технологий', 'Романов Роман Романович', 'old@factory.ru', '+7-900-444-77-88',
        'DPoauDdDEmnmX7sY5mpRFLbF1jGUFlhXvDQOo4q69JE=', FALSE);
-- нет активных тарифов