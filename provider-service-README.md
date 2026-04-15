# provider-service

Microservicio que simula la validación de tarjetas con proveedores externos (Visa/Mastercard). Recibe una solicitud de validación y aplica reglas de negocio para aprobar o rechazar la transacción.

## Stack
- Java 17 / Spring Boot 3.x
- Sin base de datos — lógica stateless
- AWS ECS Fargate / ECR Public
- GitHub Actions (CI/CD)
- AWS Cloud Map (DNS privado `provider-service.paypro.local`)

## Endpoints
```
POST /provider/validate → valida una tarjeta
```

### Request
```json
{
  "cardNumber": "4532015112830366",
  "amount": 100.00,
  "currency": "MXN"
}
```

### Response
```json
{
  "approved": true,
  "reason": "Approved by provider"
}
```

## Reglas de validación
| Condición | Resultado |
|---|---|
| Tarjeta `4111111111111111` | REJECTED — tarjeta bloqueada |
| Monto > $10,000 | REJECTED — excede límite |
| Primer dígito `4` (Visa) | APPROVED |
| Primer dígito `5` (Mastercard) | APPROVED |
| Cualquier otra red | REJECTED — red no soportada |

## Diseño
Servicio completamente stateless — no tiene DB ni estado interno. Cada request es independiente. Esto lo hace simple de escalar horizontalmente y de testear.

En producción real este servicio haría una llamada HTTP a la API de Visa/Mastercard con las credenciales del comercio.

## Variables de entorno
Ninguna requerida — la lógica de validación está hardcodeada como simulación.

---

## Retos encontrados y soluciones

### 1. Servicio sin DB — diseño stateless
**Decisión de diseño:** A diferencia de `payment-service` y `account-service`, este servicio no necesita persistencia. La validación es una función pura: mismos inputs → mismo output.
**Beneficio:** Task Definition más simple (un solo contenedor), menor costo en Fargate (`256 CPU / 512MB` vs `512 CPU / 1GB`), arranque más rápido.

### 2. Cloud Map sin base de datos
**Contexto:** `provider-service` se registra en Cloud Map igual que los otros servicios, pero no expone IP pública — solo es accesible internamente via `provider-service.paypro.local:8082`.
**Por qué:** Los servicios internos no deben estar expuestos a internet. Solo `payment-service` tiene IP pública porque es el único que recibe requests del cliente.

### 3. Rolling deployment — `2/1 Tasks running`
**Problema:** Durante un redeploy ECS mostraba `2/1 Tasks running` lo cual parecía un error.
**Explicación:** Es comportamiento normal del rolling deployment. ECS lanza la nueva Task antes de detener la anterior para garantizar disponibilidad continua. En unos segundos la Task vieja se detiene y queda `1/1`.

### 4. Primer request lento (7-12 segundos)
**Problema:** El primer POST a `/payments` tardaba varios segundos, los siguientes eran inmediatos.
**Causa:** WebClient en `payment-service` inicializa el pool de conexiones, resuelve el DNS de Cloud Map y hace el handshake TCP la primera vez. Los requests subsecuentes reusan esas conexiones.
**Solución para producción:** Implementar un warm-up o health check que haga una llamada dummy al arrancar el servicio.
