import requests
import json
import time

# --- CONFIGURACIÓN ---
BASE_URL = "http://localhost:8080"
ADMIN_USER = "middleware_admin"
ADMIN_PASS = "clave_secreta_del_middleware_2026"
FINGERPRINT = "test-fingerprint-123"

# --- UTILIDADES DE IMPRESIÓN ---
def print_result(name, response):
    status = response.status_code
    print(f"[ {status} ] {name}")
    if status >= 400:
        try:
            print(f"      Error: {response.json()}")
        except:
            print(f"      Error: {response.text}")
    else:
        try:
            print(f"      Data: {json.dumps(response.json(), indent=2)}")
        except:
            print(f"      Success (No JSON)")

# --- PRUEBAS ---

def test_all():
    print(f"--- INICIANDO PRUEBAS DE ENDPOINTS EN {BASE_URL} ---")
    
    # 1. LOGIN (Obtener Token)
    print("\n[STEP 1] Login de Administrador...")
    login_data = {
        "username": ADMIN_USER,
        "password": ADMIN_PASS
    }
    headers = {"X-Fingerprint": FINGERPRINT}
    resp = requests.post(f"{BASE_URL}/api/auth/login", json=login_data, headers=headers)
    print_result("POST /api/auth/login", resp)
    
    if resp.status_code != 200:
        print("CRITICAL: No se pudo obtener el token. Abortando.")
        return

    token = resp.json().get("token")
    auth_headers = {
        "Authorization": f"Bearer {token}",
        "X-Fingerprint": FINGERPRINT,
        "Content-Type": "application/json"
    }

    # 2. PERSONAJES (Público pero requiere token según SecurityConfig)
    print("\n[STEP 2] Consultando Personajes...")
    resp = requests.get(f"{BASE_URL}/api/personajes", headers=auth_headers)
    print_result("GET /api/personajes", resp)

    # 3. REGISTRO DE USUARIO JUGADOR
    print("\n[STEP 3] Registrando usuario de prueba...")
    # Usamos un nombre más corto para no exceder los 20 caracteres (límite del DTO)
    test_user = f"user_{int(time.time()) % 1000000}"
    registro_data = {
        "username": test_user,
        "email": f"{test_user}@test.com",
        "password": "password123"
    }
    resp = requests.post(f"{BASE_URL}/api/auth/register", json=registro_data, headers=auth_headers)
    print_result("POST /api/auth/register", resp)

    # 4. VALIDAR USUARIO JUGADOR (Validate-user)
    print("\n[STEP 4] Validando credenciales del usuario de prueba...")
    validate_data = {
        "username": test_user,
        "password": "password123"
    }
    resp = requests.post(f"{BASE_URL}/api/auth/validate-user", json=validate_data, headers=auth_headers)
    print_result("POST /api/auth/validate-user", resp)

    # 5. PERFIL DE USUARIO
    print("\n[STEP 5] Obteniendo perfil del usuario...")
    resp = requests.get(f"{BASE_URL}/api/user/{test_user}", headers=auth_headers)
    print_result(f"GET /api/user/{test_user}", resp)

    # 6. ACTUALIZAR NICKNAME
    print("\n[STEP 6] Actualizando nickname...")
    new_user = f"{test_user}n" # Solo una 'n' para no pasar de 20 caracteres
    update_data = {
        "currentUsername": test_user,
        "newUsername": new_user
    }
    resp = requests.post(f"{BASE_URL}/api/auth/update-nickname", json=update_data, headers=auth_headers)
    print_result("POST /api/auth/update-nickname", resp)
    if resp.status_code == 200:
        test_user = new_user # Solo actualizamos si tuvo éxito
    else:
        print(f"      INFO: Manteniendo username '{test_user}' para siguientes pruebas.")

    # 7. LOBBY
    print("\n[STEP 7] Pruebas de Lobby...")
    # 7.1 Listar salas antes (debería estar vacío o sin la nueva)
    resp = requests.get(f"{BASE_URL}/api/lobby", headers=auth_headers)
    print_result("GET /api/lobby (Antes de crear)", resp)
    
    # 7.2 Crear sala
    lobby_data = {
        "codigoSala": "SALA_TEST_123",
        "nombreSala": "Sala de Prueba",
        "maxJugadores": 2,
        "jugadoresConectados": 1
    }
    resp = requests.post(f"{BASE_URL}/api/lobby", json=lobby_data, headers=auth_headers)
    print_result("POST /api/lobby (Creando sala)", resp)

    # 7.3 Listar salas después (debería aparecer la nueva)
    resp = requests.get(f"{BASE_URL}/api/lobby", headers=auth_headers)
    print_result("GET /api/lobby (Después de crear)", resp)

    # 8. PARTIDAS
    print("\n[STEP 8] Pruebas de Partidas Detalladas...")
    
    # 8.1 Listar todas
    resp = requests.get(f"{BASE_URL}/api/partidas", headers=auth_headers)
    print_result("GET /api/partidas (Todas)", resp)
    partidas = resp.json() if resp.status_code == 200 else []

    # 8.2 Filtrar por diferentes estados
    estados = ["EN_ESPERA", "EN_CURSO", "FINALIZADA"]
    for est in estados:
        resp = requests.get(f"{BASE_URL}/api/partidas/estado/{est}", headers=auth_headers)
        print_result(f"GET /api/partidas/estado/{est}", resp)

    # 8.3 Obtener por ID (si existe alguna)
    if partidas and len(partidas) > 0:
        pid = partidas[0].get("id")
        resp = requests.get(f"{BASE_URL}/api/partidas/{pid}", headers=auth_headers)
        print_result(f"GET /api/partidas/{pid} (Detalle por ID)", resp)
    else:
        print("      INFO: No hay partidas en BD para probar consulta por ID.")

    # 8.4 Sala activa
    resp = requests.get(f"{BASE_URL}/api/partidas/sala-activa/SALA_TEST_123", headers=auth_headers)
    print_result("GET /api/partidas/sala-activa/SALA_TEST_123", resp)

    # 9. ESTADÍSTICAS
    print("\n[STEP 9] Pruebas de Estadísticas...")
    resp = requests.get(f"{BASE_URL}/api/estadisticas/jugador/{test_user}", headers=auth_headers)
    print_result(f"GET /api/estadisticas/jugador/{test_user}", resp)

    # 10. GAME ESTADO (Zona segura)
    print("\n[STEP 10] Verificando zona segura...")
    resp = requests.get(f"{BASE_URL}/api/game/estado", headers=auth_headers)
    print_result("GET /api/game/estado", resp)

    # 11. ELIMINAR SALA LOBBY (Cleanup)
    print("\n[STEP 11] Limpiando sala de lobby...")
    resp = requests.delete(f"{BASE_URL}/api/lobby/SALA_TEST_123", headers=auth_headers)
    print_result("DELETE /api/lobby/SALA_TEST_123", resp)

    print("\n--- PRUEBAS FINALIZADAS ---")

if __name__ == "__main__":
    try:
        test_all()
    except requests.exceptions.ConnectionError:
        print(f"ERROR: No se pudo conectar al servidor en {BASE_URL}. ¿Está arrancado?")
