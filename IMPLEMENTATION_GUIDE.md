# 🚀 Guide d'Implémentation - Améliorations Futures

## Table des matières
1. [Dependency Injection avec Hilt](#dependency-injection-avec-hilt)
2. [Implémentation Offline-First](#implémentation-offline-first)
3. [Tests Unitaires](#tests-unitaires)
4. [UI Components](#ui-components)
5. [Observabilité et Monitoring](#observabilité-et-monitoring)

---

## 1. Dependency Injection avec Hilt

### Étape 1: Ajouter les dépendances

**build.gradle.kts (Project)**
```kotlin
plugins {
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

**build.gradle.kts (App)**
```kotlin
plugins {
    // ... autres plugins
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    
    // Hilt pour ViewModel
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Hilt pour WorkManager
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")
}
```

### Étape 2: Modifier l'Application class

```kotlin
@HiltAndroidApp
class NexTalkApplication : Application() {
    // ... existing code
}
```

### Étape 3: Créer les modules Hilt

**NetworkModule.kt**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideMediaService(): MediaService {
        return MediaService()
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }
}
```

**DatabaseModule.kt**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): NexTalkDatabase {
        return Room.databaseBuilder(
            context,
            NexTalkDatabase::class.java,
            "nextalk_db"
        ).build()
    }

    @Provides
    fun provideCallDao(database: NexTalkDatabase): CallDao {
        return database.callDao()
    }

    @Provides
    fun provideStatusDao(database: NexTalkDatabase): StatusDao {
        return database.statusDao()
    }
}
```

**RepositoryModule.kt**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCallRepository(callDao: CallDao): CallRepository {
        return CallRepository(callDao)
    }

    @Provides
    @Singleton
    fun provideStatusRepository(
        statusDao: StatusDao,
        mediaService: MediaService
    ): StatusRepository {
        return StatusRepository(statusDao, mediaService)
    }
}
```

### Étape 4: Modifier les ViewModels

```kotlin
@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    // ... existing code
}
```

### Étape 5: Modifier les Activities

```kotlin
@AndroidEntryPoint
class CallActivity : AppCompatActivity() {
    
    private val viewModel: CallViewModel by viewModels()
    
    // ... existing code
}
```

---

## 2. Implémentation Offline-First

### Worker pour synchronisation

**SyncWorker.kt**
```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val callRepository: CallRepository,
    private val statusRepository: StatusRepository,
    private val networkMonitor: NetworkMonitor
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Vérifier la connectivité
        if (!networkMonitor.isCurrentlyConnected()) {
            return Result.retry()
        }

        return try {
            // Synchroniser les appels
            val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
            
            callRepository.syncCallsFromFirebase(userId).getOrThrow()
            statusRepository.syncStatusesFromFirebase(userId).getOrThrow()
            
            Result.success()
        } catch (e: Exception) {
            if (e is IOException) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_USER_ID = "user_id"
    }
}
```

**Scheduler de synchronisation**
```kotlin
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun schedulePeriodicSync(userId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(SyncWorker.KEY_USER_ID to userId)
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "sync_work",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }
}
```

---

## 3. Tests Unitaires

### Test MediaService

```kotlin
@ExperimentalCoroutinesApi
class MediaServiceTest {

    private lateinit var mediaService: MediaService
    private lateinit var mockStorage: FirebaseStorage

    @Before
    fun setup() {
        mockStorage = mockk()
        mediaService = MediaService()
    }

    @Test
    fun `uploadStatusImage should return success with URL`() = runTest {
        // Given
        val mockUri = mockk<Uri>()
        
        // When
        val result = mediaService.uploadStatusImage(mockUri)
        
        // Then
        assertTrue(result.isSuccess)
    }
}
```

### Test NetworkErrorHandler

```kotlin
@ExperimentalCoroutinesApi
class NetworkErrorHandlerTest {

    @Test
    fun `executeWithRetry should retry on network error`() = runTest {
        var attempts = 0
        
        val result = NetworkErrorHandler.executeWithRetry(maxRetries = 3) {
            attempts++
            if (attempts < 3) {
                throw IOException("Network error")
            }
            "Success"
        }
        
        assertEquals(3, attempts)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getUserFriendlyMessage should return French message`() {
        val exception = UnknownHostException()
        val message = NetworkErrorHandler.getUserFriendlyMessage(exception)
        
        assertTrue(message.contains("Internet"))
    }
}
```

---

## 4. UI Components

### LoadingButton Component

```kotlin
@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White
            )
        } else {
            Text(text)
        }
    }
}
```

### NetworkStatusBar

```kotlin
@Composable
fun NetworkStatusBar(
    networkMonitor: NetworkMonitor
) {
    val isConnected by networkMonitor.isConnected.collectAsState(initial = true)
    
    AnimatedVisibility(visible = !isConnected) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Red)
                .padding(8.dp)
        ) {
            Text(
                text = "Pas de connexion Internet",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
```

### UploadProgressDialog

```kotlin
@Composable
fun UploadProgressDialog(
    isVisible: Boolean,
    progress: Int,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Upload en cours...") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$progress%")
                }
            },
            confirmButton = {}
        )
    }
}
```

---

## 5. Observabilité et Monitoring

### Firebase Analytics

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context
    ): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }
}

// Usage dans ViewModel
class StatusViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val analytics: FirebaseAnalytics
) : ViewModel() {

    fun createTextStatus(...) {
        viewModelScope.launch {
            val result = statusRepository.createTextStatus(...)
            
            if (result.isSuccess) {
                analytics.logEvent("status_created") {
                    param("status_type", "text")
                }
            }
        }
    }
}
```

### Crashlytics

```kotlin
// build.gradle.kts
implementation("com.google.firebase:firebase-crashlytics-ktx")

// Usage
fun handleError(exception: Exception) {
    FirebaseCrashlytics.getInstance().recordException(exception)
    // ... rest of error handling
}
```

### Performance Monitoring

```kotlin
// build.gradle.kts
implementation("com.google.firebase:firebase-perf-ktx")

// Usage dans Repository
suspend fun createMediaStatus(...): Result<Status> {
    val trace = Firebase.performance.newTrace("create_media_status")
    trace.start()
    
    return try {
        // ... upload logic
        trace.incrementMetric("upload_success", 1)
        Result.success(status)
    } catch (e: Exception) {
        trace.incrementMetric("upload_failure", 1)
        Result.failure(e)
    } finally {
        trace.stop()
    }
}
```

---

## 📋 Checklist d'implémentation

### Phase 1: Dependency Injection (2-3 jours)
- [ ] Ajouter Hilt dependencies
- [ ] Créer modules Hilt
- [ ] Migrer ViewModels
- [ ] Migrer Activities
- [ ] Tests de validation

### Phase 2: Offline-First (3-4 jours)
- [ ] Implémenter SyncWorker
- [ ] Créer SyncScheduler
- [ ] Ajouter gestion file d'attente
- [ ] Tests de synchronisation
- [ ] UI pour statut sync

### Phase 3: Tests (2-3 jours)
- [ ] Tests MediaService
- [ ] Tests NetworkErrorHandler
- [ ] Tests ViewModels
- [ ] Tests Repositories
- [ ] Tests UI

### Phase 4: UI Components (2-3 jours)
- [ ] LoadingButton
- [ ] NetworkStatusBar
- [ ] UploadProgressDialog
- [ ] Error Snackbars
- [ ] Animations

### Phase 5: Observabilité (1-2 jours)
- [ ] Firebase Analytics
- [ ] Crashlytics
- [ ] Performance Monitoring
- [ ] Custom metrics
- [ ] Dashboards

---

## 🎯 Priorités recommandées

1. **Haute priorité**: Dependency Injection (base pour tout le reste)
2. **Haute priorité**: Tests unitaires (qualité du code)
3. **Moyenne priorité**: UI Components (UX)
4. **Moyenne priorité**: Offline-First (robustesse)
5. **Basse priorité**: Observabilité (monitoring)

---

## 💡 Conseils

1. **Implémenter progressivement**: Ne pas tout faire d'un coup
2. **Tester à chaque étape**: Validation continue
3. **Documentation**: Commenter le code complexe
4. **Code review**: Relire avant merge
5. **Monitoring**: Surveiller les métriques en production

---

**Note**: Ce guide est un complément au fichier MODIFICATIONS_LOG.md qui documente les modifications déjà réalisées.
