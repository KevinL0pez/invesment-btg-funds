# Despliegue del Backend con AWS CloudFormation

Esta guia despliega el backend en AWS usando:

- ECR para almacenar imagen Docker
- ECS Fargate para ejecutar el contenedor
- Application Load Balancer para exponer el servicio
- CloudFormation para infraestructura reproducible

## 1) Configurar AWS CLI (aws configure)

Antes de cualquier despliegue, configura tus credenciales AWS en tu equipo.

### 1.1 Instalar y validar AWS CLI

```bash
aws --version
```

Si no está instalado, usa la guía oficial:
[Guía de instalación de AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)

### 1.2 Ejecutar aws configure

```bash
aws configure
```

Completa los campos:

- `AWS Access Key ID`: tu `AWS_ACCESS_KEY_ID`
- `AWS Secret Access Key`: tu `AWS_SECRET_ACCESS_KEY`
- `Default region name`: `us-east-1` (o la región donde desplegarás)
- `Default output format`: `json`

### 1.3 Verificar que quedó bien

```bash
aws sts get-caller-identity
```

Si devuelve `Account`, `Arn` y `UserId`, ya puedes continuar.

## 2) Prerrequisitos

- AWS CLI configurado (`aws configure`)
- Docker Desktop instalado y activo
- Permisos en AWS para CloudFormation, ECS, ECR, IAM, EC2, ELB, Logs
- Proyecto compilable con `mvn clean package`

## 3) Tener lo siguientes archivos creados

- `Dockerfile`
- `.dockerignore`
- `cloudformation/ecr.yaml`
- `cloudformation/backend-ecs.yaml`

## 4) Crear repositorio ECR con CloudFormation

```bash
aws cloudformation deploy \
  --stack-name btg-funds-ecr \
  --template-file cloudformation/ecr.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides MaxImagesToKeep=5
```

Obtener URI del repositorio:

```bash
aws cloudformation describe-stacks \
  --stack-name btg-funds-ecr \
  --query "Stacks[0].Outputs[?OutputKey=='RepositoryUri'].OutputValue" \
  --output text
```

Guarda el resultado en una variable (PowerShell):

```powershell
$REPO_URI = aws cloudformation describe-stacks --stack-name btg-funds-ecr --query "Stacks[0].Outputs[?OutputKey=='RepositoryUri'].OutputValue" --output text
```

## 5) Build del JAR y de la imagen Docker

```bash
mvn clean package -DskipTests
```

```bash
docker build -t btg-funds-backend:latest .
```

Login a ECR:

```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <TU_ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com
```

Tag y push:

```powershell
docker tag btg-funds-backend:latest "$REPO_URI`:latest"
docker push "$REPO_URI`:latest"
```

## 6) Desplegar infraestructura del backend (ECS + ALB)

Necesitas:

- `VpcId`
- `PublicSubnets` (dos o mas)
- valores de variables para la app (JWT, DynamoDB, Twilio, SES)

Ejemplo de despliegue:

```bash
aws cloudformation deploy \
  --stack-name btg-funds-backend \
  --template-file cloudformation/backend-ecs.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    ProjectName=btg-funds \
    VpcId=vpc-xxxxxxxx \
    PublicSubnets=\"subnet-aaaaaaa,subnet-bbbbbbb\" \
    ContainerImage=\"<REPO_URI>:latest\" \
    DesiredCount=1 \
    Cpu=256 \
    Memory=512 \
    HealthCheckGracePeriodSeconds=180 \
    LogRetentionInDays=3 \
    SesMail=\"tu-remitente-verificado@dominio.com\" \
    DynamodbCustomersTable=\"btg-funds-customers-dev\" \
    DynamodbFundsTable=\"btg-funds-funds-dev\" \
    DynamodbTransactionsTable=\"btg-funds-transactions-dev\" \
    TwilioSid=\"ACxxxxxxxx\" \
    TwilioToken=\"xxxxxxxx\" \
    TwilioMessagingServiceSid=\"MGxxxxxxxx\" \
    JwtSecret=\"una_clave_segura_de_32_bytes_minimo\" \
    DemoPassword=\"cambia_este_password\"
```

Notas de optimizacion para prueba tecnica:

- `DesiredCount=1` mantiene solo una tarea ECS.
- `Cpu=256` y `Memory=512` es el perfil minimo recomendado.
- `HealthCheckGracePeriodSeconds=180` evita reinicios por health check mientras Spring Boot inicia.
- `LogRetentionInDays=3` reduce costo de CloudWatch Logs.
- En ECR se conservan solo 5 imagenes (limpieza automatica).

## 7) Obtener URL del backend

```bash
aws cloudformation describe-stacks \
  --stack-name btg-funds-backend \
  --query "Stacks[0].Outputs[?OutputKey=='AlbDnsName'].OutputValue" \
  --output text
```

Prueba:

```bash
curl http://<ALB_DNS>/v3/api-docs
```

## 8) Actualizar backend cuando cambie el codigo

1. `mvn clean package -DskipTests`
2. `docker build -t btg-funds-backend:latest .`
3. `docker tag ...`
4. `docker push ...`
5. volver a ejecutar deploy del stack `btg-funds-backend` cambiando tag de imagen (ej. `:v2`)
