# Frontend SmartLogix

Frontend React/Vite conectado al API Gateway mediante `/api`.

## Desarrollo local

```powershell
npm ci
npm run dev
```

Requiere el API Gateway en `http://localhost:8080`.

## Producción

El Dockerfile compila la aplicación y la sirve con Nginx. Nginx redirige `/api` al contenedor `api-gateway`.

```powershell
npm run lint
npm run build
```

La lógica de negocio no se calcula en React. Productos, inventario, stock, órdenes, cupones, rutas y reportes provienen de los microservicios.
