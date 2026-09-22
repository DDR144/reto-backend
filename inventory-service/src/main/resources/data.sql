-- Datos semilla del inventario (idempotente).
INSERT INTO products (product_id, name, description, stock, updated_at) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Laptop Pro 14', 'Laptop profesional de 14 pulgadas', 10, NOW()),
    ('00000000-0000-0000-0000-000000000002', 'Monitor 27 4K', 'Monitor 4K de 27 pulgadas', 5, NOW()),
    ('00000000-0000-0000-0000-000000000003', 'Teclado mecánico', 'Teclado mecánico RGB', 0, NOW())
ON CONFLICT (product_id) DO NOTHING;
