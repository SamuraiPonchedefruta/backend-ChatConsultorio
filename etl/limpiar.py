"""Limpieza y normalización del catálogo de modelos (issue E2-05-B).

Lee el archivo fuente, normaliza el texto, valida contra los límites de la base de datos
y separa los registros inválidos en un log. No toca la base de datos.

Uso:
    python etl/limpiar.py data/catalogo_ejemplo.csv --salida data/catalogo_limpio.csv
"""
from __future__ import annotations

import argparse
import re
import sys
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path

import pandas as pd

from esquema import CAMPOS_FICHA, CAMPOS_MODELO, COLUMNAS, LIMITES

RUTA_LOG_POR_DEFECTO = Path("data/invalidos.log")
LARGO_MAXIMO_EN_LOG = 50


@dataclass(frozen=True)
class RegistroInvalido:
    fila: int  # número de línea en el archivo (el encabezado es la línea 1)
    motivo: str
    marca: str
    nombre: str


@dataclass
class ResultadoLimpieza:
    validos: pd.DataFrame
    invalidos: list[RegistroInvalido] = field(default_factory=list)


def _es_vacio(valor) -> bool:
    return valor is None or (not isinstance(valor, str) and pd.isna(valor))


def _normalizar_linea(valor) -> str:
    """Texto de una sola línea: Unicode NFC y espacios colapsados."""
    if _es_vacio(valor):
        return ""
    texto = unicodedata.normalize("NFC", str(valor))
    return re.sub(r"\s+", " ", texto).strip()


def _normalizar_parrafo(valor) -> str:
    """Texto de varias líneas: conserva los saltos de línea y elimina las líneas vacías."""
    if _es_vacio(valor):
        return ""
    texto = unicodedata.normalize("NFC", str(valor)).replace("\r\n", "\n").replace("\r", "\n")
    lineas = (re.sub(r"[^\S\n]+", " ", linea).strip() for linea in texto.split("\n"))
    return "\n".join(linea for linea in lineas if linea)


def _normalizar_columnas(df: pd.DataFrame) -> pd.DataFrame:
    return df.rename(columns=lambda c: _normalizar_linea(c).lower().replace(" ", "_"))


def _motivo_invalido(registro: dict[str, str], vistos: set[tuple[str, str]]) -> str | None:
    if not registro["marca"]:
        return "marca vacía"
    if not registro["nombre"]:
        return "nombre vacío"
    for campo in CAMPOS_MODELO:
        if len(registro[campo]) > LIMITES[campo]:
            return f"{campo} excede {LIMITES[campo]} caracteres"
    if (registro["marca"].casefold(), registro["nombre"].casefold()) in vistos:
        return "duplicado (misma marca y nombre)"
    return None


def limpiar_catalogo(df: pd.DataFrame) -> ResultadoLimpieza:
    """Normaliza y valida el catálogo. Devuelve los registros válidos y los inválidos."""
    df = _normalizar_columnas(df)
    faltantes = [c for c in COLUMNAS if c not in df.columns]
    if faltantes:
        raise ValueError(f"Faltan columnas en el archivo: {', '.join(faltantes)}")

    validos: list[dict[str, str | None]] = []
    invalidos: list[RegistroInvalido] = []
    vistos: set[tuple[str, str]] = set()

    for fila, datos in enumerate(df[list(COLUMNAS)].to_dict("records"), start=2):
        registro = {
            campo: (_normalizar_linea if campo in CAMPOS_MODELO else _normalizar_parrafo)(datos[campo])
            for campo in COLUMNAS
        }
        motivo = _motivo_invalido(registro, vistos)
        if motivo:
            invalidos.append(RegistroInvalido(fila, motivo, registro["marca"], registro["nombre"]))
            continue
        vistos.add((registro["marca"].casefold(), registro["nombre"].casefold()))
        for campo in CAMPOS_FICHA:
            registro[campo] = registro[campo] or None
        validos.append(registro)

    return ResultadoLimpieza(pd.DataFrame(validos, columns=list(COLUMNAS)), invalidos)


def leer_catalogo(ruta: Path) -> pd.DataFrame:
    ruta = Path(ruta)
    if ruta.suffix.lower() != ".csv":
        raise ValueError(f"Formato no soportado: {ruta.suffix or 'sin extensión'} (se espera .csv)")
    # dtype=str y keep_default_na=False evitan que "NA" o celdas vacías se conviertan en NaN;
    # utf-8-sig tolera el BOM que agrega Excel al guardar CSV.
    return pd.read_csv(ruta, dtype=str, keep_default_na=False, encoding="utf-8-sig")


def _recortar(texto: str) -> str:
    return texto if len(texto) <= LARGO_MAXIMO_EN_LOG else texto[:LARGO_MAXIMO_EN_LOG] + "…"


def escribir_log_invalidos(invalidos: list[RegistroInvalido], ruta: Path) -> None:
    """Escribe el log de la última corrida (se sobrescribe, no se acumula)."""
    ruta = Path(ruta)
    ruta.parent.mkdir(parents=True, exist_ok=True)
    lineas = [
        f"fila {i.fila}: {i.motivo} (marca={_recortar(i.marca)!r}, nombre={_recortar(i.nombre)!r})"
        for i in invalidos
    ]
    ruta.write_text("\n".join(lineas) + ("\n" if lineas else ""), encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Limpia y valida el catálogo de modelos.")
    parser.add_argument("entrada", type=Path, help="archivo CSV fuente")
    parser.add_argument("--salida", type=Path, default=None, help="CSV con los registros válidos (opcional)")
    parser.add_argument("--log", type=Path, default=RUTA_LOG_POR_DEFECTO, help="log de registros inválidos")
    args = parser.parse_args(argv)

    try:
        resultado = limpiar_catalogo(leer_catalogo(args.entrada))
    except (FileNotFoundError, ValueError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1

    escribir_log_invalidos(resultado.invalidos, args.log)
    if args.salida:
        args.salida.parent.mkdir(parents=True, exist_ok=True)
        resultado.validos.to_csv(args.salida, index=False, encoding="utf-8")

    print(
        f"Registros válidos: {len(resultado.validos)} | "
        f"inválidos: {len(resultado.invalidos)} (log: {args.log})"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
