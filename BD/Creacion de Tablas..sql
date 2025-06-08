-- CREACIÓN DE TABLAS

CREATE TABLE zona (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    geom         JSON NOT NULL
);

CREATE TABLE contenedor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    id_zona INT,
    capacidad_maxima FLOAT DEFAULT 100,
    carga_actual FLOAT DEFAULT 0,
    lleno BOOLEAN DEFAULT FALSE,
    lat              DECIMAL(9,6) NOT NULL,
    lon              DECIMAL(9,6) NOT NULL,
    capacidad_maxima FLOAT        NOT NULL,
    carga_actual     FLOAT        NOT NULL DEFAULT 0,
    lleno            TINYINT(1)   NOT NULL DEFAULT 0,
    bloqueo          TINYINT(1)   NOT NULL DEFAULT 0,
    FOREIGN KEY (id_zona) REFERENCES zona(id)
);


CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    usuario VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol ENUM('ADMINISTRADOR', 'CONSUMIDOR', 'PROVEEDOR', 'BASURERO') NOT NULL
	 puntos       INT           NOT NULL DEFAULT 0
);

CREATE TABLE tarjeta (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(100) NOT NULL UNIQUE,
    id_consumidor INT UNIQUE,
    FOREIGN KEY (id_consumidor) REFERENCES usuario (id)
      ON DELETE CASCADE
);


CREATE TABLE producto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100),
    puntos_necesarios INT,
    id_proveedor INT,
    FOREIGN KEY (id_proveedor) REFERENCES usuario(id)
);

CREATE TABLE registro_reciclaje (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_consumidor INT,
    id_contenedor INT,
    qr VARCHAR(120) NOT NULL,
    tipo_basura VARCHAR(50),
    peso_kg FLOAT,
    puntos_obtenidos INT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_consumidor) REFERENCES usuario(id),
    FOREIGN KEY (id_contenedor) REFERENCES contenedor(id)
);

CREATE TABLE usuario_zona (
    id_usuario INT,
    id_zona INT,
    PRIMARY KEY (id_usuario, id_zona),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id),
    FOREIGN KEY (id_zona) REFERENCES zona(id)
);
