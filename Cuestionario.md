# Cuestionario — Parte A del examen de la Unidad IV

> **Cómo se llena este archivo.** Responda **dentro de este mismo archivo**, debajo de cada pregunta, en el bloque marcado como `**Respuesta:**`. No borre ni reescriba los enunciados: el evaluador compara pregunta por pregunta. No añada ni quite secciones.
>
> **Este archivo se versiona en el repositorio.** Debe existir en la raíz, llamarse exactamente `Cuestionario.md`, y sus respuestas deben llegar por *commits* sucesivos hechos cuando el docente lo indique. Un archivo que aparece completo en un único *commit* al final de la sesión no cumple el protocolo y se trata según el criterio de piso 4 del examen.
>
> Se valora la precisión técnica y la justificación, **no la extensión**. Una respuesta correcta de seis líneas vale más que una página imprecisa. Cuando la pregunta pida referirse al proyecto base, hágalo con nombres concretos de clases o de *endpoints*.

---

## Datos del estudiante

| Campo | Valor |
|---|---|
| Apellidos y nombres  | Tejada Bajaña Luis Alejandro |
| Número de carnet | 1207432939 |
| Correo institucional  | ltejadab@uteq.edu.ec |
| Fecha  | 28 de Agosto de 2026 |
| URL del repositorio | https://github.com/Alxjandr07/App-Web-segundo-corte-Tejada-Luis |

---

## A1. Restricciones de REST aplicadas a un caso concreto — 8 puntos

**a) Enuncie las seis restricciones del estilo arquitectónico REST según Fielding. (3 puntos)**

1. Cliente-servidor (Client-Server).
2. Sin estado (Stateless).
3. Cacheable (almacenable en caché).
4. Interfaz uniforme (Uniform Interface).
5. Sistema en capas (Layered System).
6. Código bajo demanda (Code on Demand) — la única opcional.


**b) El proyecto base expone `GET /api/v1/autores` y guarda el estado de la sesión del usuario solo en el JWT que el cliente envía en cada petición. Explique qué restricción concreta se está cumpliendo con esa decisión y qué consecuencia práctica tiene para escalar el sistema a varios servidores detrás de un balanceador. (3 puntos)**

**Respuesta:**
Se cumple la restricción *stateless* (sin estado): el servidor no guarda ninguna sesión y cada petición debe transportar toda la información necesaria, aquí el JWT que `JwtAuthenticationFilter` valida en cada llamada. Consecuencia práctica: cualquier réplica detrás del balanceador puede atender cualquier petición porque no hay estado que compartir; el sistema escala añadiendo servidores sin *sticky sessions* ni caché de sesión distribuida.



**c) De las seis restricciones, indique cuál es opcional y dé un ejemplo real de una API que la use. (2 puntos)**

**Respuesta:**
La restricción opcional es *Code on Demand*. Ejemplo real: una API web que entrega fragmentos JavaScript ejecutables, como la que sirve Google Maps en el navegador al descargar dinámicamente scripts/plugins según la interacción, o una API de un gestor de documentos que devuelve una fórmula de cálculo a ejecutar en el cliente.

---

## A2. Anatomía y ciclo de vida de un JWT — 8 puntos

**a) Un JWT tiene tres partes separadas por puntos. Nómbrelas en orden e indique qué contiene cada una. (3 puntos)**

**Respuesta:**
1. **Header**: metadatos de la firma, típicamente `alg` (algoritmo, ej. HS512) y `typ`.
2. **Payload**: los *claims* (afirmaciones) del usuario — en `JwtService` se emiten `sub` (usuario), `rol`, `jti` (id único), `iat` y `exp` (expiración).
3. **Firma (Signature)**: resultado de firmar header+payload con el secreto (HS512 aquí), para garantizar integridad y autenticidad.

**b) Un compañero afirma: «como el JWT va firmado, puedo guardar en el *payload* la contraseña del usuario sin riesgo». Explique por qué está equivocado, precisando la diferencia entre firmar y cifrar. (2 puntos)**

**Respuesta:**
Está equivocado porque *firmar no es cifrar*: la firma solo aporta integridad y autenticidad (que el token no fue alterado y quién lo emitió), pero el header y el payload van en **Base64**, un simple codificado, legible por cualquiera sin el secreto. La confidencialidad exige *cifrado* (JWE), no firma (JWS). Una contraseña en el payload quedaría expuesta en claro para quien capture el token.

**c) El JWT es *stateless* por diseño, lo que genera un problema conocido: no se puede invalidar un token antes de que expire. Describa dos estrategias distintas para revocarlo y señale la desventaja de cada una. (3 puntos)**

**Respuesta:**
1. **Lista negra de `jti`**: el servidor guarda los identificadores revocados (y su expiración) en la base de datos; el filtro los consulta y rechaza el token. *Desventaja*: reintroduce estado en el servidor (se pierde el *statelessness*) y añade una consulta extra por petición; si el nodo cae, pierde la lista.
2. **Tokens de vida corta + *refresh token***: se reduce el TTL (`expiracion-minutos`) para que un token comprometido quede inútil pronto y se renueva con un *refresh token* de mayor duración. *Desventaja*: aumenta el tráfico de login/renovación y obliga a proteger y revocar el propio *refresh token*, que también puede ser robado.

---

## A3. SOAP frente a REST — 8 puntos

**a) Complete la tabla comparativa con seis criterios entre SOAP y REST. (5 puntos)**

**Respuesta:**

| Criterio | SOAP | REST |
|---|---|---|
| Formato del mensaje | XML (SOAP envelope) | JSON principalmente (puede usar XML u otros) |
| Contrato de descripción | WSDL (definido y formal) | Sin contrato formal; descripción opcional tipo OpenAPI |
| Sobrecarga de serialización | Alta (envoltorio XML extenso) | Baja (JSON compacto) |
| Tipado | Fuerte/tipado estricto (XSD) | Dinámico/débil |
| Facilidad de consumo desde un cliente móvil | Menor (namespaces, WSDL) | Alta (ligero y simple de consumir) |
| Manejo de errores | SOAP Fault (estándar) | Códigos de estado HTTP + Problem Details (RFC 9457) |

**b) El Servicio de Rentas Internas del Ecuador expone la autorización de comprobantes electrónicos mediante servicios SOAP. Explique dos razones técnicas por las que una institución de ese tipo mantiene SOAP en lugar de migrar a REST. (3 puntos)**

**Respuesta:**
1. **Contrato formal y estándares tributarios**: el WS-Security/SOAP permite adherir firma electrónica (XAdES) y *no repudio* dentro del propio mensaje, requisitos legales del SRI y del estándar de facturación electrónica del Ecuador.
2. **Interoperabilidad con sistemas heredados y reglas normativas ya establecidas**: el esquema XSD/WSDL garantiza validación estricta y funciona igual con cualquier plataforma (Java, .NET, mainframe), y migrar implicaría renegociar los protocolos y pruebas con miles de contribuyentes, con alto coste y riesgo fiscal.

---

## A4. Cache-aside sobre un servicio externo — 8 puntos

> El proyecto base define en `CacheConfig` dos espacios de caché: `libros` con TTL de 2 minutos y `openlibrary` con TTL de 24 horas.

**a) Describa el patrón *cache-aside* en sus cuatro pasos, desde que llega la petición hasta que se responde. (3 puntos)**

**Respuesta:**
1. Llega la petición y la aplicación consulta primero la caché por la clave (el ISBN en `openlibrary`).
2. Si hay *hit*, se devuelve el valor en caché sin tocar el origen.
3. Si hay *miss*, se consulta la fuente real (la API de Open Library, o la BD para `libros`).
4. Se escribe el resultado en la caché con su TTL y se responde al cliente; al modificar el dato, se invalida la clave para que la próxima lectura vuelva a poblar.

**b) Justifique técnicamente por qué el TTL de `openlibrary` es doce veces mayor que el de `libros`, y qué criterio general debe guiar la elección de un TTL. (3 puntos)**

**Respuesta:**
Porque los metadatos que devuelve Open Library para un ISBN (título, portada, número de páginas, fecha de publicación) son muy estables y cambian raramente, mientras el catálogo local (`libros`, con ejemplares disponibles) muta con frecuencia (préstamos, altas, bajas), por lo que debe refrescarse cada poco. El criterio general: el TTL debe ser proporcional a la **volatilidad del dato** — a menor volatilidad, mayor TTL — y debe equilibrar la actualidad de la información contra la carga de peticiones al origen.

**c) Explique por qué nunca debe almacenarse en caché la respuesta de un fallo del servicio externo, y describa qué le ocurriría al sistema si se hiciera. (2 puntos)**

**Respuesta:**
Un fallo (timeout, 4xx/5xx) no es un dato válido; guardarlo en caché "congelaría" el error durante el TTL. Si el servicio externo se recupera, la caché seguiría sirviendo el error a todos los clientes hasta que expirara, y además los reintentos fallidos se repetirían contra una caché que nunca se refresca, degradando la disponibilidad percibida y ocultando la recuperación del origen.

---

## A5. Diagnóstico de códigos de estado y contrato de errores — 8 puntos

> Todos los errores del proyecto base salen en formato *Problem Details* conforme a la RFC 9457, que obsoleta a la RFC 7807.

Para cada escenario indique el código HTTP correcto y explique en una línea por qué. **Cada fila vale 1 punto** (0,5 por el código y 0,5 por la justificación); el literal g) vale 2 puntos.

| # | Escenario | Código | Justificación (una línea) |
|---|---|---|---|
| a | `GET /api/v1/libros/999999` y ese identificador no existe | 404 | El recurso solicitado no existe. |
| b | `POST /api/v1/libros` sin cabecera `Authorization` | 401 | La petición llega sin credenciales, no está autenticada (el `authenticationEntryPoint` responde 401). |
| c | Usuario autenticado con rol `LECTOR` envía `POST /api/v1/libros` | 403 | Está autenticado pero `@PreAuthorize("hasRole('ADMIN')")` le deniega el permiso (prohibido). |
| d | `POST /api/v1/libros` con el campo `titulo` vacío | 400 | Falla la validación `@Valid`; la solicitud está malformada. |
| e | Prestar un libro a un socio que ya tiene tres préstamos activos | 409 | Conflicto con la regla de negocio (límite de préstamos activos). |
| f | La API de Open Library no responde dentro del *timeout* configurado | 504 | El componente *upstream* no respondió a tiempo (Gateway Timeout). |

**g) Explique por qué devolver `200 OK` con un cuerpo `{"success": false}` es un error de diseño, y qué restricción de REST se incumple al hacerlo. (2 puntos)**

**Respuesta:**
Es un error porque el código de estado debe representar el resultado de la operación; devolver `200 OK` con `success:false` obliga al cliente a inspeccionar el cuerpo para saber si hubo éxito, rompiendo la *interfaz uniforme* (en concreto, la semántica de los códigos de estado como mensajes autodescriptivos). Conduciría, además, a clientes que tratan como éxito un fallo y a respuestas de error sin formatos normalizados como Problem Details.

---

## Declaración de honestidad académica

Marque con una `x` y complete:

- [x] Declaro que estas respuestas son de mi autoría, redactadas durante la sesión de examen, sin asistencia de inteligencia artificial ni comunicación con terceros.

Firma (nombre completo): Tejada Bajaña Luis Alejandro
