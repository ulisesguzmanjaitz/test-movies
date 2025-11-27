# Domus Backend Challenge - Directors API

REST API reactiva desarrollada con **Spring Boot 3.4.1** y **WebFlux** que permite obtener directores de cine que han dirigido más películas que un threshold especificado. La solución implementa paginación inteligente, caching de 3 minutos y documentación automática con Swagger.

![Java](https://img.shields.io/badge/Java-21+-blue) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-green) ![Maven](https://img.shields.io/badge/Maven-3.9.9-orange)

---

## ✨ Características

- **API REST Reactiva** - Spring WebFlux con Project Reactor
- **WebClient** - Cliente HTTP no bloqueante para consumir APIs externas
- **Paginación Inteligente** - Paralelización automática de requests
- **Caching 3 Minutos** - Reactor's cache() para optimización
- **Swagger OpenAPI 3.0** - Documentación automática e interactiva
- **Manejo de Errores** - GlobalExceptionHandler completo
- **Tests Completos** - Unitarios e integración con WebTestClient
- **Lombok** - Reducción de boilerplate
- **Validación Robusta** - Threshold negativo, nulo, tipos incorrectos

### Obtener Directores por Threshold

```bash
curl "http://localhost:8080/api/directors?threshold=4"
```

**Respuesta (200 OK)**:
```json
{
  "directors": [
    "Martin Scorsese",
    "Woody Allen",
    "Steven Spielberg"
  ]
}
```

### Casos de Error

**Threshold no numérico (400 Bad Request)**:
```bash
curl "http://localhost:8080/api/directors?threshold=abc"
```

**Respuesta**:
```json
{
  "status": 400,
  "message": "Invalid threshold. Must be a non-negative integer and not null.",
  "timestamp": "2024-01-15T14:30:45Z"
}
```

**Sin parámetro threshold (400 Bad Request)**:
```bash
curl "http://localhost:8080/api/directors"
```

**Threshold negativo (200 OK - lista vacía)**:
```bash
curl "http://localhost:8080/api/directors?threshold=-5"
```

**Respuesta**:
```json
{
  "directors": []
}
```

---

## 📡 Endpoints

### GET /api/directors

Obtiene directores que han dirigido más películas que el threshold especificado.

**Parámetros**:
- `threshold` (query, required, integer): Número mínimo de películas (debe ser >= 0)

**Respuestas**:

| Código | Descripción | Ejemplo |
|--------|-------------|---------|
| **200** | Éxito | `{"directors": ["Martin Scorsese"]}` |
| **400** | Parámetro inválido | `{"status": 400, "message": "..."}` |
| **500** | Error de servidor | `{"status": 500, "message": "..."}` |

### GET /api/directors/health

Health check del API.

**Respuesta**:
```
API is running
```

---


## Performance

### Cache (3 minutos)

El servicio implementa caching automático con Reactor's `cache()`:

```bash
# 1ª llamada (sin caché) - ~1700ms
curl "http://localhost:8080/api/directors?threshold=4"

# 2ª llamada (desde caché) - ~10ms
curl "http://localhost:8080/api/directors?threshold=4"

# Esperar 3 minutos

# 3ª llamada (caché expirado) - ~1700ms
curl "http://localhost:8080/api/directors?threshold=4"
```

---

## Seguridad

Consideraciones de seguridad implementadas:

- Validación de entrada de parámetros
- Manejo global de excepciones
- Timeouts en requests externos
- Retry logic con exponential backoff
- Logging de errores

---

**Última actualización**: 27 de Noviembre de 2025

**Status**: Production Ready

---