package ch.bus.roadpanel.feature.vpn.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.bus.roadpanel.feature.vpn.data.OpenVpnController
import ch.bus.roadpanel.feature.vpn.domain.VanConnectivityRepository
import ch.bus.roadpanel.feature.vpn.domain.VanConnectivityStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VpnViewModel(
    context: Context,
    private val openVpnController: OpenVpnController,
    private val connectivityRepository: VanConnectivityRepository,
) : ViewModel() {
    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(
        VpnUiState(openVpnInstalled = openVpnController.openVpnAppInstalled(appContext)),
    )
    val uiState: StateFlow<VpnUiState> = _uiState.asStateFlow()

    init {
        startConnectivityRefresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(
            openVpnInstalled = openVpnController.openVpnAppInstalled(appContext),
        )
        refreshVanConnectivity()
    }

    fun connectVanVpn() {
        launchOpenVpnAction {
            openVpnController.connectVanVpn(appContext)
        }
    }

    fun disconnectVanVpn() {
        launchOpenVpnAction {
            openVpnController.disconnectVanVpn(appContext)
        }
    }

    fun openOpenVpnApp() {
        launchOpenVpnAction {
            openVpnController.openOpenVpnApp(appContext)
        }
    }

    private fun launchOpenVpnAction(action: () -> ch.bus.roadpanel.feature.vpn.domain.OpenVpnLaunchResult) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLaunchingOpenVpn = true, message = null)
            val result = action()
            _uiState.value = _uiState.value.copy(
                isLaunchingOpenVpn = false,
                openVpnInstalled = openVpnController.openVpnAppInstalled(appContext),
                message = if (result.launched) {
                    result.message + " Si le profil est absent ou refuse par l'utilisateur, l'etat restera inconnu/offline."
                } else {
                    result.message
                },
            )
            delay(1_200)
            refreshVanConnectivity()
        }
    }

    private fun startConnectivityRefresh() {
        viewModelScope.launch {
            while (true) {
                refreshVanConnectivity()
                delay(15_000)
            }
        }
    }

    private fun refreshVanConnectivity() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingConnectivity = true)
            val status = runCatching { connectivityRepository.checkStatus() }
                .getOrDefault(VanConnectivityStatus.OFFLINE)
            _uiState.value = _uiState.value.copy(
                vanStatus = status,
                isCheckingConnectivity = false,
            )
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    VpnViewModel(
                        context = context.applicationContext,
                        openVpnController = OpenVpnController(),
                        connectivityRepository = VanConnectivityRepository(),
                    ) as T
            }
    }
}
