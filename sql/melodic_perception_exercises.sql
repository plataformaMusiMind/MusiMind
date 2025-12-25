-- =========================================================
-- MELODIC PERCEPTION EXERCISES FOR MUSIMIND
-- 20 exercícios: 10 com 2 compassos, 10 com 4 compassos
-- Escala de Dó Maior (C4 = MIDI 60)
-- =========================================================

-- First, ensure we have a melodic perception category
INSERT INTO exercise_categories (id, name, display_name, description, icon, color, sort_order, is_active)
VALUES (
    'cat_melodic_perception',
    'Melodic Perception',
    'Percepção Melódica',
    'Pratique identificar e escrever melodias',
    'hearing',
    '#8B5CF6',
    3,
    true
) ON CONFLICT (id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description;

-- =========================================================
-- EXERCISES: 10 exercícios de 2 compassos (8 tempos em 4/4)
-- =========================================================

-- Exercise 1: Graus conjuntos ascendentes (C-D-E-F-G-F-E-D)
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_2c_01', 'cat_melodic_perception', 'Escada Subindo', 'Melodia em graus conjuntos ascendentes e descendentes', 1, 15, 5, 60, false, true, 1);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_2c01_1', 'melodic_2c_01', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c01_2', 'melodic_2c_01', 2, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_2c01_3', 'melodic_2c_01', 3, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c01_4', 'melodic_2c_01', 4, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_2c01_5', 'melodic_2c_01', 5, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c01_6', 'melodic_2c_01', 6, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_2c01_7', 'melodic_2c_01', 7, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c01_8', 'melodic_2c_01', 8, 62, 1.0, 80, 'treble', 4, 4);  -- D4

-- Exercise 2: Graus conjuntos descendentes (G-F-E-D-C-D-E-C)
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_2c_02', 'cat_melodic_perception', 'Escada Descendo', 'Melodia em graus conjuntos descendentes', 1, 15, 5, 60, false, true, 2);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_2c02_1', 'melodic_2c_02', 1, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c02_2', 'melodic_2c_02', 2, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_2c02_3', 'melodic_2c_02', 3, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c02_4', 'melodic_2c_02', 4, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_2c02_5', 'melodic_2c_02', 5, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c02_6', 'melodic_2c_02', 6, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_2c02_7', 'melodic_2c_02', 7, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c02_8', 'melodic_2c_02', 8, 60, 2.0, 80, 'treble', 4, 4);  -- C4 (mínima)

-- Exercise 3: Arpejo de Dó Maior (C-E-G-E-C-G-E-C)
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_2c_03', 'cat_melodic_perception', 'Arpejo Básico', 'Arpejo de Dó Maior simples', 1, 15, 5, 60, false, true, 3);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_2c03_1', 'melodic_2c_03', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c03_2', 'melodic_2c_03', 2, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c03_3', 'melodic_2c_03', 3, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c03_4', 'melodic_2c_03', 4, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c03_5', 'melodic_2c_03', 5, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c03_6', 'melodic_2c_03', 6, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c03_7', 'melodic_2c_03', 7, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c03_8', 'melodic_2c_03', 8, 60, 1.0, 80, 'treble', 4, 4);  -- C4

-- Exercise 4: Padrão de terças (C-E-D-F-E-G-F-A)
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_2c_04', 'cat_melodic_perception', 'Terças Ascendentes', 'Padrão de terças subindo', 2, 20, 7, 60, false, true, 4);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_2c04_1', 'melodic_2c_04', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c04_2', 'melodic_2c_04', 2, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c04_3', 'melodic_2c_04', 3, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_2c04_4', 'melodic_2c_04', 4, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_2c04_5', 'melodic_2c_04', 5, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c04_6', 'melodic_2c_04', 6, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c04_7', 'melodic_2c_04', 7, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_2c04_8', 'melodic_2c_04', 8, 69, 1.0, 80, 'treble', 4, 4);  -- A4

-- Exercise 5: Saltos de quinta (C-G-D-A-E-G-C-G)
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_2c_05', 'cat_melodic_perception', 'Saltos de Quinta', 'Intervalos de quinta justa', 2, 20, 7, 60, false, true, 5);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_2c05_1', 'melodic_2c_05', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c05_2', 'melodic_2c_05', 2, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c05_3', 'melodic_2c_05', 3, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_2c05_4', 'melodic_2c_05', 4, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_2c05_5', 'melodic_2c_05', 5, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c05_6', 'melodic_2c_05', 6, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c05_7', 'melodic_2c_05', 7, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c05_8', 'melodic_2c_05', 8, 67, 1.0, 80, 'treble', 4, 4);  -- G4

-- Exercise 6: Melodia do Brilha, Brilha simplificada (C-C-G-G-A-A-G)
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_2c_06', 'cat_melodic_perception', 'Estrela Brilhante', 'Melodia simples conhecida', 1, 15, 5, 60, false, true, 6);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_2c06_1', 'melodic_2c_06', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c06_2', 'melodic_2c_06', 2, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c06_3', 'melodic_2c_06', 3, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c06_4', 'melodic_2c_06', 4, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c06_5', 'melodic_2c_06', 5, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_2c06_6', 'melodic_2c_06', 6, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_2c06_7', 'melodic_2c_06', 7, 67, 2.0, 80, 'treble', 4, 4);  -- G4 (mínima)

-- Exercise 7: Bordadura (C-D-C-E-D-C-B-C)
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_2c_07', 'cat_melodic_perception', 'Bordaduras', 'Ornamentos com notas vizinhas', 2, 20, 7, 60, false, true, 7);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_2c07_1', 'melodic_2c_07', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c07_2', 'melodic_2c_07', 2, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_2c07_3', 'melodic_2c_07', 3, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c07_4', 'melodic_2c_07', 4, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c07_5', 'melodic_2c_07', 5, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_2c07_6', 'melodic_2c_07', 6, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c07_7', 'melodic_2c_07', 7, 59, 1.0, 80, 'treble', 4, 4),  -- B3
('mn_2c07_8', 'melodic_2c_07', 8, 60, 1.0, 80, 'treble', 4, 4);  -- C4

-- Exercise 8: Pentatônica (C-D-E-G-A-G-E-C)
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_2c_08', 'cat_melodic_perception', 'Escala Pentatônica', 'Melodia com escala de 5 notas', 2, 20, 7, 60, false, true, 8);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_2c08_1', 'melodic_2c_08', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c08_2', 'melodic_2c_08', 2, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_2c08_3', 'melodic_2c_08', 3, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c08_4', 'melodic_2c_08', 4, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c08_5', 'melodic_2c_08', 5, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_2c08_6', 'melodic_2c_08', 6, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c08_7', 'melodic_2c_08', 7, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c08_8', 'melodic_2c_08', 8, 60, 1.0, 80, 'treble', 4, 4);  -- C4

-- Exercise 9: Saltos de sexta (C-A-D-B-E-C-G-E)
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_2c_09', 'cat_melodic_perception', 'Saltos de Sexta', 'Intervalos de sexta maior', 3, 25, 10, 60, false, true, 9);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_2c09_1', 'melodic_2c_09', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c09_2', 'melodic_2c_09', 2, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_2c09_3', 'melodic_2c_09', 3, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_2c09_4', 'melodic_2c_09', 4, 71, 1.0, 80, 'treble', 4, 4),  -- B4
('mn_2c09_5', 'melodic_2c_09', 5, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c09_6', 'melodic_2c_09', 6, 72, 1.0, 80, 'treble', 4, 4),  -- C5
('mn_2c09_7', 'melodic_2c_09', 7, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c09_8', 'melodic_2c_09', 8, 64, 1.0, 80, 'treble', 4, 4);  -- E4

-- Exercise 10: Contorno melódico misto (C-E-D-F-E-G-A-C)
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_2c_10', 'cat_melodic_perception', 'Contorno Misto', 'Mistura de graus conjuntos e disjuntos', 2, 20, 7, 60, false, true, 10);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_2c10_1', 'melodic_2c_10', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_2c10_2', 'melodic_2c_10', 2, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c10_3', 'melodic_2c_10', 3, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_2c10_4', 'melodic_2c_10', 4, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_2c10_5', 'melodic_2c_10', 5, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_2c10_6', 'melodic_2c_10', 6, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_2c10_7', 'melodic_2c_10', 7, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_2c10_8', 'melodic_2c_10', 8, 72, 1.0, 80, 'treble', 4, 4);  -- C5

-- =========================================================
-- EXERCISES: 10 exercícios de 4 compassos (16 tempos em 4/4)
-- =========================================================

-- Exercise 11: Escala completa ida e volta
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_4c_01', 'cat_melodic_perception', 'Escala Completa', 'Escala de Dó Maior completa ida e volta', 2, 30, 10, 90, false, true, 11);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_4c01_01', 'melodic_4c_01', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c01_02', 'melodic_4c_01', 2, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c01_03', 'melodic_4c_01', 3, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c01_04', 'melodic_4c_01', 4, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_4c01_05', 'melodic_4c_01', 5, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c01_06', 'melodic_4c_01', 6, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_4c01_07', 'melodic_4c_01', 7, 71, 1.0, 80, 'treble', 4, 4),  -- B4
('mn_4c01_08', 'melodic_4c_01', 8, 72, 1.0, 80, 'treble', 4, 4),  -- C5
('mn_4c01_09', 'melodic_4c_01', 9, 71, 1.0, 80, 'treble', 4, 4),  -- B4
('mn_4c01_10', 'melodic_4c_01', 10, 69, 1.0, 80, 'treble', 4, 4), -- A4
('mn_4c01_11', 'melodic_4c_01', 11, 67, 1.0, 80, 'treble', 4, 4), -- G4
('mn_4c01_12', 'melodic_4c_01', 12, 65, 1.0, 80, 'treble', 4, 4), -- F4
('mn_4c01_13', 'melodic_4c_01', 13, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c01_14', 'melodic_4c_01', 14, 62, 1.0, 80, 'treble', 4, 4), -- D4
('mn_4c01_15', 'melodic_4c_01', 15, 60, 2.0, 80, 'treble', 4, 4); -- C4 (mínima)

-- Exercise 12: Arpejo com extensão
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_4c_02', 'cat_melodic_perception', 'Arpejo Estendido', 'Arpejo maior com extensão de oitava', 2, 30, 10, 90, false, true, 12);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_4c02_01', 'melodic_4c_02', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c02_02', 'melodic_4c_02', 2, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c02_03', 'melodic_4c_02', 3, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c02_04', 'melodic_4c_02', 4, 72, 1.0, 80, 'treble', 4, 4),  -- C5
('mn_4c02_05', 'melodic_4c_02', 5, 76, 1.0, 80, 'treble', 4, 4),  -- E5
('mn_4c02_06', 'melodic_4c_02', 6, 72, 1.0, 80, 'treble', 4, 4),  -- C5
('mn_4c02_07', 'melodic_4c_02', 7, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c02_08', 'melodic_4c_02', 8, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c02_09', 'melodic_4c_02', 9, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c02_10', 'melodic_4c_02', 10, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c02_11', 'melodic_4c_02', 11, 67, 1.0, 80, 'treble', 4, 4), -- G4
('mn_4c02_12', 'melodic_4c_02', 12, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c02_13', 'melodic_4c_02', 13, 60, 2.0, 80, 'treble', 4, 4), -- C4 (mínima)
('mn_4c02_14', 'melodic_4c_02', 14, 60, 2.0, 80, 'treble', 4, 4); -- C4 (mínima)

-- Exercise 13: Padrão de sequência ascendente
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_4c_03', 'cat_melodic_perception', 'Sequência Ascendente', 'Padrão melódico repetido subindo', 2, 30, 10, 90, false, true, 13);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_4c03_01', 'melodic_4c_03', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c03_02', 'melodic_4c_03', 2, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c03_03', 'melodic_4c_03', 3, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c03_04', 'melodic_4c_03', 4, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c03_05', 'melodic_4c_03', 5, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c03_06', 'melodic_4c_03', 6, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_4c03_07', 'melodic_4c_03', 7, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c03_08', 'melodic_4c_03', 8, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_4c03_09', 'melodic_4c_03', 9, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c03_10', 'melodic_4c_03', 10, 69, 1.0, 80, 'treble', 4, 4), -- A4
('mn_4c03_11', 'melodic_4c_03', 11, 71, 1.0, 80, 'treble', 4, 4), -- B4
('mn_4c03_12', 'melodic_4c_03', 12, 69, 1.0, 80, 'treble', 4, 4), -- A4
('mn_4c03_13', 'melodic_4c_03', 13, 67, 1.0, 80, 'treble', 4, 4), -- G4
('mn_4c03_14', 'melodic_4c_03', 14, 65, 1.0, 80, 'treble', 4, 4), -- F4
('mn_4c03_15', 'melodic_4c_03', 15, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c03_16', 'melodic_4c_03', 16, 60, 1.0, 80, 'treble', 4, 4); -- C4

-- Exercise 14: Melodia ondulante
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_4c_04', 'cat_melodic_perception', 'Onda Melódica', 'Contorno ondulante com saltos e graus', 3, 35, 12, 90, false, true, 14);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_4c04_01', 'melodic_4c_04', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c04_02', 'melodic_4c_04', 2, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c04_03', 'melodic_4c_04', 3, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c04_04', 'melodic_4c_04', 4, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_4c04_05', 'melodic_4c_04', 5, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_4c04_06', 'melodic_4c_04', 6, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c04_07', 'melodic_4c_04', 7, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c04_08', 'melodic_4c_04', 8, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c04_09', 'melodic_4c_04', 9, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c04_10', 'melodic_4c_04', 10, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c04_11', 'melodic_4c_04', 11, 62, 1.0, 80, 'treble', 4, 4), -- D4
('mn_4c04_12', 'melodic_4c_04', 12, 67, 1.0, 80, 'treble', 4, 4), -- G4
('mn_4c04_13', 'melodic_4c_04', 13, 65, 1.0, 80, 'treble', 4, 4), -- F4
('mn_4c04_14', 'melodic_4c_04', 14, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c04_15', 'melodic_4c_04', 15, 62, 1.0, 80, 'treble', 4, 4), -- D4
('mn_4c04_16', 'melodic_4c_04', 16, 60, 1.0, 80, 'treble', 4, 4); -- C4

-- Exercise 15: Tema com variação
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_4c_05', 'cat_melodic_perception', 'Tema e Variação', 'Melodia com repetição variada', 3, 35, 12, 90, false, true, 15);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_4c05_01', 'melodic_4c_05', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c05_02', 'melodic_4c_05', 2, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c05_03', 'melodic_4c_05', 3, 64, 2.0, 80, 'treble', 4, 4),  -- E4 (mínima)
('mn_4c05_04', 'melodic_4c_05', 4, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c05_05', 'melodic_4c_05', 5, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c05_06', 'melodic_4c_05', 6, 60, 2.0, 80, 'treble', 4, 4),  -- C4 (mínima)
('mn_4c05_07', 'melodic_4c_05', 7, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c05_08', 'melodic_4c_05', 8, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_4c05_09', 'melodic_4c_05', 9, 67, 2.0, 80, 'treble', 4, 4),  -- G4 (mínima)
('mn_4c05_10', 'melodic_4c_05', 10, 65, 1.0, 80, 'treble', 4, 4), -- F4
('mn_4c05_11', 'melodic_4c_05', 11, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c05_12', 'melodic_4c_05', 12, 60, 2.0, 80, 'treble', 4, 4); -- C4 (mínima)

-- Exercise 16: Saltos com resolução
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_4c_06', 'cat_melodic_perception', 'Saltos e Resolução', 'Grandes saltos seguidos de graus conjuntos', 3, 35, 12, 90, false, true, 16);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_4c06_01', 'melodic_4c_06', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c06_02', 'melodic_4c_06', 2, 72, 1.0, 80, 'treble', 4, 4),  -- C5
('mn_4c06_03', 'melodic_4c_06', 3, 71, 1.0, 80, 'treble', 4, 4),  -- B4
('mn_4c06_04', 'melodic_4c_06', 4, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_4c06_05', 'melodic_4c_06', 5, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c06_06', 'melodic_4c_06', 6, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c06_07', 'melodic_4c_06', 7, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c06_08', 'melodic_4c_06', 8, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c06_09', 'melodic_4c_06', 9, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_4c06_10', 'melodic_4c_06', 10, 72, 1.0, 80, 'treble', 4, 4), -- C5
('mn_4c06_11', 'melodic_4c_06', 11, 69, 1.0, 80, 'treble', 4, 4), -- A4
('mn_4c06_12', 'melodic_4c_06', 12, 67, 1.0, 80, 'treble', 4, 4), -- G4
('mn_4c06_13', 'melodic_4c_06', 13, 65, 1.0, 80, 'treble', 4, 4), -- F4
('mn_4c06_14', 'melodic_4c_06', 14, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c06_15', 'melodic_4c_06', 15, 62, 1.0, 80, 'treble', 4, 4), -- D4
('mn_4c06_16', 'melodic_4c_06', 16, 60, 1.0, 80, 'treble', 4, 4); -- C4

-- Exercise 17: Escala com pausas
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_4c_07', 'cat_melodic_perception', 'Escala Fragmentada', 'Escala com respirações', 2, 30, 10, 90, false, true, 17);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_4c07_01', 'melodic_4c_07', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c07_02', 'melodic_4c_07', 2, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c07_03', 'melodic_4c_07', 3, 64, 2.0, 80, 'treble', 4, 4),  -- E4 (mínima)
('mn_4c07_04', 'melodic_4c_07', 4, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_4c07_05', 'melodic_4c_07', 5, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c07_06', 'melodic_4c_07', 6, 69, 2.0, 80, 'treble', 4, 4),  -- A4 (mínima)
('mn_4c07_07', 'melodic_4c_07', 7, 71, 1.0, 80, 'treble', 4, 4),  -- B4
('mn_4c07_08', 'melodic_4c_07', 8, 72, 1.0, 80, 'treble', 4, 4),  -- C5
('mn_4c07_09', 'melodic_4c_07', 9, 71, 2.0, 80, 'treble', 4, 4),  -- B4 (mínima)
('mn_4c07_10', 'melodic_4c_07', 10, 69, 1.0, 80, 'treble', 4, 4), -- A4
('mn_4c07_11', 'melodic_4c_07', 11, 67, 1.0, 80, 'treble', 4, 4), -- G4
('mn_4c07_12', 'melodic_4c_07', 12, 60, 2.0, 80, 'treble', 4, 4); -- C4 (mínima)

-- Exercise 18: Melodia em arco
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_4c_08', 'cat_melodic_perception', 'Arco Melódico', 'Contorno em forma de arco', 2, 30, 10, 90, false, true, 18);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_4c08_01', 'melodic_4c_08', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c08_02', 'melodic_4c_08', 2, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c08_03', 'melodic_4c_08', 3, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c08_04', 'melodic_4c_08', 4, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c08_05', 'melodic_4c_08', 5, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_4c08_06', 'melodic_4c_08', 6, 72, 1.0, 80, 'treble', 4, 4),  -- C5
('mn_4c08_07', 'melodic_4c_08', 7, 71, 1.0, 80, 'treble', 4, 4),  -- B4
('mn_4c08_08', 'melodic_4c_08', 8, 72, 1.0, 80, 'treble', 4, 4),  -- C5
('mn_4c08_09', 'melodic_4c_08', 9, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_4c08_10', 'melodic_4c_08', 10, 67, 1.0, 80, 'treble', 4, 4), -- G4
('mn_4c08_11', 'melodic_4c_08', 11, 65, 1.0, 80, 'treble', 4, 4), -- F4
('mn_4c08_12', 'melodic_4c_08', 12, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c08_13', 'melodic_4c_08', 13, 62, 1.0, 80, 'treble', 4, 4), -- D4
('mn_4c08_14', 'melodic_4c_08', 14, 60, 2.0, 80, 'treble', 4, 4); -- C4 (mínima)

-- Exercise 19: Ziguezague
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_4c_09', 'cat_melodic_perception', 'Ziguezague', 'Alternância entre notas altas e baixas', 3, 35, 12, 90, false, true, 19);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_4c09_01', 'melodic_4c_09', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c09_02', 'melodic_4c_09', 2, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_4c09_03', 'melodic_4c_09', 3, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c09_04', 'melodic_4c_09', 4, 71, 1.0, 80, 'treble', 4, 4),  -- B4
('mn_4c09_05', 'melodic_4c_09', 5, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c09_06', 'melodic_4c_09', 6, 72, 1.0, 80, 'treble', 4, 4),  -- C5
('mn_4c09_07', 'melodic_4c_09', 7, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_4c09_08', 'melodic_4c_09', 8, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_4c09_09', 'melodic_4c_09', 9, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c09_10', 'melodic_4c_09', 10, 60, 1.0, 80, 'treble', 4, 4), -- C4
('mn_4c09_11', 'melodic_4c_09', 11, 65, 1.0, 80, 'treble', 4, 4), -- F4
('mn_4c09_12', 'melodic_4c_09', 12, 62, 1.0, 80, 'treble', 4, 4), -- D4
('mn_4c09_13', 'melodic_4c_09', 13, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c09_14', 'melodic_4c_09', 14, 60, 1.0, 80, 'treble', 4, 4), -- C4
('mn_4c09_15', 'melodic_4c_09', 15, 67, 1.0, 80, 'treble', 4, 4), -- G4
('mn_4c09_16', 'melodic_4c_09', 16, 60, 1.0, 80, 'treble', 4, 4); -- C4

-- Exercise 20: Frase musical completa
INSERT INTO exercises (id, category_id, title, description, difficulty, xp_reward, coins_reward, estimated_time_seconds, is_premium, is_active, sort_order)
VALUES ('melodic_4c_10', 'cat_melodic_perception', 'Frase Completa', 'Uma frase musical de 4 compassos', 3, 35, 12, 90, false, true, 20);

INSERT INTO melodic_notes (id, exercise_id, sequence_order, pitch, duration_beats, tempo, clef, time_signature_num, time_signature_den) VALUES
('mn_4c10_01', 'melodic_4c_10', 1, 60, 1.0, 80, 'treble', 4, 4),  -- C4
('mn_4c10_02', 'melodic_4c_10', 2, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c10_03', 'melodic_4c_10', 3, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c10_04', 'melodic_4c_10', 4, 65, 1.0, 80, 'treble', 4, 4),  -- F4
('mn_4c10_05', 'melodic_4c_10', 5, 64, 1.0, 80, 'treble', 4, 4),  -- E4
('mn_4c10_06', 'melodic_4c_10', 6, 62, 1.0, 80, 'treble', 4, 4),  -- D4
('mn_4c10_07', 'melodic_4c_10', 7, 60, 2.0, 80, 'treble', 4, 4),  -- C4 (mínima)
('mn_4c10_08', 'melodic_4c_10', 8, 67, 1.0, 80, 'treble', 4, 4),  -- G4
('mn_4c10_09', 'melodic_4c_10', 9, 69, 1.0, 80, 'treble', 4, 4),  -- A4
('mn_4c10_10', 'melodic_4c_10', 10, 72, 1.0, 80, 'treble', 4, 4), -- C5
('mn_4c10_11', 'melodic_4c_10', 11, 71, 1.0, 80, 'treble', 4, 4), -- B4
('mn_4c10_12', 'melodic_4c_10', 12, 69, 1.0, 80, 'treble', 4, 4), -- A4
('mn_4c10_13', 'melodic_4c_10', 13, 67, 1.0, 80, 'treble', 4, 4), -- G4
('mn_4c10_14', 'melodic_4c_10', 14, 65, 1.0, 80, 'treble', 4, 4), -- F4
('mn_4c10_15', 'melodic_4c_10', 15, 64, 1.0, 80, 'treble', 4, 4), -- E4
('mn_4c10_16', 'melodic_4c_10', 16, 60, 1.0, 80, 'treble', 4, 4); -- C4

-- =========================================================
-- TABLE CREATION (run this first if table doesn't exist)
-- =========================================================

CREATE TABLE IF NOT EXISTS melodic_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exercise_id UUID NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    sequence_order INTEGER NOT NULL,
    pitch INTEGER NOT NULL DEFAULT 60,
    duration_beats REAL NOT NULL DEFAULT 1.0,
    tempo INTEGER NOT NULL DEFAULT 80,
    clef TEXT NOT NULL DEFAULT 'treble',
    time_signature_num INTEGER NOT NULL DEFAULT 4,
    time_signature_den INTEGER NOT NULL DEFAULT 4,
    accidental TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_melodic_notes_exercise 
ON melodic_notes(exercise_id, sequence_order);
