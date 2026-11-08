"""Contrato de datos del catálogo: columnas esperadas y límites de la base de datos."""

# Campos de la tabla modelo
CAMPOS_MODELO = ("marca", "nombre")

# Campos de texto libre de la tabla ficha_modelo
CAMPOS_FICHA = ("consumo", "mantenimiento", "fallas_comunes", "repuestos", "que_revisar")

# Columnas que debe tener el archivo ya limpio, en este orden
COLUMNAS = CAMPOS_MODELO + CAMPOS_FICHA

# Límites de longitud de modelo (deben coincidir con V10__catalogo.sql)
LIMITES = {"marca": 60, "nombre": 100}
