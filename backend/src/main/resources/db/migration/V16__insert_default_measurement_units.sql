
INSERT INTO measurement_units (symbol, name) VALUES
-- Concentration
('mg/dL', 'Milligrams per deciliter'),
('g/dL', 'Grams per deciliter'),
('mg/L', 'Milligrams per liter'),
('g/L', 'Grams per liter'),
('µg/L', 'Micrograms per liter'),
('ng/mL', 'Nanograms per milliliter'),
('pg/mL', 'Picograms per milliliter'),
('mmol/L', 'Millimoles per liter'),
('µmol/L', 'Micromoles per liter'),
('mEq/L', 'Milliequivalents per liter'),

-- Hormones and enzymes
('mIU/L', 'Milli-international units per liter'),
('µIU/mL', 'Micro-international units per milliliter'),
('IU/L', 'International units per liter'),
('U/L', 'Units per liter'),
('mU/L', 'Milliunits per liter'),

-- Blood count
('10^3/µL', 'Thousands per microliter'),
('10^6/µL', 'Millions per microliter'),
('10^9/L', 'Billions per liter'),
('10^12/L', 'Trillions per liter'),
('fL', 'Femtoliters'),
('pg', 'Picograms'),

-- Percentages and ratios
('%', 'Percent'),
('ratio', 'Ratio'),
('INR', 'International normalized ratio'),

-- Urine and other measurements
('mg/24h', 'Milligrams per 24 hours'),
('g/24h', 'Grams per 24 hours'),
('mL', 'Milliliters'),
('mL/min', 'Milliliters per minute'),
('mL/min/1.73m²', 'Milliliters per minute per 1.73 square meters'),
('mm/h', 'Millimeters per hour'),
('mOsm/kg', 'Milliosmoles per kilogram'),
('copies/mL', 'Copies per milliliter'),
('CFU/mL', 'Colony-forming units per milliliter'),
('cells/µL', 'Cells per microliter'),
('ng/dL', 'Nanograms per deciliter'),
('pmol/L', 'Picomoles per liter')
ON CONFLICT DO NOTHING;
