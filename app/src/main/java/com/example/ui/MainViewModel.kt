package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KidneyRepository
import com.example.data.local.KidneyDatabase
import com.example.data.local.PredictionEntity
import com.example.data.local.UserEntity
import com.example.ml.KidneyFeatureExtractor
import com.example.ml.KidneyModelEngine
import com.example.ml.KidneyPredictionResult
import com.example.ml.SampleScansProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Dashboard : Screen()
    object Analyze : Screen()
    data class Result(val prediction: KidneyPredictionResult, val bitmap: Bitmap?, val modality: String, val imageName: String) : Screen()
    object History : Screen()
    object ModelInfo : Screen()
    object About : Screen()
    object Profile : Screen()
}

data class DashboardStats(
    val totalScans: Int = 0,
    val normalCases: Int = 0,
    val cystCases: Int = 0,
    val tumorCases: Int = 0,
    val stoneCases: Int = 0,
    val concordantCases: Int = 0,
    val agreementRate: Float = 100f
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KidneyRepository

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    // Analysis Pipeline state
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisStep = MutableStateFlow("")
    val analysisStep: StateFlow<String> = _analysisStep.asStateFlow()

    // History filter
    private val _historyFilter = MutableStateFlow("ALL")
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    // Selected scan for analyze screen
    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    private val _selectedScanName = MutableStateFlow("normal_ct_01")
    val selectedScanName: StateFlow<String> = _selectedScanName.asStateFlow()

    private val _selectedModality = MutableStateFlow("CT Scan")
    val selectedModality: StateFlow<String> = _selectedModality.asStateFlow()

    init {
        val db = KidneyDatabase.getDatabase(application)
        repository = KidneyRepository(db.kidneyDao())
        viewModelScope.launch {
            repository.seedDemoDoctorIfEmpty()
            // Auto-load sample scan bitmap
            val sample = SampleScansProvider.SAMPLES.first()
            _selectedScanName.value = sample.title
            _selectedBitmap.value = SampleScansProvider.generateScanBitmap(sample.id)
        }
    }

    // Dashboard Statistics Flow
    val dashboardStats: StateFlow<DashboardStats> = _currentUser.flatMapLatest { user ->
        val uid = user?.id ?: 0L
        combine(
            repository.getTotalScans(uid),
            repository.getNormalCount(uid),
            repository.getCystCount(uid),
            repository.getTumorCount(uid),
            repository.getStoneCount(uid),
            repository.getConcordantCount(uid)
        ) { args: Array<Int> ->
            val total = args[0]
            val normal = args[1]
            val cyst = args[2]
            val tumor = args[3]
            val stone = args[4]
            val concordant = args[5]
            val rate = if (total > 0) (concordant.toFloat() / total * 100f) else 100f
            DashboardStats(
                totalScans = total,
                normalCases = normal,
                cystCases = cyst,
                tumorCases = tumor,
                stoneCases = stone,
                concordantCases = concordant,
                agreementRate = rate
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // History Flow
    val historyRecords: StateFlow<List<PredictionEntity>> = combine(_currentUser, _historyFilter) { user, filter ->
        Pair(user?.id ?: 0L, filter)
    }.flatMapLatest { (uid, filter) ->
        repository.getHistory(uid, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Authentication Actions ---
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _authError.value = null
            val result = repository.authenticateUser(email, pass)
            result.onSuccess { user ->
                _currentUser.value = user
                _currentScreen.value = Screen.Dashboard
            }.onFailure { err ->
                _authError.value = err.message ?: "Authentication failed."
            }
            _isAuthenticating.value = false
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _authError.value = null
            val result = repository.registerUser(name, email, pass)
            result.onSuccess {
                login(email, pass)
            }.onFailure { err ->
                _authError.value = err.message ?: "Registration failed."
            }
            _isAuthenticating.value = false
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = Screen.Dashboard
    }

    fun clearAuthError() {
        _authError.value = null
    }

    // --- Scan Selection & Processing ---
    fun selectSampleScan(sampleId: String) {
        val sample = SampleScansProvider.SAMPLES.find { it.id == sampleId } ?: return
        _selectedScanName.value = sample.title
        _selectedModality.value = sample.modality
        _selectedBitmap.value = SampleScansProvider.generateScanBitmap(sample.id)
    }

    fun setCustomBitmap(bitmap: Bitmap, name: String) {
        _selectedBitmap.value = bitmap
        _selectedScanName.value = name
    }

    fun setModality(modality: String) {
        _selectedModality.value = modality
    }

    fun runAnalysis() {
        val bitmap = _selectedBitmap.value ?: return
        val user = _currentUser.value ?: return
        val modality = _selectedModality.value
        val imageName = _selectedScanName.value

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisStep.value = "1/4: Resizing & Normalizing 224x224..."
            delay(350)

            _analysisStep.value = "2/4: Applying Bilateral Denoising & CLAHE..."
            delay(400)

            _analysisStep.value = "3/4: Extracting 24-D Radiomic & Texture Features..."
            val features = KidneyFeatureExtractor.extract(bitmap)
            delay(450)

            _analysisStep.value = "4/4: Evaluating SVM Hyperplane & Decision Tree..."
            val result = KidneyModelEngine.classify(features)
            delay(300)

            // Save to Room DB
            val entity = PredictionEntity(
                userId = user.id,
                scanType = modality,
                imageName = imageName,
                imageUri = "",
                prediction = result.prediction,
                svmPrediction = result.svmPrediction,
                dtPrediction = result.dtPrediction,
                confidence = result.confidence,
                modelsAgree = result.modelsAgree,
                meanIntensity = features.meanIntensity,
                contrast = features.glcmContrast,
                homogeneity = features.glcmHomogeneity,
                hyperdenseRatio = features.hyperdenseRatio,
                hypodenseRatio = features.hypodenseRatio,
                modelVersion = result.modelVersion
            )
            repository.savePrediction(entity)

            _isAnalyzing.value = false
            _currentScreen.value = Screen.Result(
                prediction = result,
                bitmap = bitmap,
                modality = modality,
                imageName = imageName
            )
        }
    }

    fun deleteHistoryRecord(id: Long) {
        val uid = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.deletePrediction(id, uid)
        }
    }

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }
}
