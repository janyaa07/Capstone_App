package com.aireventure.auth.AuthFiles

import android.content.Context
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.regions.Regions
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AuthRepository(context: Context) {

    private val userPool = CognitoUserPool(
        context,
        "ap-southeast-2_5sAKyJbix",
        "1btlv5fr1f6jq29kh6phnk3251",
        null,
        Regions.AP_SOUTHEAST_2
    )

    // SIGNUP
    suspend fun signUp(email: String, password: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val attributes = CognitoUserAttributes().apply {
                addAttribute("email", email.trim().lowercase())
            }
            userPool.signUpInBackground(
                email.trim().lowercase(),
                password,
                attributes,
                null,
                object : SignUpHandler {
                    override fun onSuccess(
                        user: CognitoUser,
                        signUpResult: SignUpResult
                    ) {
                        continuation.resume(Result.success(Unit))
                    }

                    override fun onFailure(exception: Exception) {
                        continuation.resume(Result.failure(exception))
                    }
                }
            )
        }

    // LOGIN
    suspend fun login(email: String, password: String): Result<CognitoUserSession> =
        suspendCancellableCoroutine { continuation ->
            val cognitoUser = userPool.getUser(email.trim().lowercase())
            cognitoUser.getSessionInBackground(object : AuthenticationHandler {
                override fun onSuccess(userSession: CognitoUserSession, newDevice: CognitoDevice?) {
                    continuation.resume(Result.success(userSession))
                }

                override fun getAuthenticationDetails(
                    authenticationContinuation: AuthenticationContinuation,
                    userId: String
                ) {
                    val authDetails = AuthenticationDetails(userId, password, null)
                    authenticationContinuation.setAuthenticationDetails(authDetails)
                    authenticationContinuation.continueTask()
                }

                override fun getMFACode(continuation: MultiFactorAuthenticationContinuation) {}
                override fun authenticationChallenge(continuation: ChallengeContinuation) {}
                override fun onFailure(exception: Exception) {
                    continuation.resume(Result.failure(exception))
                }
            })
        }
}