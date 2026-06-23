package app.ascend.data.model

/** The signed-in user's locally-stored profile (DataStore). */
data class UserProfile(
    val name: String = "",
    val targetRole: String = "",
    val location: String = "",
    val resumeName: String? = null,   // null = no resume uploaded yet
    val onboarded: Boolean = false,
) {
    val hasResume: Boolean get() = !resumeName.isNullOrBlank()
}
