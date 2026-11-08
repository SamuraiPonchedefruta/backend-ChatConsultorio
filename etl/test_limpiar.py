import pandas as pd
import pytest

from esquema import LIMITES
from limpiar import limpiar_catalogo, main


def _fila(**cambios):
    base = {
        "marca": "Toyota",
        "nombre": "Yaris",
        "consumo": "ok",
        "mantenimiento": "ok",
        "fallas_comunes": "ok",
        "repuestos": "ok",
        "que_revisar": "ok",
    }
    base.update(cambios)
    return base


def _df(*filas):
    return pd.DataFrame(list(filas))


def test_normaliza_espacios_y_unicode():
    resultado = limpiar_catalogo(_df(_fila(marca="  Citro\u0065\u0308n  ", nombre="  C4   Cactus ")))
    fila = resultado.validos.iloc[0]
    assert fila["marca"] == "Citro\u00ebn"
    assert fila["nombre"] == "C4 Cactus"


def test_parrafos_conservan_saltos_de_linea():
    resultado = limpiar_catalogo(_df(_fila(consumo="a  b\r\n\r\n c ")))
    assert resultado.validos.iloc[0]["consumo"] == "a b\nc"


def test_campos_de_ficha_vacios_pasan_a_nulo():
    resultado = limpiar_catalogo(_df(_fila(consumo="   ")))
    assert pd.isna(resultado.validos.iloc[0]["consumo"])


def test_marca_vacia_va_a_invalidos():
    resultado = limpiar_catalogo(_df(_fila(marca="  ")))
    assert resultado.validos.empty
    assert len(resultado.invalidos) == 1
    assert "marca" in resultado.invalidos[0].motivo


def test_limite_exacto_es_valido_y_uno_mas_no():
    exacto = _fila(nombre="A" * LIMITES["nombre"])
    excedido = _fila(nombre="B" * (LIMITES["nombre"] + 1))
    resultado = limpiar_catalogo(_df(exacto, excedido))
    assert len(resultado.validos) == 1
    assert len(resultado.invalidos) == 1
    assert "nombre" in resultado.invalidos[0].motivo


def test_duplicado_ignora_mayusculas_y_reporta_la_fila():
    resultado = limpiar_catalogo(_df(_fila(), _fila(marca="TOYOTA", nombre="yaris")))
    assert len(resultado.validos) == 1
    assert resultado.invalidos[0].fila == 3  # encabezado = línea 1, segundo dato = línea 3
    assert "duplicado" in resultado.invalidos[0].motivo


def test_columnas_faltantes_lanzan_error():
    with pytest.raises(ValueError, match="consumo"):
        limpiar_catalogo(_df(_fila()).drop(columns=["consumo"]))


def test_nombres_de_columna_tolerantes():
    df = _df(_fila()).rename(columns={"marca": " Marca ", "fallas_comunes": "Fallas Comunes"})
    assert len(limpiar_catalogo(df).validos) == 1


def test_limpiar_dos_veces_da_lo_mismo():
    primero = limpiar_catalogo(_df(_fila(), _fila(nombre="Corolla", consumo="  ")))
    segundo = limpiar_catalogo(primero.validos)
    pd.testing.assert_frame_equal(primero.validos, segundo.validos)


def test_main_escribe_salida_y_log(tmp_path):
    entrada = tmp_path / "catalogo.csv"
    entrada.write_text(
        "marca,nombre,consumo,mantenimiento,fallas_comunes,repuestos,que_revisar\n"
        "Toyota,Yaris,a,b,c,d,e\n"
        ",Sin marca,a,b,c,d,e\n",
        encoding="utf-8-sig",
    )
    log = tmp_path / "invalidos.log"
    salida = tmp_path / "limpio.csv"

    codigo = main([str(entrada), "--log", str(log), "--salida", str(salida)])

    assert codigo == 0
    assert "marca vacía" in log.read_text(encoding="utf-8")
    assert len(pd.read_csv(salida)) == 1


def test_main_con_archivo_inexistente_devuelve_error(tmp_path, capsys):
    assert main([str(tmp_path / "no_existe.csv")]) == 1
    assert "Error" in capsys.readouterr().err
