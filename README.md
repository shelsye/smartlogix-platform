# SmartLogix — Plataforma Full Stack Integrada

Corrección aplicada sobre el proyecto original, conservando su estructura y sus archivos auxiliares. La solución utiliza React, Nginx, API Gateway, Eureka, microservicios Spring Boot y bases H2 persistentes por volumen Docker.

## Inicio obligatorio con Docker

1. Abre Docker Desktop y espera a que indique que el motor está en ejecución.
2. Abre PowerShell en la carpeta que contiene `docker-compose.yml`.
3. Comprueba el motor:

```powershell
docker version
```

Debe mostrar las secciones `Client` y `Server`.

4. Primera ejecución o cambio desde una versión anterior:

```powershell
.\reset-docker.ps1
```

5. Ejecuciones posteriores, conservando datos:

```powershell
.\run-docker.ps1
```

También puedes usar directamente:

```powershell
docker compose up --build -d
```

## Accesos

- Aplicación: http://localhost:3000
- API Gateway: http://localhost:8080
- Eureka: http://localhost:8762
- Administrador: `admin@smartlogix.com`
- Contraseña: `admin1`

No existen clientes precargados. Cada cliente se registra con nombre, apellido, correo y contraseña.

## Flujo inicial

El inventario comienza vacío porque no se inyectan productos de prueba ni arreglos hardcodeados. Inicia sesión como administrador y crea productos desde **Inventario → Agregar producto**. La tienda consulta el catálogo directamente desde `inventory-service`.

## Arquitectura

- `frontend`: React + Vite + Recharts, servido por Nginx.
- `api-gateway`: punto único de acceso y validación JWT.
- `auth-service`: registro, login, perfil, usuarios, administrador y cupón personal.
- `inventory-service`: productos, catálogo, stock, reservas y despacho.
- `order-service`: órdenes, detalles, pagos ficticios, boletas, descuento y reportes.
- `shipment-service`: configuración persistida para las 16 regiones y cálculo de rutas.
- `discovery-service`: Eureka.

Cada servicio de negocio persiste en una base H2 independiente mediante volúmenes Docker:

- `auth_data`
- `inventory_data`
- `order_data`
- `shipment_data`

## Funcionalidades implementadas

### Usuarios y seguridad

- Registro real de clientes en base de datos.
- Contraseñas cifradas con BCrypt.
- Login con JWT y separación `ROLE_USER` / `ROLE_ADMIN`.
- Perfil editable: nombre, apellido, correo y contraseña.
- Administrador obligatorio configurable por variables de entorno.

### Cupón de bienvenida

- Al registrar un cliente se crea `BIENVENIDA10` en la base de `auth-service`.
- Descuento real del 10% calculado en `order-service`.
- Bloqueo pesimista para impedir doble uso concurrente.
- El cupón queda asociado al ID del usuario y a la orden donde fue utilizado.

### Inventario y catálogo

- CRUD administrativo completo: crear, editar, eliminar, activar, desactivar y modificar stock.
- Catálogo público sin cantidades internas de inventario.
- El cliente solo recibe imagen, nombre, precio, descripción y disponibilidad booleana.
- Reserva transaccional de unidades al comprar.
- Liberación al cancelar una orden pendiente.
- Despacho definitivo al marcarla como entregada.
- Bloqueo de compra cuando no existe stock suficiente.

### Adaptador inteligente de envíos

Las 16 regiones se cargan en la base de `shipment-service` y la interfaz siempre las consulta por API:

- Arica y Parinacota
- Tarapacá
- Antofagasta
- Atacama
- Coquimbo
- Valparaíso
- Metropolitana de Santiago
- O'Higgins
- Maule
- Ñuble
- Biobío
- La Araucanía
- Los Ríos
- Los Lagos
- Aysén
- Magallanes

Para cada región se calculan en backend:

- **Económico**: menor costo.
- **Mejor Ruta**: ponderación de tiempo, costo y distancia.
- **Express**: menor tiempo estimado.

Cada alternativa entrega ruta, transportista, precio, distancia, días y fecha estimada. Al confirmar la orden, `order-service` valida y reserva el stock antes de crear el envío.

### Pago ficticio y boleta electrónica

- Formulario de tarjeta ficticia en el checkout.
- Validación backend de titular, Luhn, vencimiento, CVV y cuotas.
- Tarjeta de prueba: `4111 1111 1111 1111`.
- No se guarda el número completo ni el CVV; solo marca y últimos cuatro dígitos.
- Pago, referencia, autorización y estado persistidos en `order-service`.
- Boleta electrónica ficticia persistida con subtotal, descuento, envío, neto, IVA 19% y total.
- Cliente y administrador pueden abrirla e imprimirla o guardarla como PDF.
- Al cancelar, el pago queda reembolsado y la boleta anulada.

### Órdenes y sincronización

- Cliente: listar, editar dirección/cantidades/método y cancelar mientras esté pendiente.
- Administrador: ver todas y cambiar a Pendiente, Preparando, Enviado, Entregado o Cancelado.
- Inventario, órdenes, cupón y envío se coordinan mediante llamadas internas autenticadas.
- Los dashboards consultan periódicamente las APIs para reflejar cambios sin reiniciar.

### Dashboard administrativo

Estadísticas y gráficos derivados de bases reales:

- ventas y órdenes totales;
- ingresos;
- pendientes, entregadas y canceladas;
- ventas por región, día y mes;
- productos más vendidos;
- productos sin stock;
- unidades disponibles y reservadas;
- clientes registrados.

## Diseño visual

La interfaz utiliza la paleta solicitada:

- negro `#000000`;
- morado oscuro `#52057B`;
- morado `#892CDC`;
- violeta neón `#BC6FF1`.

Cliente y administrador tienen dashboards con menú lateral, tarjetas, tablas, modales, gráficos y diseño responsive.

## Comandos útiles

```powershell
docker compose ps
docker compose logs -f
docker compose logs -f order-service
docker compose down --remove-orphans
```

Para reiniciar también las bases:

```powershell
docker compose down -v --remove-orphans
docker compose up --build -d
```

## Importante

Ejecuta Compose desde esta carpeta. El comando siguiente debe encontrar el archivo:

```powershell
dir .\docker-compose.yml
```

Si `docker version` no muestra `Server`, el motor de Docker Desktop está apagado y ningún proyecto podrá iniciar todavía.
