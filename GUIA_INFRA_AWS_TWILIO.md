# Guia paso a paso: DynamoDB, SES y Twilio

Este documento explica como crear y configurar desde cero:

1. IAM (grupo, usuario y permisos) para usar AWS de forma correcta.
2. Base de datos en DynamoDB para este proyecto.
3. Amazon SES para envio de correos.
4. Activacion de cuenta y numero en Twilio para envio de SMS.

---

## 1) AWS IAM: grupo, usuario y permisos

### 1.1 Crear grupo de IAM

1. Inicia sesion en AWS Console con tu cuenta.
2. Ve a `IAM` -> `User groups` -> `Create group`.
3. Nombre sugerido: `btg-funds-backend-group`.
4. Asigna politicas (puedes iniciar con estas y luego restringir):
   - `AmazonDynamoDBFullAccess`
   - `AmazonSESFullAccess`
5. Crea el grupo.

> Recomendado para produccion: crear politicas custom de minimo privilegio en lugar de FullAccess.

### 1.2 Crear usuario de IAM para la aplicacion

1. Ve a `IAM` -> `Users` -> `Create user`.
2. Nombre sugerido: `btg-funds-app-user`.
3. En permisos, agrega el usuario al grupo `btg-funds-backend-group`.
4. Finaliza creacion.

### 1.3 Crear access keys del usuario

1. Abre el usuario `btg-funds-app-user`.
2. Ve a `Security credentials`.
3. En `Access keys`, crea una nueva key.
4. Guarda:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`

> Guarda estas credenciales en `.env`. Nunca las subas a Git.

---

## 2) DynamoDB: creacion de tablas desde cero

Para este proyecto, usa region `us-east-1` (o la que configures en `AWS_REGION`, pero debe ser consistente en todo).

### 2.1 Crear tabla de clientes

1. Ve a `DynamoDB` -> `Tables` -> `Create table`.
2. Table name: `btg-funds-customers-dev`.
3. Partition key: `id` (String).
4. Billing mode: `On-demand`.
5. Create table.

### 2.2 Crear tabla de fondos

1. `Create table`.
2. Table name: `btg-funds-funds-dev`.
3. Partition key: `id` (String).
4. Billing mode: `On-demand`.
5. Create table.

### 2.3 Crear tabla de transacciones

1. `Create table`.
2. Table name: `btg-funds-transactions-dev`.
3. Partition key: `customerId` (String).
4. Sort key: `timestamp` (Number).
5. Billing mode: `On-demand`.
6. Create table.

### 2.4 Variables de entorno para DynamoDB

Configura en `.env`:

```env
AWS_REGION=us-east-1
AWS_ENDPOINT=https://dynamodb.us-east-1.amazonaws.com
AWS_ACCESS_KEY_ID=TU_ACCESS_KEY
AWS_SECRET_ACCESS_KEY=TU_SECRET_KEY

DYNAMODB_CUSTOMERS_TABLE=btg-funds-customers-dev
DYNAMODB_FUNDS_TABLE=btg-funds-funds-dev
DYNAMODB_TRANSACTIONS_TABLE=btg-funds-transactions-dev
```

---

## 3) Amazon SES: configuracion correcta para envio de correo

### 3.1 Verificar identidad de remitente

1. Ve a `Amazon SES` en la misma region de la app (`AWS_REGION`).
2. Ve a `Verified identities` -> `Create identity`.
3. Elige `Email address`.
4. Ingresa el correo remitente (ejemplo: `mi-correo@dominio.com`).
5. Confirma el email de verificacion que envia AWS.

### 3.2 Revisar modo Sandbox vs Produccion

1. En SES, revisa si la cuenta esta en `Sandbox`.
2. Si esta en Sandbox:
   - El remitente debe estar verificado.
   - El destinatario tambien debe estar verificado.
3. Si necesitas enviar a cualquier correo, solicita salida de sandbox:
   - `SES` -> `Account dashboard` -> `Request production access`.

### 3.3 Permisos IAM para SES

Si usaste el grupo con `AmazonSESFullAccess`, ya cubre permisos.
Si usas politica custom, incluye minimo:

- `ses:SendEmail`
- `ses:SendRawEmail`
- `ses:GetSendQuota`

### 3.4 Variables de entorno para SES

Configura en `.env`:

```env
SES_MAIL=tu-correo-verificado@dominio.com

# Opcional (solo si necesitas endpoint custom)
AWS_SES_ENDPOINT=
```

> Si `AWS_SES_ENDPOINT` no se usa, dejalo vacio.

---

## 4) Twilio: activar cuenta y numero para SMS

### 4.1 Crear y activar cuenta Twilio

1. Crea cuenta en Twilio.
2. Verifica email y telefono.
3. Completa datos solicitados en onboarding.

### 4.2 Obtener SID y Auth Token

1. En `Twilio Console`, copia:
   - `Account SID`
   - `Auth Token`

### 4.3 Activar capacidad de SMS (telefono origen)

Tienes dos opciones:

#### Opcion A (recomendada): Messaging Service
1. Ve a `Messaging` -> `Services` -> `Create Messaging Service`.
2. Crea servicio y agrega un numero Twilio al pool.
3. Copia el `Messaging Service SID` (inicia por `MG...`).

#### Opcion B: Numero Twilio directo
1. Ve a `Phone Numbers` -> `Manage` -> `Buy a number`.
2. Compra un numero con soporte SMS.
3. Usa ese numero como `From`.

### 4.4 Cuenta Trial: validar destino

Si tu cuenta es Trial:
- Debes verificar el numero destino en `Verified Caller IDs`.
- Solo podras enviar SMS a numeros verificados.

### 4.5 Variables de entorno para Twilio

Configura en `.env`:

```env
TWILIO_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Opcion A:
TWILIO_MESSAGING_SERVICE_SID=MGxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Opcion B (si no usas Messaging Service):
TWILIO_PHONE=+1XXXXXXXXXX
```

> No uses el numero del cliente como `From`. `From` debe ser Twilio (numero comprado o Messaging Service).

---

## 5) Checklist final de funcionamiento

1. IAM:
   - Grupo creado con permisos DynamoDB + SES.
   - Usuario creado y asociado al grupo.
   - Access key activa.
2. DynamoDB:
   - Tablas creadas en la misma region de la app.
3. SES:
   - Remitente verificado.
   - Sandbox validado (o produccion aprobada).
4. Twilio:
   - SID y token correctos.
   - Messaging Service SID o numero Twilio configurado.
5. `.env`:
   - Sin comillas innecesarias.
   - Sin valores vacios en variables obligatorias.
