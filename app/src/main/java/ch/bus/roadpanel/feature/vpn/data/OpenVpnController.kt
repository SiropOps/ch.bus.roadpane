package ch.bus.roadpanel.feature.vpn.data

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import ch.bus.roadpanel.feature.vpn.domain.OpenVpnLaunchResult

class OpenVpnController {
    fun connectVanVpn(context: Context): OpenVpnLaunchResult =
        startOpenVpnApiActivity(
            context = context,
            className = CONNECT_ACTIVITY,
            successMessage = "Demande de connexion envoyee a OpenVPN. Une confirmation Android peut etre requise.",
        )

    fun disconnectVanVpn(context: Context): OpenVpnLaunchResult =
        startOpenVpnApiActivity(
            context = context,
            className = DISCONNECT_ACTIVITY,
            successMessage = "Demande de deconnexion envoyee a OpenVPN.",
        )

    fun openOpenVpnApp(context: Context): OpenVpnLaunchResult {
        if (!openVpnAppInstalled(context)) {
            return OpenVpnLaunchResult(
                launched = false,
                message = "OpenVPN for Android n'est pas installe.",
            )
        }

        val intent = context.packageManager.getLaunchIntentForPackage(OPENVPN_PACKAGE)
            ?: return OpenVpnLaunchResult(
                launched = false,
                message = "Impossible d'ouvrir OpenVPN for Android.",
            )

        return runCatching {
            context.startActivity(intent.withNewTaskIfNeeded(context))
        }.fold(
            onSuccess = {
                OpenVpnLaunchResult(
                    launched = true,
                    message = "OpenVPN for Android est ouvert.",
                )
            },
            onFailure = { exception -> exception.toOpenVpnResult() },
        )
    }

    fun openVpnAppInstalled(context: Context): Boolean =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    OPENVPN_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(OPENVPN_PACKAGE, 0)
            }
        }.isSuccess

    private fun startOpenVpnApiActivity(
        context: Context,
        className: String,
        successMessage: String,
    ): OpenVpnLaunchResult {
        if (!openVpnAppInstalled(context)) {
            return OpenVpnLaunchResult(
                launched = false,
                message = "OpenVPN for Android n'est pas installe.",
            )
        }

        val intent = Intent(Intent.ACTION_MAIN)
            .setComponent(ComponentName(OPENVPN_PACKAGE, className))
            .putExtra(PROFILE_NAME_EXTRA, VAN_PROFILE_NAME)
            .withNewTaskIfNeeded(context)

        return runCatching {
            context.startActivity(intent)
        }.fold(
            onSuccess = {
                OpenVpnLaunchResult(
                    launched = true,
                    message = "$successMessage Profil : $VAN_PROFILE_NAME.",
                )
            },
            onFailure = { exception -> exception.toOpenVpnResult() },
        )
    }

    private fun Intent.withNewTaskIfNeeded(context: Context): Intent =
        apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

    private fun Throwable.toOpenVpnResult(): OpenVpnLaunchResult =
        when (this) {
            is ActivityNotFoundException -> OpenVpnLaunchResult(
                launched = false,
                message = "L'activite OpenVPN demandee est introuvable.",
            )
            is SecurityException -> OpenVpnLaunchResult(
                launched = false,
                message = "Android a bloque l'action OpenVPN pour raisons de securite.",
            )
            else -> OpenVpnLaunchResult(
                launched = false,
                message = message ?: "Action OpenVPN impossible.",
            )
        }

    companion object {
        const val OPENVPN_PACKAGE = "de.blinkt.openvpn"
        const val VAN_PROFILE_NAME = "client-altidoma-fr-natel"
        private const val PROFILE_NAME_EXTRA = "de.blinkt.openvpn.api.profileName"
        private const val CONNECT_ACTIVITY = "de.blinkt.openvpn.api.ConnectVPN"
        private const val DISCONNECT_ACTIVITY = "de.blinkt.openvpn.api.DisconnectVPN"
    }
}
