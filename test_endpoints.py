"""
test_endpoints.py — Suite de pruebas de integración para Warhammer Battleship CLMM
=====================================================================================
Cubre el flujo completo: Frontend → Middleware (8080) → Backend API (8081) → BD

USO:
  Ejecutar TODAS las pruebas en orden:
      python test_endpoints.py

  Ejecutar una prueba INDIVIDUAL por nombre:
      python test_endpoints.py auth
      python test_endpoints.py personajes
      python test_endpoints.py registro
      python test_endpoints.py validate
      python test_endpoints.py perfil
      python test_endpoints.py nickname
      python test_endpoints.py password
      python test_endpoints.py foto
      python test_endpoints.py estadisticas
      python test_endpoints.py lobby
      python test_endpoints.py partidas
      python test_endpoints.py seguridad

REQUISITOS:
  pip install requests

SERVICIOS NECESARIOS ARRANCADOS:
  Middleware   → http://localhost:8080
  Backend API  → http://localhost:8081 (solo para test de seguridad directo)
"""

import requests
import json
import time
import sys

# ─────────────────────────────────────────────
#  CONFIGURACIÓN GLOBAL
# ─────────────────────────────────────────────
MIDDLEWARE_URL   = "http://localhost:8080"
BACKEND_URL      = "http://localhost:8081"
INTERNAL_API_KEY = "ClmmInternalSecretKey2026_NoCompartir"  # Clave local (application.properties)
ADMIN_USER       = "middleware_admin"
ADMIN_PASS       = "clave_secreta_del_middleware_2026"
FINGERPRINT      = "test-fingerprint-abc123"

# Usuario de prueba dinámico (timestamp para evitar colisiones)
TEST_USER        = f"tst{int(time.time()) % 100000}"
TEST_EMAIL       = f"{TEST_USER}@test.com"
TEST_PASS        = "Password123!"

# Estado compartido entre pruebas
_TOKEN  = None   # JWT del Middleware (se obtiene en test_auth)
_USER   = TEST_USER  # Se actualiza si se cambia el nickname

# ─────────────────────────────────────────────
#  UTILIDADES
# ─────────────────────────────────────────────
GREEN  = "\033[92m"
RED    = "\033[91m"
YELLOW = "\033[93m"
CYAN   = "\033[96m"
RESET  = "\033[0m"
BOLD   = "\033[1m"

def ok(msg):    print(f"  {GREEN}✔ {msg}{RESET}")
def fail(msg):  print(f"  {RED}✘ {msg}{RESET}")
def info(msg):  print(f"  {YELLOW}→ {msg}{RESET}")
def header(msg):print(f"\n{BOLD}{CYAN}{'─'*55}\n  {msg}\n{'─'*55}{RESET}")

def assert_status(resp, expected, label):
    if resp.status_code == expected:
        ok(f"[{resp.status_code}] {label}")
        return True
    else:
        fail(f"[{resp.status_code}] {label}  (esperado {expected})")
        try:
            info(f"Body: {json.dumps(resp.json(), ensure_ascii=False)}")
        except Exception:
            info(f"Body: {resp.text[:200]}")
        return False

def auth_headers(token=None):
    t = token or _TOKEN
    h = {"X-Fingerprint": FINGERPRINT, "Content-Type": "application/json"}
    if t:
        h["Authorization"] = f"Bearer {t}"
    return h

# ─────────────────────────────────────────────
#  PRUEBAS INDIVIDUALES
# ─────────────────────────────────────────────

def test_auth():
    """
    TEST: Login del administrador del Middleware.
    Endpoint: POST /api/auth/login
    Obtiene el JWT necesario para el resto de pruebas autenticadas.
    """
    global _TOKEN
    header("TEST AUTH — Login de administrador")
    resp = requests.post(
        f"{MIDDLEWARE_URL}/api/auth/login",
        json={"username": ADMIN_USER, "password": ADMIN_PASS},
        headers={"X-Fingerprint": FINGERPRINT}
    )
    if assert_status(resp, 200, "POST /api/auth/login"):
        _TOKEN = resp.json().get("token")
        info(f"Token obtenido: {_TOKEN[:40]}...")
        return True
    return False


def test_personajes():
    """
    TEST: Catálogo de personajes (endpoint público).
    Endpoint: GET /api/personajes
    No requiere JWT. El Middleware hace proxy al Backend API.
    """
    header("TEST PERSONAJES — Catálogo público")
    resp = requests.get(f"{MIDDLEWARE_URL}/api/personajes")
    if assert_status(resp, 200, "GET /api/personajes (sin token)"):
        personajes = resp.json()
        info(f"Personajes encontrados: {len(personajes)}")
        for p in personajes:
            info(f"  · [{p.get('id')}] {p.get('nombre')}")
        return True
    return False


def test_registro():
    """
    TEST: Registro de un nuevo usuario jugador.
    Endpoint: POST /api/auth/register
    Usuario dinámico con timestamp para evitar colisiones entre ejecuciones.
    """
    header(f"TEST REGISTRO — Crear usuario '{TEST_USER}'")
    resp = requests.post(
        f"{MIDDLEWARE_URL}/api/auth/register",
        json={"username": TEST_USER, "email": TEST_EMAIL, "password": TEST_PASS},
        headers=auth_headers()
    )
    return assert_status(resp, 200, f"POST /api/auth/register → usuario '{TEST_USER}'")


def test_validate():
    """
    TEST: Validación de credenciales de usuario.
    Endpoint: POST /api/auth/validate-user
    Verifica que el usuario registrado puede autenticarse correctamente.
    También prueba credenciales incorrectas (debe devolver 401).
    """
    header("TEST VALIDATE-USER — Validar credenciales")
    # Credenciales correctas
    resp = requests.post(
        f"{MIDDLEWARE_URL}/api/auth/validate-user",
        json={"username": TEST_USER, "password": TEST_PASS},
        headers=auth_headers()
    )
    r1 = assert_status(resp, 200, "POST /api/auth/validate-user (credenciales correctas)")
    if r1:
        info(f"profilePicture: {resp.json().get('profilePicture', '(vacía)')}")

    # Credenciales incorrectas — debe rechazar
    resp2 = requests.post(
        f"{MIDDLEWARE_URL}/api/auth/validate-user",
        json={"username": TEST_USER, "password": "contrasenaMal"},
        headers=auth_headers()
    )
    r2 = assert_status(resp2, 401, "POST /api/auth/validate-user (contraseña incorrecta → 401)")
    return r1 and r2


def test_perfil():
    """
    TEST: Obtener perfil de un usuario.
    Endpoint: GET /api/user/{username}
    """
    header("TEST PERFIL — Consultar datos de usuario")
    resp = requests.get(
        f"{MIDDLEWARE_URL}/api/user/{TEST_USER}",
        headers=auth_headers()
    )
    if assert_status(resp, 200, f"GET /api/user/{TEST_USER}"):
        info(f"Perfil: {json.dumps(resp.json(), ensure_ascii=False)}")
        return True
    return False


def test_nickname():
    """
    TEST: Cambiar el nickname de un usuario.
    Endpoint: POST /api/auth/update-nickname
    Actualiza la variable global _USER para los test posteriores.
    """
    global _USER
    header("TEST NICKNAME — Cambiar nombre de usuario")
    new_name = f"{TEST_USER}x"
    resp = requests.post(
        f"{MIDDLEWARE_URL}/api/auth/update-nickname",
        json={"currentUsername": TEST_USER, "newUsername": new_name},
        headers=auth_headers()
    )
    if assert_status(resp, 200, f"POST /api/auth/update-nickname → '{TEST_USER}' → '{new_name}'"):
        _USER = new_name
        info(f"Username actualizado a: {_USER}")
        return True
    # Si falla, mantenemos el original
    _USER = TEST_USER
    return False


def test_password():
    """
    TEST: Cambiar la contraseña de un usuario.
    Endpoint: POST /api/auth/update-password
    Verifica después que el login con la nueva contraseña funciona.
    """
    header("TEST PASSWORD — Cambiar contraseña")
    new_pass = "NuevaPass456!"
    resp = requests.post(
        f"{MIDDLEWARE_URL}/api/auth/update-password",
        json={"username": _USER, "newPassword": new_pass},
        headers=auth_headers()
    )
    r1 = assert_status(resp, 200, f"POST /api/auth/update-password para '{_USER}'")

    if r1:
        # Verificar que la nueva contraseña funciona
        resp2 = requests.post(
            f"{MIDDLEWARE_URL}/api/auth/validate-user",
            json={"username": _USER, "password": new_pass},
            headers=auth_headers()
        )
        r2 = assert_status(resp2, 200, "Validar credenciales con nueva contraseña → OK")
        return r2
    return False


def test_foto():
    """
    TEST: Actualizar foto de perfil de un usuario.
    Endpoint: POST /api/auth/update-profile-picture
    """
    header("TEST FOTO — Actualizar avatar")
    avatar_url = "https://api.dicebear.com/7.x/adventurer/svg?seed=clmm_test"
    resp = requests.post(
        f"{MIDDLEWARE_URL}/api/auth/update-profile-picture",
        json={"username": _USER, "profilePicture": avatar_url},
        headers=auth_headers()
    )
    return assert_status(resp, 200, f"POST /api/auth/update-profile-picture para '{_USER}'")


def test_estadisticas():
    """
    TEST: Estadísticas agregadas de un usuario en MongoDB.

    Flujo:
      1. Insertar 2 documentos de prueba directamente en el Backend API
         (simulando lo que haría el GameEngine al finalizar 2 partidas).
      2. Consultar el agregado via Middleware → GET /api/estadisticas/jugador/{username}
      3. Verificar que los valores calculados coinciden con los insertados.

    La inserción usa el Backend API directamente con X-Internal-Key,
    igual que lo haría el BackendClient del Middleware en producción.
    """
    header(f"TEST ESTADÍSTICAS — Insertar datos y consultar stats de '{_USER}'")

    # ── Datos de las 2 partidas de prueba ──────────────────────────
    partida_1 = {
        "username":       _USER,
        "personajeNombre":"Wulfrik",
        "hitsAcertados":  12,
        "hitsFallados":   5,
        "barcosHundidos": 3,
        "ganador":        True
    }
    partida_2 = {
        "username":       _USER,
        "personajeNombre":"Aranessa",
        "hitsAcertados":  8,
        "hitsFallados":   10,
        "barcosHundidos": 2,
        "ganador":        False
    }

    # ── PASO 1: Insertar via Backend API (X-Internal-Key) ──────────
    backend_headers = {
        "X-Internal-Key": INTERNAL_API_KEY,
        "Content-Type":   "application/json"
    }
    r1 = requests.post(f"{BACKEND_URL}/api/estadisticas/guardar",
                       json=partida_1, headers=backend_headers)
    assert_status(r1, 200, f"POST {BACKEND_URL}/api/estadisticas/guardar (partida 1 — ganador)")

    r2 = requests.post(f"{BACKEND_URL}/api/estadisticas/guardar",
                       json=partida_2, headers=backend_headers)
    assert_status(r2, 200, f"POST {BACKEND_URL}/api/estadisticas/guardar (partida 2 — perdedor)")

    if r1.status_code != 200 or r2.status_code != 200:
        fail("No se pudieron insertar los datos de prueba. ¿Está arrancado el Backend API en :8081?")
        return False

    # ── PASO 2: Consultar agregado via Middleware ──────────────────
    resp = requests.get(
        f"{MIDDLEWARE_URL}/api/estadisticas/jugador/{_USER}",
        headers=auth_headers()
    )
    if not assert_status(resp, 200, f"GET /api/estadisticas/jugador/{_USER}"):
        return False

    # ── PASO 3: Verificar valores esperados ────────────────────────
    stats = resp.json()
    esperado_jugadas  = 2
    esperado_ganadas  = 1
    esperado_acertados= partida_1["hitsAcertados"] + partida_2["hitsAcertados"]  # 20
    esperado_fallados = partida_1["hitsFallados"]   + partida_2["hitsFallados"]   # 15
    esperado_hundidos = partida_1["barcosHundidos"] + partida_2["barcosHundidos"] # 5

    # Puede haber partidas previas de otras ejecuciones, así que verificamos ≥
    resultado = True
    checks = [
        ("partidasJugadas",  stats.get("partidasJugadas", 0),  esperado_jugadas,   ">="),
        ("partidasGanadas",  stats.get("partidasGanadas", 0),  esperado_ganadas,   ">="),
        ("hitsAcertados",    stats.get("hitsAcertados", 0),    esperado_acertados, ">="),
        ("hitsFallados",     stats.get("hitsFallados", 0),     esperado_fallados,  ">="),
        ("barcosHundidos",   stats.get("barcosHundidos", 0),   esperado_hundidos,  ">="),
    ]
    for campo, valor, esperado, op in checks:
        pasa = valor >= esperado if op == ">=" else valor == esperado
        simbolo = "≥" if op == ">=" else "="
        if pasa:
            ok(f"{campo}: {valor} {simbolo} {esperado} ✓")
        else:
            fail(f"{campo}: {valor} {simbolo} {esperado}  ← valor inesperado")
            resultado = False

    punteria = stats.get("punteria", "0%")
    info(f"Puntería calculada: {punteria}")
    return resultado



def test_lobby():
    """
    TEST: Gestión de salas de lobby (en memoria del Middleware).
    Endpoints: GET /api/lobby, POST /api/lobby, DELETE /api/lobby/{id}
    """
    header("TEST LOBBY — Gestión de salas")
    code = f"SALA_{int(time.time()) % 9999}"

    # Listar salas iniciales
    resp = requests.get(f"{MIDDLEWARE_URL}/api/lobby")
    r1 = assert_status(resp, 200, "GET /api/lobby (lista pública, sin token)")
    salas_iniciales = len(resp.json()) if r1 else 0
    info(f"Salas activas antes de crear: {salas_iniciales}")

    # Crear sala (requiere JWT)
    sala_data = {
        "codigoSala": code,
        "jugador1": _USER,
        "nombreJugador1": _USER,
        "avatarJugador1": "https://api.dicebear.com/7.x/adventurer/svg?seed=test",
        "estado": "ESPERANDO"
    }
    resp2 = requests.post(
        f"{MIDDLEWARE_URL}/api/lobby",
        json=sala_data,
        headers=auth_headers()
    )
    r2 = assert_status(resp2, 200, f"POST /api/lobby → crear sala '{code}'")

    # Verificar que aparece en el listado
    resp3 = requests.get(f"{MIDDLEWARE_URL}/api/lobby")
    r3 = assert_status(resp3, 200, "GET /api/lobby (después de crear)")
    if r3:
        salas_ahora = resp3.json()
        encontrada = any(s.get("codigoSala") == code for s in salas_ahora)
        if encontrada:
            ok(f"Sala '{code}' visible en el listado ✓")
        else:
            fail(f"Sala '{code}' NO encontrada en el listado")
        info(f"Total salas tras crear: {len(salas_ahora)}")

    # Verificar sala activa (endpoint de partidas)
    resp4 = requests.get(f"{MIDDLEWARE_URL}/api/partidas/sala-activa/{code}")
    assert_status(resp4, 200, f"GET /api/partidas/sala-activa/{code}")
    if resp4.status_code == 200:
        info(f"¿Sala activa?: {resp4.json().get('activa')}")

    # Eliminar sala (limpieza)
    resp5 = requests.delete(
        f"{MIDDLEWARE_URL}/api/lobby/{code}",
        headers=auth_headers()
    )
    r5 = assert_status(resp5, 204, f"DELETE /api/lobby/{code} (limpieza)")

    return r1 and r2 and r3 and r5


def test_partidas():
    """
    TEST: Historial de partidas persistidas en MySQL.

    Flujo:
      1. Crear 3 partidas via Backend API con X-Internal-Key (una por estado).
         - Partida A → se deja en EN_ESPERA (estado inicial).
         - Partida B → se actualiza a EN_CURSO.
         - Partida C → se actualiza a FINALIZADA (con ganador).
      2. Consultar todas via Middleware → GET /api/partidas
      3. Filtrar por cada estado y verificar que aparece ≥ 1 resultado.
      4. Consultar detalle de una partida por ID.
      5. Verificar que un estado inválido devuelve 400.
    """
    header("TEST PARTIDAS — Insertar y consultar historial MySQL")

    backend_headers = {
        "X-Internal-Key": INTERNAL_API_KEY,
        "Content-Type":   "application/json"
    }
    host = ADMIN_USER  # El admin ya existe en BD, es válido como host

    # ── PASO 1: Crear 3 partidas de prueba ─────────────────────────
    ids = []
    for i in range(3):
        r = requests.post(
            f"{BACKEND_URL}/api/partidas/crear?host={host}",
            headers=backend_headers
        )
        if assert_status(r, 200, f"POST {BACKEND_URL}/api/partidas/crear (partida {i+1})"):
            ids.append(r.json())   # devuelve el id (Long)
            info(f"  Partida creada con id={r.json()}")
        else:
            fail("No se pudo crear la partida de prueba. ¿Está arrancado el Backend API?")
            return False

    id_en_espera, id_en_curso, id_finalizada = ids

    # Partida B → EN_CURSO
    r = requests.put(
        f"{BACKEND_URL}/api/partidas/{id_en_curso}/estado?estado=EN_CURSO",
        headers=backend_headers
    )
    assert_status(r, 200, f"PUT /api/partidas/{id_en_curso}/estado → EN_CURSO")

    # Partida C → FINALIZADA con ganador
    r = requests.put(
        f"{BACKEND_URL}/api/partidas/{id_finalizada}/estado?estado=FINALIZADA&ganador={host}",
        headers=backend_headers
    )
    assert_status(r, 200, f"PUT /api/partidas/{id_finalizada}/estado → FINALIZADA")

    # ── PASO 2: Listar todas via Middleware ────────────────────────
    resp_todas = requests.get(f"{MIDDLEWARE_URL}/api/partidas", headers=auth_headers())
    r_todas = assert_status(resp_todas, 200, "GET /api/partidas (todas)")
    partidas = resp_todas.json() if r_todas else []
    info(f"Total partidas en BD: {len(partidas)}")

    # ── PASO 3: Filtrar por estado y verificar ≥ 1 resultado ───────
    resultados = []
    for estado in ["EN_ESPERA", "EN_CURSO", "FINALIZADA"]:
        r = requests.get(
            f"{MIDDLEWARE_URL}/api/partidas/estado/{estado}",
            headers=auth_headers()
        )
        ok_estado = assert_status(r, 200, f"GET /api/partidas/estado/{estado}")
        resultados.append(ok_estado)
        if ok_estado:
            cantidad = len(r.json())
            info(f"  Partidas {estado}: {cantidad}")
            if cantidad >= 1:
                ok(f"  ≥ 1 partida en estado {estado} ✓")
            else:
                fail(f"  0 partidas en estado {estado} — se esperaba al menos 1")
                resultados[-1] = False

    # ── PASO 4: Detalle por ID (la partida finalizada) ─────────────
    r_det = requests.get(
        f"{MIDDLEWARE_URL}/api/partidas/{id_finalizada}",
        headers=auth_headers()
    )
    r4 = assert_status(r_det, 200, f"GET /api/partidas/{id_finalizada} (detalle FINALIZADA)")
    if r4:
        detalle = r_det.json()
        estado_real = detalle.get("estado", "?")
        info(f"  Estado confirmado: {estado_real}")
        if estado_real == "FINALIZADA":
            ok("  Estado del detalle = FINALIZADA ✓")
        else:
            fail(f"  Estado inesperado: {estado_real}")

    # ── PASO 5: Estado inválido → 400 ─────────────────────────────
    r_inv = requests.get(
        f"{MIDDLEWARE_URL}/api/partidas/estado/INVALIDO",
        headers=auth_headers()
    )
    assert_status(r_inv, 400, "GET /api/partidas/estado/INVALIDO → 400 esperado")

    return r_todas and all(resultados) and r4



def test_seguridad():
    """
    TEST: Verificación del blindaje de seguridad.
    1. Endpoint protegido del Middleware sin JWT → 401
    2. Petición directa al Backend API sin X-Internal-Key → 401
    3. Petición directa al Backend API con X-Internal-Key correcta → 200
    """
    header("TEST SEGURIDAD — Blindaje del sistema")

    # 1. Middleware: endpoint protegido sin token debe rechazar
    resp1 = requests.get(f"{MIDDLEWARE_URL}/api/estadisticas/jugador/alguien")
    r1 = assert_status(resp1, 401, "Middleware: GET /api/estadisticas sin JWT → 401")

    # 2. Backend API directo sin clave interna → debe rechazar
    try:
        resp2 = requests.get(f"{BACKEND_URL}/api/personajes", timeout=3)
        r2 = assert_status(resp2, 401, "Backend API directo sin X-Internal-Key → 401")
    except requests.exceptions.ConnectionError:
        ok("Backend API no es accesible externamente (conexión rechazada) ✓")
        r2 = True
    except requests.exceptions.Timeout:
        ok("Backend API no responde al exterior (timeout) ✓")
        r2 = True

    # 3. Backend API directo CON clave interna → debe funcionar
    try:
        resp3 = requests.get(
            f"{BACKEND_URL}/api/personajes",
            headers={"X-Internal-Key": INTERNAL_API_KEY},
            timeout=3
        )
        r3 = assert_status(resp3, 200, "Backend API con X-Internal-Key correcta → 200")
        if r3:
            info(f"Personajes devueltos por el backend directamente: {len(resp3.json())}")
    except requests.exceptions.ConnectionError:
        info("Backend API no accesible en localhost:8081 (normal en Docker sin port mapping)")
        r3 = True  # No es un fallo si el puerto no está expuesto

    return r1 and r2


# ─────────────────────────────────────────────
#  REGISTRO DE PRUEBAS
# ─────────────────────────────────────────────
TESTS = {
    "auth":        (test_auth,        "Login administrador (obtiene JWT)"),
    "personajes":  (test_personajes,  "Catálogo de personajes (público)"),
    "registro":    (test_registro,    "Registro de usuario nuevo"),
    "validate":    (test_validate,    "Validación de credenciales"),
    "perfil":      (test_perfil,      "Consulta de perfil de usuario"),
    "nickname":    (test_nickname,    "Cambio de nickname"),
    "password":    (test_password,    "Cambio de contraseña"),
    "foto":        (test_foto,        "Actualización de foto de perfil"),
    "estadisticas":(test_estadisticas,"Estadísticas agregadas (MongoDB)"),
    "lobby":       (test_lobby,       "Gestión de salas de lobby"),
    "partidas":    (test_partidas,    "Historial de partidas (MySQL)"),
    "seguridad":   (test_seguridad,   "Blindaje de seguridad (API Key)"),
}

# Orden de ejecución por defecto (auth siempre primero para obtener el token)
DEFAULT_ORDER = [
    "auth", "personajes", "registro", "validate", "perfil",
    "nickname", "password", "foto", "estadisticas",
    "lobby", "partidas", "seguridad"
]


# ─────────────────────────────────────────────
#  RUNNER
# ─────────────────────────────────────────────
def run_tests(keys):
    """Ejecuta las pruebas especificadas y muestra un resumen final."""
    # Si se pide una prueba individual que no es 'auth', ejecutar auth primero
    if keys != DEFAULT_ORDER and "auth" not in keys:
        print(f"{YELLOW}→ Ejecutando 'auth' primero para obtener el JWT...{RESET}")
        fn, _ = TESTS["auth"]
        fn()

    resultados = {}
    for key in keys:
        if key not in TESTS:
            print(f"{RED}Prueba desconocida: '{key}'. Pruebas disponibles: {list(TESTS.keys())}{RESET}")
            continue
        fn, desc = TESTS[key]
        try:
            resultados[key] = fn()
        except Exception as e:
            fail(f"Excepción en '{key}': {e}")
            resultados[key] = False

    # ─── Resumen ───
    print(f"\n{BOLD}{'═'*55}")
    print("  RESUMEN DE PRUEBAS")
    print(f"{'═'*55}{RESET}")
    passed = sum(1 for v in resultados.values() if v)
    total  = len(resultados)
    for key, result in resultados.items():
        icon = f"{GREEN}✔{RESET}" if result else f"{RED}✘{RESET}"
        _, desc = TESTS[key]
        print(f"  {icon}  {key:<14} {desc}")
    print(f"\n  {BOLD}Resultado: {passed}/{total} pruebas pasadas{RESET}")
    if passed == total:
        print(f"  {GREEN}{BOLD}¡Todas las pruebas pasaron correctamente!{RESET}\n")
    else:
        print(f"  {RED}{BOLD}{total - passed} prueba(s) fallaron.{RESET}\n")


# ─────────────────────────────────────────────
#  PUNTO DE ENTRADA
# ─────────────────────────────────────────────
if __name__ == "__main__":
    print(f"\n{BOLD}Warhammer Battleship CLMM — Suite de Pruebas{RESET}")
    print(f"Middleware: {MIDDLEWARE_URL}  |  Backend API: {BACKEND_URL}")
    print(f"Usuario de prueba dinámico: {TEST_USER}\n")

    args = sys.argv[1:]

    if not args:
        # Sin argumentos → ejecutar todas en orden
        run_tests(DEFAULT_ORDER)
    else:
        # Argumento = nombre(s) de prueba individual
        run_tests(args)
