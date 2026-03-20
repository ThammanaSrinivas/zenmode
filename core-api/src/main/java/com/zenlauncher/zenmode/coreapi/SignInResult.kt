package com.zenlauncher.zenmode.coreapi

data class SignInResult(
    val userId: String?,
    val displayName: String?,
    val isNewUser: Boolean,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)
