# Fid - Aplicación de Monitorización de Dieta y Calorías

Fid es una aplicación móvil moderna para Android que permite a los usuarios monitorizar su dieta y calorías de forma inteligente usando IA, con enfoque en la personalización dinámica y seguridad psicológica.

## Características Principales

### ✅ Implementadas

1. **Bienvenida y Onboarding** - Primera pantalla con introducción a la aplicación
2. **Registro / Inicio de Sesión** - Sistema de autenticación de usuarios
3. **Establecimiento de Objetivos** - Configuración personalizada de metas nutricionales
4. **Panel de Control (Dashboard)** - Vista principal con:
   - Anillo de progreso de calorías
   - Barras de progreso de macronutrientes
   - Índice de bienestar
   - Registro de comidas del día
5. **Registro por Foto** - Captura de alimentos mediante cámara
6. **Registro por Voz** - Reconocimiento de voz para registro rápido
7. **Registro Manual** - Búsqueda y selección manual de alimentos
8. **Detalle de Alimento** - Información nutricional completa y ajuste de cantidades
9. **Progreso** - Visualización de tendencias con:
   - Modo normal (con números)
   - Modo sin números (cualitativo)
10. **Ajustes y Perfil** - Configuración de la aplicación y perfil de usuario

## Requisitos Técnicos Cumplidos

- ✅ **Android 14.0 (API 34)** - Orientado a la última versión estable
- ✅ **Múltiples Actividades** - 4 actividades implementadas:
  - `MainActivity` (principal)
  - `AuthActivity` (autenticación)
  - `GoalSetupActivity` (configuración de objetivos)
  - `PhotoRegistrationActivity` (captura de fotos)
- ✅ **Notificaciones** - Sistema completo de notificaciones con:
  - Toast para feedback inmediato
  - Snackbar para mensajes contextuales
  - Dialogs para confirmaciones y errores
- ✅ **Base de Datos SQLite** - Implementación completa con Room:
  - Entidades: User, FoodEntry, FoodItem, WellnessEntry
  - DAOs con prevención de inyección SQL
  - Repositorio para operaciones
- ✅ **Multi-idioma** - Soporte completo para:
  - Español (valores por defecto)
  - Inglés (values-en)
- ✅ **Arquitectura** - Diseño limpio y eficiente:
  - MVVM pattern
  - Jetpack Compose para UI
  - Coroutines para operaciones asíncronas
  - Navigation Component

## Tecnologías Utilizadas

- **Kotlin** - Lenguaje de programación
- **Jetpack Compose** - UI moderna y declarativa
- **Room Database** - Persistencia local con SQLite
- **Navigation Compose** - Navegación entre pantallas
- **Coroutines & Flow** - Programación asíncrona
- **Material Design 3** - Diseño moderno y consistente

## Estructura del Proyecto

```
app/src/main/
├── java/com/example/fid/
│   ├── data/
│   │   ├── cloud/              # Interfaces para base de datos en la nube
│   │   ├── database/           # SQLite con Room
│   │   │   ├── dao/           # Data Access Objects
│   │   │   ├── entities/      # Entidades de base de datos
│   │   │   └── FidDatabase.kt # Configuración de Room
│   │   └── repository/         # Capa de repositorio
│   ├── navigation/             # Sistema de navegación
│   ├── ui/
│   │   ├── screens/           # Pantallas de la aplicación
│   │   │   ├── onboarding/
│   │   │   ├── auth/
│   │   │   ├── goals/
│   │   │   ├── dashboard/
│   │   │   ├── registration/
│   │   │   ├── progress/
│   │   │   └── settings/
│   │   └── theme/             # Tema y colores
│   ├── utils/                 # Utilidades (notificaciones, diálogos)
│   └── MainActivity.kt        # Actividad principal
└── res/
    ├── values/                # Recursos en español
    └── values-en/             # Recursos en inglés
```

## Configuración de la Base de Datos en la Nube

La aplicación está preparada para conectarse a una base de datos en la nube. Para implementarla:

### Opción 1: Firebase (Recomendado)

1. Añadir dependencias en `app/build.gradle.kts`:
```kotlin
implementation("com.google.firebase:firebase-firestore-ktx:24.10.0")
implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
```

2. Implementar los métodos en `FirebaseCloudDatabase.kt`

3. Configurar Firebase en tu proyecto:
   - Crear proyecto en Firebase Console
   - Descargar `google-services.json`
   - Colocar en `app/`

### Opción 2: API REST Personalizada

1. Crear nueva implementación de `CloudDatabaseInterface`
2. Usar Retrofit o Ktor para llamadas HTTP
3. Configurar endpoints en la clase de implementación

## Cálculos Nutricionales

La aplicación implementa:

- **TMB (Tasa Metabólica Basal)** - Ecuación Mifflin-St Jeor
- **TDEE (Gasto Energético Diario Total)** - TMB × Factor de actividad
- **Distribución de Macros** - 30% proteínas, 30% grasas, 40% carbohidratos

## Funcionalidades de Seguridad

- ✅ Prevención de inyección SQL con queries parametrizadas
- ✅ Validación de campos de entrada
- ✅ Modo sin números para salud mental
- ✅ Mensajes suaves y no críticos

## Instalación y Ejecución

1. Clonar el repositorio
2. Abrir el proyecto en Android Studio
3. Sincronizar Gradle
4. Ejecutar en emulador o dispositivo físico con Android 7.0+ (API 24+)

## Permisos Requeridos

- `CAMERA` - Para registro de alimentos por foto
- `RECORD_AUDIO` - Para registro de alimentos por voz
- `INTERNET` - Para conexión con base de datos en la nube
- `ACCESS_NETWORK_STATE` - Para verificar conectividad

## Próximos Pasos / TODOs

- [ ] Implementar conexión real con base de datos en la nube
- [ ] Añadir procesamiento real de imágenes con IA
- [ ] Implementar reconocimiento de voz real
- [ ] Añadir integración con Google Fit / Apple Health
- [ ] Implementar sincronización automática
- [ ] Añadir más gráficos y visualizaciones
- [ ] Tests unitarios e instrumentados

## Diseño

La aplicación sigue un diseño oscuro (dark theme) con:
- Color primario: Verde (#00FF87)
- Paleta de colores para macros:
  - Proteínas: Azul Cyan
  - Grasas: Amarillo
  - Carbohidratos: Naranja

## Autor

Desarrollado como proyecto académico para Sistemas Empotrados.

## Licencia

Este proyecto es de uso académico.
