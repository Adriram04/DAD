INSERT INTO zona (id, nombre, geom) VALUES
( 1,'Zona 1',
 '[[37.32,-6.07],[37.356667,-6.07],[37.356667,-6.03125],[37.32,-6.03125],[37.32,-6.07]]'),
( 2,'Zona 2',
 '[[37.32,-6.03125],[37.356667,-6.03125],[37.356667,-5.9925],[37.32,-5.9925],[37.32,-6.03125]]'),
( 3,'Zona 3',
 '[[37.32,-5.9925],[37.356667,-5.9925],[37.356667,-5.95375],[37.32,-5.95375],[37.32,-5.9925]]'),
( 4,'Zona 4',
 '[[37.32,-5.95375],[37.356667,-5.95375],[37.356667,-5.915],[37.32,-5.915],[37.32,-5.95375]]'),
( 5,'Zona 5',
 '[[37.356667,-6.07],[37.393333,-6.07],[37.393333,-6.03125],[37.356667,-6.03125],[37.356667,-6.07]]'),
( 6,'Zona 6',
 '[[37.356667,-6.03125],[37.393333,-6.03125],[37.393333,-5.9925],[37.356667,-5.9925],[37.356667,-6.03125]]'),
( 7,'Zona 7',
 '[[37.356667,-5.9925],[37.393333,-5.9925],[37.393333,-5.95375],[37.356667,-5.95375],[37.356667,-5.9925]]'),
( 8,'Zona 8',
 '[[37.356667,-5.95375],[37.393333,-5.95375],[37.393333,-5.915],[37.356667,-5.915],[37.356667,-5.95375]]'),
( 9,'Zona 9',
 '[[37.393333,-6.07],[37.43,-6.07],[37.43,-6.03125],[37.393333,-6.03125],[37.393333,-6.07]]'),
(10,'Zona 10',
 '[[37.393333,-6.03125],[37.43,-6.03125],[37.43,-5.9925],[37.393333,-5.9925],[37.393333,-6.03125]]'),
(11,'Zona 11',
 '[[37.393333,-5.9925],[37.43,-5.9925],[37.43,-5.95375],[37.393333,-5.95375],[37.393333,-5.9925]]'),
(12,'Zona 12',
 '[[37.393333,-5.95375],[37.43,-5.95375],[37.43,-5.915],[37.393333,-5.915],[37.393333,-5.95375]]')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), geom = VALUES(geom);

-- CONTENEDORES

INSERT INTO contenedor(id, nombre,           lat,        lon,        id_zona, capacidad_maxima, carga_actual, lleno, bloqueo)
VALUES
( 1, 'Contenedor A', 37.338333, -6.050625, 1, 100,  0, 0, 0),
( 2, 'Contenedor B', 37.338333, -6.011875, 2, 100,  0, 0, 0),
( 8, 'Contenedor C', 37.338333, -5.973125, 3, 100, 80, 0, 0),
(10, 'Contenedor D', 37.338333, -5.934375, 4,  45,  0, 0, 0),
(11, 'Contenedor E', 37.375000, -6.050625, 5,  45,  0, 0, 0),
(12, 'Contenedor F', 37.375000, -6.011875, 6,  80,  0, 0, 0),
(13, 'Contenedor G', 37.350000, -6.000000, 2,  50,  0, 0, 0),
(15, 'Contenedor H', 37.380252, -5.988544, 7,  90,  0, 0, 0),
(16, 'Contenedor I', 37.363670, -5.961734, 7,  80,  0, 0, 0),
(17, 'Contenedor J', 37.329273, -6.059947, 1,   5,  0, 0, 0),
(18, 'Contenedor K', 37.381611, -5.967152, 7,  50,  0, 0, 0);


-- PLACAS BASE
INSERT INTO placa_base (modelo, direccion_mac, id_contenedor) VALUES
('ESP32', '00:1A:7D:DA:71:13', 1),
('ESP32', '00:1A:7D:DA:71:14', 2);


-- ASIGNAR ZONAS A BASURERO
INSERT INTO usuario_zona (id_usuario, id_zona) VALUES
(4, 1); -- María en Barrio Norte

-- TARJETAS
INSERT INTO tarjeta (uid, id_consumidor) VALUES
('b3def54b', 12); -- Tarjeta de Juan

-- SENSORES
INSERT INTO sensor (tipo, identificador, id_contenedor) VALUES
('peso', 'peso_01', 1),
('tipo', 'tipo_01', 1);

-- VALORES DE SENSOR
INSERT INTO sensor_value (id_sensor, valor) VALUES
(1, 2.5), -- 2.5 kg
(2, 1);   -- 1 = plástico

-- ACTUADORES
INSERT INTO actuador (tipo, identificador, id_contenedor) VALUES
('rele', 'rele_01', 1);

-- ESTADO DE ACTUADOR
INSERT INTO actuator_state (id_actuador, estado) VALUES
(1, true);

-- PRODUCTOS
INSERT INTO producto (nombre, puntos_necesarios, id_proveedor) VALUES
('Eco-Bolsa', 100, 3),
('Cupón 10% Descuento', 150, 3);

-- REGISTRO DE RECICLAJE
INSERT INTO registro_reciclaje
      (id_consumidor, id_contenedor, qr,          tipo_basura, peso_kg, puntos_obtenidos)
VALUES(2,              1,            'QR-BOLSA-01','PLASTICO',  2.50,    5)
ON DUPLICATE KEY UPDATE peso_kg = VALUES(peso_kg);

